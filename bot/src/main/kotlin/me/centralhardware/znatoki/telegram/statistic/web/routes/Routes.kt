package me.centralhardware.znatoki.telegram.statistic.web.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import me.centralhardware.znatoki.telegram.statistic.extensions.db
import me.centralhardware.znatoki.telegram.statistic.web.*
import me.centralhardware.znatoki.telegram.statistic.web.http.*
import me.centralhardware.znatoki.telegram.statistic.web.service.*

/*
 * The web API, one function per section. Each endpoint only reads the request and hands it to its
 * service; what a group of endpoints requires is declared once with requires(Access.…), and checks
 * that depend on the record itself (own record, open month) live in the services.
 */

/**
 * Unauthenticated: the no-access screen's "Запросить доступ" signs its request with its Telegram credential itself;
 * the sign-in screen reads the bot's username; Android checks the TWA's signing key against assetlinks.json.
 */
fun Route.publicRoutes() {
    post("/auth/access-request") { db { call.respond(AppService.requestAccess(call.receive<AuthRequest>().credential)) } }
    get("/auth/config") { call.respond(AuthConfigDto(WebConfig.botUsername)) }
    get("/.well-known/assetlinks.json") { call.respond(AppService.assetLinks()) }
    get("/dl/{token}") { Downloads.serve(call, call.parameters["token"] ?: notFound()) }
}

fun Route.appRoutes() {
    get("/boot") { db { call.respond(AppService.boot(call.caller)) } }
    get("/search") { db { call.respond(AppService.search(call.caller, call.query("q"))) } }
    get("/photo/{kind}/{id}") { db {
        call.response.header(HttpHeaders.CacheControl, "private, max-age=3600")
        call.respondBytes(AppService.photo(call.caller, call.parameters["kind"] ?: notFound(), call.parameters["id"] ?: notFound()), ContentType.Image.JPEG)
    } }
}

fun Route.lessonRoutes() = route("/lessons") {
    get { db { call.respond(LessonService.list(call.caller, call.visibleMonth(), call.tutorScope())) } }
    get("/last-price") { db {
        call.respond(
            LessonService.lastPrice(
                call.caller,
                call.query("student")?.toIntOrNull() ?: badRequest("Нет ученика."),
                call.query("subject")?.toLongOrNull() ?: badRequest("Нет предмета."),
                call.query("tutor")?.toLongOrNull(),
            )
        )
    } }
    get("/{id}") { db { call.respond(LessonService.get(call.caller, call.pathUuid())) } }
    patch("/{id}") { db { call.respond(LessonService.update(call.caller, call.pathUuid(), call.receive())) } }
    requires(Access.TIME) {
        post { db { call.receiveForm<LessonInput>().let { call.respond(LessonService.create(call.caller, it.data, it.photo)) } } }
    }
    requires(Access.ADMIN) {
        post("/bulk") { db { call.respond(LessonService.bulk(call.caller, call.receive())) } }
    }
}

fun Route.paymentRoutes() = route("/payments") {
    requires(Access.READ_PAYMENTS) {
        get { db { call.respond(PaymentService.list(call.caller, call.visibleMonth(), call.tutorScope())) } }
        get("/{id}") { db { call.respond(PaymentService.get(call.caller, call.pathInt())) } }
    }
    requires(Access.PAYMENT) {
        post { db { call.receiveForm<PaymentInput>().let { call.respond(PaymentService.create(call.caller, it.data, it.photo)) } } }
    }
    patch("/{id}") { db { call.respond(PaymentService.update(call.caller, call.pathInt(), call.receive())) } }
}

fun Route.studentRoutes() = route("/students") {
    requires(Access.STUDENTS) {
        get { db { call.respond(StudentWebService.list(call.caller)) } }
        get("/{id}") { db { call.respond(StudentWebService.get(call.caller, call.pathInt())) } }
    }
    requires(Access.CLIENT) {
        post { db { call.respond(StudentWebService.create(call.caller, call.receive())) } }
        put("/{id}") { db { call.respond(StudentWebService.update(call.caller, call.pathInt(), call.receive())) } }
        post("/{id}/status") { db { call.respond(StudentWebService.setStatus(call.caller, call.pathInt(), call.receive())) } }
    }
    requires(Access.NOTES) {
        post("/{id}/notes") { db { call.respond(StudentWebService.addNote(call.caller, call.pathInt(), call.receive<TextInput>().text)) } }
    }
    requires(Access.ADMIN) {
        post("/{id}/delete") { db { call.respond(StudentWebService.setDeleted(call.caller, call.pathInt(), true)) } }
        post("/{id}/restore") { db { call.respond(StudentWebService.setDeleted(call.caller, call.pathInt(), false)) } }
    }
}

