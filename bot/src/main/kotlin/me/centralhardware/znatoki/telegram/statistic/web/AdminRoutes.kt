package me.centralhardware.znatoki.telegram.statistic.web

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.service.StudentService
import java.io.File
import java.nio.file.Files
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

/** What "Вернуть доступ" gives back: the plain tutor preset. */
private val TUTOR_PRESET = listOf("ADD_TIME", "ADD_PAYMENT", "ADD_CLIENT", "FORCE_GROUP", "EXTRA_HALF_HOUR", "WEB_INTERFACE")
private val TS = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

private fun Caller.requireManager() { if (!canManageTutors) forbidden("Управлять командой могут админ и те, у кого есть MANAGE_TUTORS.") }

private fun Tutor.toDto(currentSession: UUID?) = TutorDto(
    id = id.id,
    name = name,
    perms = permissions.map { it.name },
    subjects = subjects.map { it.id },
    rate = rate,
    lastLogin = SessionStore.lastLogin(id.id)?.createdAt?.toString(),
    lastDevice = SessionStore.lastLogin(id.id)?.device,
    sessions = SessionStore.active(id.id).map { SessionDto(it.id.toString(), it.device, it.ip, it.lastSeenAt.toString(), it.id == currentSession) },
)

private fun subjectList(ids: List<Long>, names: Map<Long, String>) = ids.mapNotNull { names[it] }.joinToString(", ")

/** Only admins hand out or take away ADMIN and MANAGE_TUTORS, and nobody demotes themselves. */
private fun Caller.checkPermissionChange(target: Tutor?, input: TutorInput) {
    val known = Permissions.entries.map { it.name }.toSet()
    input.perms.forEach { if (it !in known) badRequest("Неизвестное право $it.") }
    val privileged = setOf(Permissions.ADMIN.name, Permissions.MANAGE_TUTORS.name)
    val before = target?.permissions?.map { it.name }.orEmpty().toSet()
    if (!isAdmin && (before.any { it in privileged } || input.perms.any { it in privileged })) {
        forbidden("Права ADMIN и MANAGE_TUTORS выдаёт только админ.")
    }
    if (target?.id?.id == id && Permissions.ADMIN.name in before && Permissions.ADMIN.name !in input.perms) {
        badRequest("Нельзя снять права администратора с самого себя.")
    }
    val teaches = input.perms.any { it == "ADD_TIME" || it == "ADD_TIME_FOR_OTHERS" }
    if (teaches && Permissions.ADMIN.name !in input.perms && Permissions.DIRECTOR.name !in input.perms && input.subjects.isEmpty()) {
        badRequest("Выберите хотя бы один предмет: без него нельзя внести занятие.")
    }
    input.rate?.let { if (it !in 0..100) badRequest("Ставка от 0 до 100%.") }
}

