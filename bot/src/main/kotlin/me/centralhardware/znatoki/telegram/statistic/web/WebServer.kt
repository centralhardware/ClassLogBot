package me.centralhardware.znatoki.telegram.statistic.web

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import dev.inmo.tgbotapi.bot.TelegramBot
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import java.io.File
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** An expected failure with a message the app shows to the user as is. */
class HttpError(val status: HttpStatusCode, override val message: String, val code: String? = null) : RuntimeException(message)

fun badRequest(message: String): Nothing = throw HttpError(HttpStatusCode.BadRequest, message)
fun forbidden(message: String = "Недостаточно прав."): Nothing = throw HttpError(HttpStatusCode.Forbidden, message)
fun notFound(message: String = "Запись не найдена."): Nothing = throw HttpError(HttpStatusCode.NotFound, message)

private val CallerKey = AttributeKey<Caller>("Caller")
val ApplicationCall.caller: Caller get() = attributes[CallerKey]

/** Accepts `Authorization: Bearer <session>` or, for <img> and download links, `?t=<session>`. */
private val SessionAuth = createRouteScopedPlugin("SessionAuth") {
    onCall { call ->
        val token = call.request.header(HttpHeaders.Authorization)?.removePrefix("Bearer ")?.trim()
            ?: call.request.queryParameters["t"]
        val id = token?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: throw HttpError(HttpStatusCode.Unauthorized, "Нужно войти заново.", "auth")
        val session = SessionStore.find(id)?.takeIf { SessionStore.isUsable(it) }
            ?: throw HttpError(HttpStatusCode.Unauthorized, "Сеанс завершён. Закройте и откройте приложение заново.", "auth")
        val caller = Caller.load(session) ?: throw HttpError(HttpStatusCode.Forbidden, "Нет доступа к веб-интерфейсу.", "no_web")
        if (session.lastSeenAt.isBefore(LocalDateTime.now().minusMinutes(1))) SessionStore.touch(session.id)
        call.attributes.put(CallerKey, caller)
    }
}

/** Files generated for one download; Telegram's downloadFile fetches them without our auth header. */
object Downloads {
    private class Entry(val file: File, val name: String, val type: ContentType, val expires: LocalDateTime)

    private val entries = ConcurrentHashMap<String, Entry>()

    fun register(file: File, name: String, type: ContentType): String {
        entries.entries.removeIf { (_, e) -> e.expires.isBefore(LocalDateTime.now()).also { if (it) e.file.delete() } }
        val token = UUID.randomUUID().toString()
        entries[token] = Entry(file, name, type, LocalDateTime.now().plusMinutes(10))
        return "/dl/$token"
    }

    suspend fun serve(call: ApplicationCall, token: String) {
        val e = entries[token]?.takeIf { it.expires.isAfter(LocalDateTime.now()) } ?: notFound("Ссылка устарела, выгрузите файл ещё раз.")
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, e.name).toString(),
        )
        call.response.header(HttpHeaders.ContentType, e.type.toString())
        call.respondFile(e.file)
    }
}

class WebContext(val bot: TelegramBot) {
    val notifier = WebNotifier(bot)
    val alerts = AuditAlerts(notifier)
}

object WebServer {

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    fun start(bot: TelegramBot) {
        if (!WebConfig.enabled) {
            KSLog.info("WebServer: disabled by WEB_ENABLED=false")
            return
        }
        val ctx = WebContext(bot)
        AuditService.listener = ctx.alerts

        embeddedServer(CIO, port = WebConfig.port) {
            install(ContentNegotiation) { json(json) }
            install(StatusPages) {
                exception<HttpError> { call, e -> call.respond(e.status, ErrorDto(e.message, e.code)) }
                exception<kotlinx.serialization.SerializationException> { call, e ->
                    call.respond(HttpStatusCode.BadRequest, ErrorDto("Некорректный запрос: ${e.message}"))
                }
                exception<io.ktor.server.plugins.BadRequestException> { call, e ->
                    call.respond(HttpStatusCode.BadRequest, ErrorDto("Некорректный запрос: ${e.message}"))
                }
                exception<Throwable> { call, e ->
                    KSLog.error("WebServer: ${call.request.local.uri} failed", e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorDto("Ошибка на сервере. Попробуйте ещё раз."))
                }
            }
            routing {
                authRoutes(ctx)
                get("/dl/{token}") { Downloads.serve(call, call.parameters["token"]!!) }
                route("/api") {
                    install(SessionAuth)
                    bootRoutes()
                    lessonRoutes(ctx)
                    paymentRoutes(ctx)
                    studentRoutes()
                    scheduleRoutes()
                    reportRoutes(ctx)
                    statsRoutes()
                    tutorRoutes()
                    auditRoutes(ctx)
                    photoRoutes()
                }
                staticResources("/", "web") {
                    modify { _, call -> call.response.header(HttpHeaders.CacheControl, "no-cache") }
                }
            }
        }.start(wait = false)
        ctx.notifier.launch { scheduleReminders() }
        KSLog.info("WebServer: listening on :${WebConfig.port}, app url ${WebConfig.appUrl ?: "(WEB_APP_URL not set)"}")
    }
}

/** Routes that need the signed-in caller live under /api; this helper keeps the handlers short. */
fun Route.apiGet(path: String, body: suspend io.ktor.server.routing.RoutingContext.(Caller) -> Unit) =
    get(path) { body(call.caller) }
