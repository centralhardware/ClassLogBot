package me.centralhardware.znatoki.telegram.statistic.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.SourceOption
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap

val STATUS_REASONS = listOf("Каникулы", "Болезнь", "Переезд", "Закончил школу", "Дорого", "Не подошёл формат", "Другое")

private val accessRequests = ConcurrentHashMap<Long, LocalDateTime>()

/** "iPhone · Telegram iOS", from the platform Telegram reports plus the browser's user agent. */
fun deviceName(platform: String?, userAgent: String?): String {
    val ua = userAgent.orEmpty()
    val hw = when {
        "iPad" in ua -> "iPad"
        "iPhone" in ua -> "iPhone"
        "Android" in ua -> "Android"
        "Windows" in ua -> "Windows"
        "Mac OS" in ua || "Macintosh" in ua -> "Mac"
        "Linux" in ua -> "Linux"
        else -> "Устройство"
    }
    val app = when (platform) {
        "ios" -> "Telegram iOS"
        "android", "android_x" -> "Telegram Android"
        "tdesktop" -> "Telegram Desktop"
        "macos" -> "Telegram macOS"
        "weba", "webk", "web" -> "Telegram Web"
        null, "", "unknown" -> "браузер"
        else -> "Telegram $platform"
    }
    return "$hw · $app"
}

/** The client address with the last part hidden, like 85.140.12.x. */
fun ApplicationCall.maskedIp(): String {
    val ip = request.header("X-Forwarded-For")?.substringBefore(',')?.trim() ?: request.origin.remoteHost
    return if (ip.count { it == '.' } == 3) ip.substringBeforeLast('.') + ".x" else ip.substringBeforeLast(':') + ":x"
}

fun bootFor(caller: Caller): BootDto {
    val t = caller.tutor
    val first = WebRepo.firstMonth() ?: YearMonth.now()
    val now = YearMonth.now()
    val months = generateSequence(first) { it.plusMonths(1) }.takeWhile { !it.isAfter(now) }.map { it.toString() }.toList()
    val tutors = TutorMapper.getAll().map {
        val teaching = it.subjects.isNotEmpty() || Permissions.ADD_TIME in it.permissions
        TutorBriefDto(it.id.id, it.name, it.subjects.map { s -> s.id }, teaching, it.permissions.isNotEmpty())
    }.sortedBy { it.name }
    return BootDto(
        token = caller.session.id.toString(),
        me = MeDto(t.id.id, t.name, t.permissions.map { it.name }, t.subjects.map { it.id }),
        caps = CapsDto(
            time = caller.canTime,
            payment = caller.canPayment,
            readPayments = caller.readsPayments,
            client = caller.canClient,
            admin = caller.isAdmin,
            director = caller.isDirector,
            manageTutors = caller.canManageTutors,
            forceGroup = caller.canForceGroup,
            extraHalfHour = caller.canExtraHalfHour,
            timeForOthers = caller.canTimeForOthers,
            paymentForOthers = caller.canPaymentForOthers,
            seesAll = caller.seesAll,
        ),
        today = LocalDate.now().toString(),
        months = months,
        subjects = WebRepo.subjects(),
        tutors = tutors,
        sources = (WebRepo.usedSources() + SourceOption.options()).distinctBy { it.lowercase() },
        reasons = STATUS_REASONS,
    )
}

fun Route.authRoutes(ctx: WebContext) {
    /** Opens a session from Telegram's signed initData; the app calls it on every launch. */
    post("/auth") {
        val req = call.receive<AuthRequest>()
        val data = InitDataValidator.validate(req.initData)
            ?: throw HttpError(HttpStatusCode.Unauthorized, "Не удалось подтвердить вход через Telegram. Откройте приложение из бота.", "auth")
        val device = deviceName(req.platform, call.request.header("User-Agent"))
        val ip = call.maskedIp()
        val tutor = TutorMapper.findByIdOrNull(TutorId(data.user.id))
        if (tutor == null || Permissions.WEB_INTERFACE !in tutor.permissions) {
            AuditService.log(
                data.user.id, "Нет доступа", "session", data.user.id, tutor?.name ?: data.user.displayName,
                listOf(
                    Change("причина", null, if (tutor == null) "нет в списке преподавателей" else "нет права WEB_INTERFACE"),
                    Change("устройство", null, device), Change("IP", null, ip),
                ),
                DataSource.WEB,
            )
            throw HttpError(HttpStatusCode.Forbidden, "Нет доступа к веб-интерфейсу.", if (tutor == null) "unknown" else "no_web")
        }

        val existing = SessionStore.findByInitData(data.hash)
        if (existing != null && existing.tutorId == tutor.id.id && existing.revoked) {
            throw HttpError(HttpStatusCode.Unauthorized, "Сеанс завершён. Закройте и откройте приложение заново.", "revoked")
        }
        val session = if (existing != null && existing.tutorId == tutor.id.id && SessionStore.isUsable(existing)) {
            existing
        } else {
            val knownDevice = SessionStore.knownDevice(tutor.id.id, device)
            SessionStore.create(tutor.id.id, data.hash, device, ip).also {
                AuditService.log(tutor.id.id, "Вход", "session", tutor.id.id, tutor.name,
                    listOf(Change("устройство", null, device), Change("IP", null, ip)), DataSource.WEB)
                if (!knownDevice) ctx.alerts.newDevice(tutor.id.id, tutor.name, device, ip)
            }
        }
        call.respond(bootFor(Caller(tutor, session)))
    }

    /** "Запросить доступ" on the no-access screen: tells the admins who is asking. */
    post("/auth/access-request") {
        val req = call.receive<AuthRequest>()
        val data = InitDataValidator.validate(req.initData)
            ?: throw HttpError(HttpStatusCode.Unauthorized, "Не удалось подтвердить вход через Telegram.", "auth")
        val prev = accessRequests[data.user.id]
        if (prev != null && prev.isAfter(LocalDateTime.now().minusHours(1))) {
            call.respond(OkDto())
            return@post
        }
        accessRequests[data.user.id] = LocalDateTime.now()
        val u = data.user
        val known = TutorMapper.findByIdOrNull(TutorId(u.id))
        ctx.notifier.toPermission(
            Permissions.ADMIN,
            buildString {
                append("🔑 Запрос доступа к веб-интерфейсу: ${u.displayName}")
                u.username?.let { append(" (@$it)") }
                append(", Telegram ID ${u.id}.")
                if (known != null) append(" Уже в команде как «${known.name}», нет права WEB_INTERFACE.")
                else append(" Пока не в команде.")
            },
            "Открыть команду" to "/#tab=tutors",
        )
        call.respond(OkDto())
    }
}

fun Route.bootRoutes() {
    apiGet("/boot") { c -> call.respond(bootFor(c)) }
}