fun Route.tutorRoutes() {
    apiGet("/tutors") { c ->
        c.requireManager()
        call.respond(TutorMapper.getAll().sortedBy { it.name }.map { it.toDto(c.session.id) })
    }

    post("/tutors") {
        val c = call.caller
        c.requireManager()
        val input = call.receive<TutorInput>()
        if (input.name.isBlank()) badRequest("Введите имя преподавателя.")
        if (input.id !in 10_000L..999_999_999_999_999L) badRequest("Telegram ID — это число, например 428100231. Его можно узнать у @userinfobot.")
        if (TutorMapper.findByIdOrNull(TutorId(input.id)) != null) badRequest("Преподаватель с таким Telegram ID уже есть.")
        c.checkPermissionChange(null, input)
        WebRepo.insertTutor(input.copy(name = input.name.trim()))
        val subjects = subjectNames()
        AuditService.log(
            c.id, "Добавлен", "tutor", input.id, input.name.trim(),
            listOf(Change("Telegram ID", null, input.id), Change("права", null, input.perms.joinToString(", ")), Change("предметы", null, subjectList(input.subjects, subjects))),
            DataSource.WEB,
        )
        call.respond(TutorMapper.findByIdOrNull(TutorId(input.id))!!.toDto(null))
    }

    put("/tutors/{id}") {
        val c = call.caller
        c.requireManager()
        val target = TutorMapper.findByIdOrNull(TutorId(call.parameters["id"]?.toLongOrNull() ?: notFound())) ?: notFound()
        val input = call.receive<TutorInput>().copy(id = target.id.id)
        if (input.name.isBlank()) badRequest("Введите имя преподавателя.")
        c.checkPermissionChange(target, input)
        WebRepo.updateTutor(input.copy(name = input.name.trim()))
        val subjects = subjectList(target.subjects.map { it.id }, subjectNames()) to subjectList(input.subjects, subjectNames())
        val changes = listOf(
            Change("имя", target.name, input.name.trim()),
            Change("права", target.permissions.joinToString(", ") { it.name }, input.perms.joinToString(", ")),
            Change("предметы", subjects.first, subjects.second),
            Change("ставка", target.rate?.let { "$it%" }, input.rate?.let { "$it%" }),
        ).filter { it.old != it.new }
        if (changes.isNotEmpty()) AuditService.log(c.id, "Права изменены", "tutor", target.id.id, input.name.trim(), changes, DataSource.WEB)
        call.respond(TutorMapper.findByIdOrNull(target.id)!!.toDto(c.session.id))
    }

    post("/tutors/{id}/{action}") {
        val c = call.caller
        c.requireManager()
        val target = TutorMapper.findByIdOrNull(TutorId(call.parameters["id"]?.toLongOrNull() ?: notFound())) ?: notFound()
        when (call.parameters["action"]) {
            "disable" -> {
                if (target.id.id == c.id) badRequest("Нельзя отключить самого себя.")
                if (!c.isAdmin && Permissions.ADMIN in target.permissions) forbidden("Отключить админа может только админ.")
                WebRepo.updateTutor(TutorInput(target.id.id, target.name, emptyList(), target.subjects.map { it.id }, target.rate))
                SessionStore.revokeAll(target.id.id)
                AuditService.log(c.id, "Доступ отключён", "tutor", target.id.id, target.name,
                    listOf(Change("права", target.permissions.joinToString(", ") { it.name }, "—")), DataSource.WEB)
            }
            "enable" -> {
                WebRepo.updateTutor(TutorInput(target.id.id, target.name, TUTOR_PRESET, target.subjects.map { it.id }, target.rate))
                AuditService.log(c.id, "Доступ возвращён", "tutor", target.id.id, target.name,
                    listOf(Change("права", "—", TUTOR_PRESET.joinToString(", "))), DataSource.WEB)
            }
            "logout" -> {
                val n = SessionStore.revokeAll(target.id.id)
                AuditService.log(c.id, "Выход на всех устройствах", "session", target.id.id, target.name, listOf(Change("сеансов", n, 0)), DataSource.WEB)
            }
            else -> notFound()
        }
        call.respond(TutorMapper.findByIdOrNull(target.id)!!.toDto(c.session.id))
    }

    delete("/sessions/{id}") {
        val c = call.caller
        val s = SessionStore.find(runCatching { UUID.fromString(call.parameters["id"]) }.getOrNull() ?: notFound()) ?: notFound()
        if (s.tutorId != c.id) c.requireManager()
        SessionStore.revoke(s.id)
        val name = TutorMapper.findByIdOrNull(TutorId(s.tutorId))?.name ?: s.tutorId.toString()
        AuditService.log(c.id, "Сеанс завершён", "session", s.tutorId, name, listOf(Change("устройство", s.device, null)), DataSource.WEB)
        call.respond(OkDto())
    }
}

private fun auditWhere(type: String?, q: String?, rtype: String?, rid: String?): Pair<String, Map<String, Any?>> {
    val clauses = mutableListOf("true")
    val params = mutableMapOf<String, Any?>()
    if (rtype != null && rid != null) {
        clauses += "a.entity_type = :rt AND a.entity_id = :rid"; params["rt"] = rtype; params["rid"] = rid
    } else if (type != null && type != "all") {
        clauses += "a.entity_type = :t"; params["t"] = type
    }
    if (!q.isNullOrBlank()) {
        clauses += "(coalesce(a.title, '') || ' ' || coalesce(t.name, '') || ' ' || a.action) ILIKE :q"; params["q"] = "%${q.trim()}%"
    }
    return clauses.joinToString(" AND ") to params
}

private fun kotliquery.Row.toAudit() = AuditDto(
    n = int("id"),
    at = localDateTime("timestamp").toString(),
    actor = long("user_id"),
    actorName = stringOrNull("actor") ?: long("user_id").toString(),
    source = stringOrNull("source") ?: "BOT",
    action = string("action"),
    type = stringOrNull("entity_type") ?: "",
    id = stringOrNull("entity_id") ?: "",
    title = stringOrNull("title") ?: stringOrNull("details") ?: "",
    changes = stringOrNull("changes")?.let { WebServer.json.parseToJsonElement(it) },
)

