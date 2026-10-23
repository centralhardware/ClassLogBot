package me.centralhardware.znatoki.telegram.statistic.web.http

import io.ktor.http.HttpHeaders
import me.centralhardware.znatoki.telegram.statistic.extensions.db
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.header
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.util.AttributeKey
import io.ktor.utils.io.toByteArray
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.web.Caller
import me.centralhardware.znatoki.telegram.statistic.web.TelegramAuth
import me.centralhardware.znatoki.telegram.statistic.web.WebServer
import me.centralhardware.znatoki.telegram.statistic.web.parseMonth
import java.time.YearMonth
import java.util.UUID

// ---------- errors ----------

/** An expected failure with a message the app shows to the user as is. */
class HttpError(val status: HttpStatusCode, override val message: String, val code: String? = null) : RuntimeException(message)

fun badRequest(message: String): Nothing = throw HttpError(HttpStatusCode.BadRequest, message)
fun forbidden(message: String = "Недостаточно прав."): Nothing = throw HttpError(HttpStatusCode.Forbidden, message)
/** Runs [block], answering 400 with the rule's message when [InvalidRecord] says the input breaks one. */
inline fun <T> invalidAsBadRequest(block: () -> T): T =
    try { block() } catch (e: me.centralhardware.znatoki.telegram.statistic.service.InvalidRecord) { badRequest(e.message ?: "Неверные данные.") }

fun notFound(message: String = "Запись не найдена."): Nothing = throw HttpError(HttpStatusCode.NotFound, message)

// ---------- who is calling ----------

private val CallerKey = AttributeKey<Caller>("Caller")
val ApplicationCall.caller: Caller get() = attributes[CallerKey]

/**
 * Every request carries a Telegram credential: the Mini App's initData (`Authorization: tma <initData>`)
 * or, outside Telegram, the Login Widget's data (`Authorization: tlw <data>`); `?tma=` / `?tlw=` for <img> links.
 * It is checked against the bot token each time and the tutor must have WEB_INTERFACE.
 */
val InitDataAuth = createRouteScopedPlugin("InitDataAuth") {
    onCall { call ->
        val q = call.request.queryParameters
        val credential = call.request.header(HttpHeaders.Authorization)
            ?: q["tma"]?.let { "tma $it" }
            ?: q["tlw"]?.let { "tlw $it" }
        val user = credential?.let { TelegramAuth.verify(it) }
            ?: throw HttpError(HttpStatusCode.Unauthorized, "Войдите через Telegram.", "auth")
        val caller = db { Caller.load(user.id) } ?: throw HttpError(
            HttpStatusCode.Forbidden, "Нет доступа к веб-интерфейсу.",
            if (db { TutorMapper.findByIdOrNull(TutorId(user.id)) } == null) "unknown" else "no_web",
        )
        call.attributes.put(CallerKey, caller)
    }
}

// ---------- what a group of endpoints requires ----------

enum class Access(val message: String, val check: (Caller) -> Boolean) {
    TIME("Нет права вносить занятия.", { it.canTime }),
    PAYMENT("Нет права вносить оплаты.", { it.canPayment }),
    READ_PAYMENTS("Нет доступа к оплатам.", { it.readsPayments }),
    STUDENTS("Нет доступа к ученикам.", { it.seesStudents }),
    CLIENT("Изменять учеников могут те, у кого есть право ADD_CLIENT.", { it.canClient }),
    NOTES("Нет права добавлять заметки.", { it.canTime || it.canClient }),
    SEES_ALL("Доступно администратору и директору.", { it.seesAll }),
    MANAGE_TUTORS("Управлять командой могут админ и те, у кого есть MANAGE_TUTORS.", { it.canManageTutors }),
    ADMIN("Доступно только администратору.", { it.isAdmin }),
}

private class AccessConfig { lateinit var access: Access }

private val RequiresAccess = createRouteScopedPlugin("RequiresAccess", ::AccessConfig) {
    val access = pluginConfig.access
    onCall { call -> if (!access.check(call.caller)) forbidden(access.message) }
}

/** A transparent route node: it matches everything and only carries the access check. */
private class AccessSelector(private val access: Access) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int) = RouteSelectorEvaluation.Transparent
    override fun toString() = "(requires $access)"
}

/** Endpoints declared in [build] are reachable only for callers that pass [access]. */
fun Route.requires(access: Access, build: Route.() -> Unit): Route =
    createChild(AccessSelector(access)).apply {
        install(RequiresAccess) { this.access = access }
        build()
    }

// ---------- request parameters ----------

fun ApplicationCall.query(name: String): String? = request.queryParameters[name]?.takeIf { it.isNotBlank() }

fun ApplicationCall.pathInt(name: String = "id"): Int = parameters[name]?.toIntOrNull() ?: notFound()
fun ApplicationCall.pathLong(name: String = "id"): Long = parameters[name]?.toLongOrNull() ?: notFound()
fun ApplicationCall.pathUuid(name: String = "id"): UUID = parameters[name]?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: notFound()

/** `?month=`: any month for admins and the director, always the current one for everyone else. */
fun ApplicationCall.visibleMonth(param: String? = query("month")): YearMonth =
    if (caller.seesAll) parseMonth(param) ?: YearMonth.now() else YearMonth.now()

/** `?tutor=` (all | me | id): whose records a list shows; tutors always see their own. */
fun ApplicationCall.tutorScope(): Long? {
    val c = caller
    val p = query("tutor")
    return when {
        !c.seesAll -> c.id
        p == null || p == "all" -> null
        p == "me" -> c.id
        else -> p.toLongOrNull() ?: c.id
    }
}

/** `?tutor=` for a single tutor's report: anyone for admins and the director, yourself otherwise. */
fun ApplicationCall.reportTutor(): Long = caller.let { c -> if (c.seesAll) query("tutor")?.toLongOrNull() ?: c.id else c.id }

/** A multipart form: the JSON `data` field decoded as [T], plus the `photo` file if any. */
class Form<T>(val data: T, val photo: ByteArray?)

suspend inline fun <reified T> ApplicationCall.receiveForm(): Form<T> {
    var data: String? = null
    var photo: ByteArray? = null
    receiveMultipart(formFieldLimit = 25L * 1024 * 1024).forEachPart { part ->
        when (part) {
            is PartData.FormItem -> if (part.name == "data") data = part.value
            is PartData.FileItem -> if (part.name == "photo") photo = part.provider().toByteArray()
            else -> {}
        }
        part.dispose()
    }
    return Form(WebServer.json.decodeFromString<T>(data ?: badRequest("Нет данных формы.")), photo)
}
