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
