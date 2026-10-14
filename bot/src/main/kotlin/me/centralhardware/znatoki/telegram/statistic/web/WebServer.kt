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
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.web.http.HttpError
import me.centralhardware.znatoki.telegram.statistic.web.http.InitDataAuth
import me.centralhardware.znatoki.telegram.statistic.web.http.notFound
import me.centralhardware.znatoki.telegram.statistic.web.routes.*
import me.centralhardware.znatoki.telegram.statistic.web.service.AuditAlerts
import me.centralhardware.znatoki.telegram.statistic.web.service.Notifications
import me.centralhardware.znatoki.telegram.statistic.web.service.Reminders
import java.io.File
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** The Mini App's server: static frontend from /web, the API under /api, run inside the bot process. */
object WebServer {

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    fun start(bot: TelegramBot) {
        if (!WebConfig.enabled) {
            KSLog.info("WebServer: disabled by WEB_ENABLED=false")
            return
        }
        Notifications.bot = bot
        AuditService.listener = AuditAlerts
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { Reminders.run() }
        embeddedServer(CIO, port = WebConfig.port) { module() }.start(wait = false)
        KSLog.info("WebServer: listening on :${WebConfig.port}")
    }

    private fun Application.module() {
        install(ContentNegotiation) { json(json) }
        install(StatusPages) {
            exception<HttpError> { call, e -> call.respond(e.status, ErrorDto(e.message, e.code)) }
            exception<SerializationException> { call, e -> call.respond(HttpStatusCode.BadRequest, ErrorDto("Некорректный запрос: ${e.message}")) }
            exception<BadRequestException> { call, e -> call.respond(HttpStatusCode.BadRequest, ErrorDto("Некорректный запрос: ${e.message}")) }
            exception<Throwable> { call, e ->
                KSLog.error("WebServer: ${call.request.local.uri} failed", e)
                call.respond(HttpStatusCode.InternalServerError, ErrorDto("Ошибка на сервере. Попробуйте ещё раз."))
            }
        }
        routing {
            publicRoutes()
            route("/api") {
                install(InitDataAuth)
                appRoutes()
                lessonRoutes()
                paymentRoutes()
                studentRoutes()
                scheduleRoutes()
                reportRoutes()
                statsRoutes()
                teamRoutes()
                auditRoutes()
            }
            staticResources("/", "web") {
                modify { _, call -> call.response.header(HttpHeaders.CacheControl, "no-cache") }
            }
        }
    }
}

/** Files generated for one download; Telegram's downloadFile fetches them without the auth header. */
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