fun Route.scheduleRoutes() = requires(Access.TIME) {
    get("/schedule") { db { call.respond(ScheduleService.get(call.caller)) } }
    put("/reminders") { db { call.respond(ScheduleService.saveReminders(call.caller, call.receive())) } }
    route("/schedule/slots") {
        post { db { call.respond(ScheduleService.add(call.caller, call.receive())) } }
        delete("/{id}") { db { call.respond(ScheduleService.setDeleted(call.caller, call.pathInt(), true)) } }
        post("/{id}/restore") { db { call.respond(ScheduleService.setDeleted(call.caller, call.pathInt(), false)) } }
        post("/{id}/skip") { db { call.respond(ScheduleService.skip(call.caller, call.pathInt(), call.receive<DateInput>().date)) } }
        post("/{id}/unmark") { db { call.respond(ScheduleService.unmark(call.caller, call.pathInt(), call.receive<DateInput>().date)) } }
    }
}

fun Route.reportRoutes() = route("/report") {
    get { db { call.respond(ReportWebService.report(call.caller, call.reportTutor(), call.visibleMonth())) } }
    get("/preview") { db {
        val subject = call.query("subject")?.toLongOrNull() ?: badRequest("Нет предмета.")
        call.respond(ReportWebService.preview(call.caller, call.reportTutor(), call.visibleMonth(), subject))
    } }
    post("/export") { db {
        val input = call.receive<ExportInput>()
        val month = call.visibleMonth(input.month)
        if (input.to == "chat") call.respond(ExportService.toChat(call.caller, input, month))
        else call.respond(ExportService.toDownload(call.caller, input, month))
    } }
    requires(Access.TIME) {
        post("/submit") { db { call.respond(SubmissionService.submit(call.caller, call.receive())) } }
    }
    requires(Access.SEES_ALL) {
        get("/inbox") { db { call.respond(SubmissionService.inbox(call.visibleMonth())) } }
        route("/{tutor}/{month}") {
            fun io.ktor.server.application.ApplicationCall.month() = parseMonth(parameters["month"]) ?: notFound()
            post("/accept") { db { call.respond(SubmissionService.accept(call.caller, call.pathLong("tutor"), call.month())) } }
            post("/return") { db { call.respond(SubmissionService.returnBack(call.caller, call.pathLong("tutor"), call.month(), call.receive<ReturnInput>().comment)) } }
            post("/reopen") { db { call.respond(SubmissionService.reopen(call.caller, call.pathLong("tutor"), call.month())) } }
        }
    }
}

fun Route.statsRoutes() {
    get("/stats") { db { call.respond(StatsService.stats(call.caller, call.query("months")?.toIntOrNull())) } }
}

fun Route.teamRoutes() = requires(Access.MANAGE_TUTORS) {
    route("/tutors") {
        get { db { call.respond(TeamService.list()) } }
        post { db { call.respond(TeamService.add(call.caller, call.receive())) } }
        put("/{id}") { db { call.respond(TeamService.update(call.caller, call.pathLong(), call.receive())) } }
        post("/{id}/disable") { db { call.respond(TeamService.disable(call.caller, call.pathLong())) } }
        post("/{id}/enable") { db { call.respond(TeamService.enable(call.caller, call.pathLong())) } }
    }
}

fun Route.auditRoutes() = requires(Access.ADMIN) {
    get("/audit") { db {
        val filter = AuditFilter(call.query("type"), call.query("q"), call.query("rtype"), call.query("rid"))
        call.respond(AuditLogService.page(filter, call.query("limit")?.toIntOrNull() ?: 25, call.query("offset")?.toIntOrNull() ?: 0))
    } }
    get("/alerts") { db { call.respond(AuditAlerts.settings()) } }
    put("/alerts") { db { call.receive<AlertsDto>().let { AuditAlerts.save(it); call.respond(it) } } }
}