fun auditCsv(type: String?): File {
    val (where, params) = auditWhere(type, null, null, null)
    val rows = runList(
        queryOf("SELECT a.*, t.name AS actor FROM audit_log a LEFT JOIN tutors t ON t.id = a.user_id WHERE $where ORDER BY a.id DESC", params)
    ) { it.toAudit() }
    val esc = { s: String -> "\"" + s.replace("\"", "\"\"") + "\"" }
    val file = Files.createTempDirectory("classlog").resolve("audit_log.csv").toFile()
    file.bufferedWriter().use { w ->
        w.write("﻿id;время;кто;источник;действие;тип;запись;название;изменения\n")
        rows.forEach { a ->
            w.write(listOf(a.n.toString(), a.at.replace('T', ' '), a.actorName, a.source, a.action, a.type, a.id, a.title, a.changes?.toString() ?: "")
                .joinToString(";") { esc(it) } + "\n")
        }
    }
    return file
}

fun Route.auditRoutes(ctx: WebContext) {
    apiGet("/audit") { c ->
        if (!c.isAdmin) forbidden("Журнал видит только админ.")
        val q = call.request.queryParameters
        val (where, params) = auditWhere(q["type"], q["q"], q["rtype"], q["rid"])
        val limit = (q["limit"]?.toIntOrNull() ?: 25).coerceIn(1, 200)
        val offset = (q["offset"]?.toIntOrNull() ?: 0).coerceAtLeast(0)
        val total = runSingle(
            queryOf("SELECT count(*) AS n FROM audit_log a LEFT JOIN tutors t ON t.id = a.user_id WHERE $where", params)
        ) { it.int("n") } ?: 0
        val items = runList(
            queryOf(
                "SELECT a.*, t.name AS actor FROM audit_log a LEFT JOIN tutors t ON t.id = a.user_id WHERE $where ORDER BY a.id DESC LIMIT $limit OFFSET $offset",
                params,
            )
        ) { it.toAudit() }
        call.respond(AuditPageDto(total, items))
    }

    apiGet("/alerts") { c ->
        if (!c.isAdmin) forbidden()
        call.respond(ctx.alerts.settings())
    }

    put("/alerts") {
        val c = call.caller
        if (!c.isAdmin) forbidden()
        val a = call.receive<AlertsDto>()
        ctx.alerts.save(a)
        call.respond(a)
    }
}

fun Route.photoRoutes() {
    /** The lesson photo or payment receipt from MinIO, for the record's card. */
    apiGet("/photo/{kind}/{id}") { c ->
        val key = when (call.parameters["kind"]) {
            "lesson" -> WebRepo.lesson(uuidParam(call.parameters["id"]))?.also { if (!c.seesAll && it.tutor != c.id) forbidden() }?.photo
            "payment" -> {
                if (!c.readsPayments) forbidden()
                WebRepo.payment(call.parameters["id"]?.toIntOrNull() ?: notFound())?.also { if (!c.seesAll && it.tutor != c.id) forbidden() }?.photo
            }
            else -> null
        } ?: notFound("Фото нет.")
        val bytes = MinioService.getBytes(key).getOrElse { notFound("Фото не найдено в хранилище.") }
        call.response.header(HttpHeaders.CacheControl, "private, max-age=3600")
        call.respondBytes(bytes, ContentType.Image.JPEG)
    }

    /** Global search: students by fuzzy name, plus the lessons and payments the caller may see. */
    apiGet("/search") { c ->
        val q = call.request.queryParameters["q"]?.trim().orEmpty()
        if (q.length < 2) { call.respond(SearchDto(emptyList(), emptyList(), emptyList())); return@apiGet }
        val names = WebRepo.studentNames()
        val scope = if (c.seesAll) null else c.id
        val found = StudentService.search(q).map { it.id.id }.filter { names[it]?.deleted == false }
        val rows = WebRepo.studentRows(includeDeleted = false, withBalance = c.seesAll).associateBy { it.id }
        val subjects = WebRepo.subjects()
        val subjectHit = subjects.filter { it.name.contains(q, true) }.map { it.id }.toSet()
        val amount = q.filter { it.isDigit() }.toIntOrNull()?.takeIf { q.all { ch -> ch.isDigit() || ch.isWhitespace() } }

        val month = YearMonth.now()
        val lessons = (WebRepo.lessonsOfStudents(found, scope, 6) +
            if (subjectHit.isNotEmpty() || amount != null) WebRepo.lessons(month.start(), month.end(), scope)
                .filter { !it.deleted && (it.subject in subjectHit || it.amount == amount) } else emptyList())
            .distinctBy { it.id }.take(8)
        val payments = if (c.readsPayments) (WebRepo.paymentsOfStudents(found, scope, 5) +
            if (amount != null) WebRepo.payments(month.start(), month.end(), scope).filter { !it.deleted && it.amount == amount } else emptyList())
            .distinctBy { it.id }.take(6) else emptyList()
        call.respond(SearchDto(found.mapNotNull { rows[it] }.take(5), lessons.map { it.toDto(names) }, payments.map { it.toDto(names) }))
    }
}

