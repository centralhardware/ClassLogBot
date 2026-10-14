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
import me.centralhardware.znatoki.telegram.statistic.web.*
import me.centralhardware.znatoki.telegram.statistic.web.http.*
import me.centralhardware.znatoki.telegram.statistic.web.service.*

/*
 * The web API, one function per section. Each endpoint only reads the request and hands it to its
 * service; what a group of endpoints requires is declared once with requires(Access.…), and checks
 * that depend on the record itself (own record, open month) live in the services.
 */

/** Unauthenticated: the no-access screen's "Запросить доступ" signs its request with initData itself. */
fun Route.publicRoutes() {
    post("/auth/access-request") { call.respond(AppService.requestAccess(call.receive<AuthRequest>().initData)) }
    get("/dl/{token}") { Downloads.serve(call, call.parameters["token"]!!) }
}

fun Route.appRoutes() {
    get("/boot") { call.respond(AppService.boot(call.caller)) }
    get("/search") { call.respond(AppService.search(call.caller, call.query("q"))) }
    get("/photo/{kind}/{id}") {
        call.response.header(HttpHeaders.CacheControl, "private, max-age=3600")
        call.respondBytes(AppService.photo(call.caller, call.parameters["kind"]!!, call.parameters["id"]!!), ContentType.Image.JPEG)
    }
}

fun Route.lessonRoutes() = route("/lessons") {
    get { call.respond(LessonService.list(call.caller, call.visibleMonth(), call.tutorScope())) }
    get("/last-price") {
        call.respond(
            LessonService.lastPrice(
                call.caller,
                call.query("student")?.toIntOrNull() ?: badRequest("Нет ученика."),
                call.query("subject")?.toLongOrNull() ?: badRequest("Нет предмета."),
                call.query("tutor")?.toLongOrNull(),
            )
        )
    }
    get("/{id}") { call.respond(LessonService.get(call.caller, call.pathUuid())) }
    patch("/{id}") { call.respond(LessonService.update(call.caller, call.pathUuid(), call.receive())) }
    requires(Access.TIME) {
        post { call.receiveForm<LessonInput>().let { call.respond(LessonService.create(call.caller, it.data, it.photo)) } }
    }
    requires(Access.ADMIN) {
        post("/bulk") { call.respond(LessonService.bulk(call.caller, call.receive())) }
    }
}

fun Route.paymentRoutes() = route("/payments") {
    requires(Access.READ_PAYMENTS) {
        get { call.respond(PaymentService.list(call.visibleMonth(), call.tutorScope())) }
        get("/{id}") { call.respond(PaymentService.get(call.caller, call.pathInt())) }
    }
    requires(Access.PAYMENT) {
        post { call.receiveForm<PaymentInput>().let { call.respond(PaymentService.create(call.caller, it.data, it.photo)) } }
    }
    patch("/{id}") { call.respond(PaymentService.update(call.caller, call.pathInt(), call.receive())) }
}

fun Route.studentRoutes() = route("/students") {
    requires(Access.STUDENTS) {
        get { call.respond(StudentWebService.list(call.caller)) }
        get("/{id}") { call.respond(StudentWebService.get(call.caller, call.pathInt())) }
    }
    requires(Access.CLIENT) {
        post { call.respond(StudentWebService.create(call.caller, call.receive())) }
        put("/{id}") { call.respond(StudentWebService.update(call.caller, call.pathInt(), call.receive())) }
        post("/{id}/status") { call.respond(StudentWebService.setStatus(call.caller, call.pathInt(), call.receive())) }
    }
    requires(Access.NOTES) {
        post("/{id}/notes") { call.respond(StudentWebService.addNote(call.caller, call.pathInt(), call.receive<TextInput>().text)) }
    }
    requires(Access.ADMIN) {
        post("/{id}/delete") { call.respond(StudentWebService.setDeleted(call.caller, call.pathInt(), true)) }
        post("/{id}/restore") { call.respond(StudentWebService.setDeleted(call.caller, call.pathInt(), false)) }
    }
}

fun Route.scheduleRoutes() = requires(Access.TIME) {
    get("/schedule") { call.respond(ScheduleService.get(call.caller)) }
    put("/reminders") { call.respond(ScheduleService.saveReminders(call.caller, call.receive())) }
    route("/schedule/slots") {
        post { call.respond(ScheduleService.add(call.caller, call.receive())) }
        delete("/{id}") { call.respond(ScheduleService.setDeleted(call.caller, call.pathInt(), true)) }
        post("/{id}/restore") { call.respond(ScheduleService.setDeleted(call.caller, call.pathInt(), false)) }
        post("/{id}/skip") { call.respond(ScheduleService.skip(call.caller, call.pathInt(), call.receive<DateInput>().date)) }
        post("/{id}/unmark") { call.respond(ScheduleService.unmark(call.caller, call.pathInt(), call.receive<DateInput>().date)) }
    }
}

fun Route.reportRoutes() = route("/report") {
    get { call.respond(ReportWebService.report(call.caller, call.reportTutor(), call.visibleMonth())) }
    get("/preview") {
        val subject = call.query("subject")?.toLongOrNull() ?: badRequest("Нет предмета.")
        call.respond(ReportWebService.preview(call.caller, call.reportTutor(), call.visibleMonth(), subject))
    }
    post("/export") {
        val input = call.receive<ExportInput>()
        val month = call.visibleMonth(input.month)
        if (input.to == "chat") call.respond(ExportService.toChat(call.caller, input, month))
        else call.respond(ExportService.toDownload(call.caller, input, month))
    }
    requires(Access.TIME) {
        post("/submit") { call.respond(SubmissionService.submit(call.caller, call.receive())) }
    }
    requires(Access.SEES_ALL) {
        get("/inbox") { call.respond(SubmissionService.inbox(call.visibleMonth())) }
        route("/{tutor}/{month}") {
            fun io.ktor.server.application.ApplicationCall.month() = parseMonth(parameters["month"]) ?: notFound()
            post("/accept") { call.respond(SubmissionService.accept(call.caller, call.pathLong("tutor"), call.month())) }
            post("/return") { call.respond(SubmissionService.returnBack(call.caller, call.pathLong("tutor"), call.month(), call.receive<ReturnInput>().comment)) }
            post("/reopen") { call.respond(SubmissionService.reopen(call.caller, call.pathLong("tutor"), call.month())) }
        }
    }
}

fun Route.statsRoutes() {
    get("/stats") { call.respond(StatsService.stats(call.caller, call.query("months")?.toIntOrNull())) }
}

fun Route.teamRoutes() = requires(Access.MANAGE_TUTORS) {
    route("/tutors") {
        get { call.respond(TeamService.list()) }
        post { call.respond(TeamService.add(call.caller, call.receive())) }
        put("/{id}") { call.respond(TeamService.update(call.caller, call.pathLong(), call.receive())) }
        post("/{id}/disable") { call.respond(TeamService.disable(call.caller, call.pathLong())) }
        post("/{id}/enable") { call.respond(TeamService.enable(call.caller, call.pathLong())) }
    }
}

fun Route.auditRoutes() = requires(Access.ADMIN) {
    get("/audit") {
        val filter = AuditFilter(call.query("type"), call.query("q"), call.query("rtype"), call.query("rid"))
        call.respond(AuditLogService.page(filter, call.query("limit")?.toIntOrNull() ?: 25, call.query("offset")?.toIntOrNull() ?: 0))
    }
    get("/alerts") { call.respond(AuditAlerts.settings()) }
    put("/alerts") { call.receive<AlertsDto>().let { AuditAlerts.save(it); call.respond(it) } }
}
