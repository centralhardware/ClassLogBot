package me.centralhardware.znatoki.telegram.statistic.web

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.PhoneNumber
import me.centralhardware.znatoki.telegram.statistic.entity.SchoolClass
import me.centralhardware.znatoki.telegram.statistic.entity.Student
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change
import me.centralhardware.znatoki.telegram.statistic.service.StudentService
import java.time.LocalDate
import java.time.LocalDateTime

private val STATUS_TITLES = mapOf("active" to "Занимается", "paused" to "Пауза", "left" to "Ушёл")

/** Phone digits as the bot stores them: 7XXXXXXXXXX; null for an empty field. */
private fun normalizePhone(raw: String?): PhoneNumber? {
    var d = raw.orEmpty().filter { it.isDigit() }
    if (d.isEmpty()) return null
    if (d.startsWith("8")) d = "7" + d.drop(1) else if (d.startsWith("9")) d = "7$d"
    if (!PhoneNumber.validate(d)) badRequest("Телефон $raw: нужно 10 цифр после +7.")
    return PhoneNumber(d)
}

private fun fmtPhone(p: String?) = p?.let { "+7 ${it.substring(1, 4)} ${it.substring(4, 7)}-${it.substring(7, 9)}-${it.substring(9)}" }

private fun validate(input: StudentInput, except: Int?) {
    if (input.last.isBlank() || input.first.isBlank()) badRequest("Введите фамилию и имя.")
    if (input.cls == null || !SchoolClass.validate(input.cls)) badRequest("Класс от 1 до 11.")
    val dup = StudentMapper.findAll().any {
        it.id.id != except && it.secondName.trim().equals(input.last.trim(), true) && it.name.trim().equals(input.first.trim(), true)
    }
    if (dup) badRequest("Ученик с такими фамилией и именем уже есть.")
}

fun Route.studentRoutes() {
    requires(Access.STUDENTS) {
        apiGet("/students") { c ->
            call.respond(WebRepo.studentRows(includeDeleted = c.isAdmin, withBalance = c.seesAll))
        }
        apiGet("/students/{id}") { c ->
            val id = call.parameters["id"]?.toIntOrNull() ?: notFound()
            val row = WebRepo.studentRow(id, c.seesAll)?.takeIf { !it.deleted || c.isAdmin } ?: notFound("Ученик не найден.")
            val s = StudentMapper.findById(id.toStudentId())
            val (count, recent) = WebRepo.studentLessonStats(id, if (c.seesAll) null else c.id)
            call.respond(
                StudentDetailDto(
                    row = row,
                    phone = s.phone?.value,
                    parent = s.responsiblePhone?.value,
                    motherFio = s.motherFio,
                    birthDate = s.birthDate?.toString(),
                    recordDate = s.recordDate?.toString(),
                    notes = WebRepo.notes(id),
                    lessons = count,
                    paid = if (c.seesAll) WebRepo.paidTotal(id) else null,
                    recent = recent,
                )
            )
        }
    }


    requires(Access.CLIENT) {
        post("/students") {
            val c = call.caller
            val input = call.receive<StudentInput>()
            validate(input, null)
            val id = WebRepo.insertStudent(input.copy(last = input.last.trim(), first = input.first.trim(), middle = input.middle.trim()),
                normalizePhone(input.phone)?.value, normalizePhone(input.parent)?.value, c.id).toStudentId()
            StudentService.refresh()
            AuditService.log(
                c.id, "Создано", "student", id.id, "${input.last.trim()} ${input.first.trim()}",
                listOfNotNull(Change("класс", null, input.cls), input.source?.takeIf { it.isNotBlank() }?.let { Change("источник", null, it) }),
                DataSource.WEB, id.id,
            )
            call.respond(WebRepo.studentRow(id.id, c.seesAll)!!)
        }
        put("/students/{id}") {
            val c = call.caller
            val id = call.parameters["id"]?.toIntOrNull() ?: notFound()
            val input = call.receive<StudentInput>()
            validate(input, id)
            val old = StudentMapper.findById(id.toStudentId())
            val phone = normalizePhone(input.phone)?.value
            val parent = normalizePhone(input.parent)?.value
            val source = input.source?.trim()?.takeIf { it.isNotEmpty() }
            val oldSource = WebRepo.studentRow(id, false)?.source
            WebRepo.updateStudent(id, input.copy(last = input.last.trim(), first = input.first.trim(), middle = input.middle.trim()), phone, parent, c.id)
            StudentService.refresh()
            val changes = listOf(
                Change("фамилия", old.secondName, input.last.trim()),
                Change("имя", old.name, input.first.trim()),
                Change("отчество", old.lastName, input.middle.trim()),
                Change("класс", old.schoolClass?.value, input.cls),
                Change("телефон", fmtPhone(old.phone?.value), fmtPhone(phone)),
                Change("телефон родителя", fmtPhone(old.responsiblePhone?.value), fmtPhone(parent)),
                Change("источник", oldSource, source),
            ).filter { (it.old?.toString() ?: "") != (it.new?.toString() ?: "") }
            if (changes.isNotEmpty()) {
                AuditService.log(c.id, "Изменено", "student", id, "${input.last.trim()} ${input.first.trim()}", changes, DataSource.WEB, id)
            }
            call.respond(WebRepo.studentRow(id, c.seesAll)!!)
        }
        post("/students/{id}/status") {
            val c = call.caller
            val id = call.parameters["id"]?.toIntOrNull() ?: notFound()
            val input = call.receive<StatusInput>()
            val title = STATUS_TITLES[input.status] ?: badRequest("Неизвестный статус.")
            val old = WebRepo.studentRow(id, false) ?: notFound()
            val reason = if (input.status == "active") null else input.reason?.takeIf { it in STATUS_REASONS } ?: "Другое"
            WebRepo.setStudentStatus(id, input.status, reason, c.id)
            AuditService.log(
                c.id, "Статус", "student", id, "${old.last} ${old.first}",
                listOf(Change("статус", STATUS_TITLES[old.status], title + (reason?.let { " · $it" } ?: ""))),
                DataSource.WEB, id,
            )
            call.respond(WebRepo.studentRow(id, c.seesAll)!!)
        }
    }



    requires(Access.NOTES) {
        post("/students/{id}/notes") {
            val c = call.caller
            val id = call.parameters["id"]?.toIntOrNull() ?: notFound()
            val text = call.receive<TextInput>().text.trim().take(2000)
            if (text.isEmpty()) badRequest("Пустая заметка.")
            val row = WebRepo.studentRow(id, false) ?: notFound()
            WebRepo.addNote(id, c.id, text)
            AuditService.log(
                c.id, "Заметка", "student", id, "${row.last} ${row.first}",
                listOf(Change("заметка", null, if (text.length > 40) text.take(40) + "…" else text)), DataSource.WEB, id,
            )
            call.respond(WebRepo.notes(id))
        }
    }

    requires(Access.ADMIN) {
        post("/students/{id}/{action}") {
            val c = call.caller
            val id = call.parameters["id"]?.toIntOrNull() ?: notFound()
            val deleted = when (call.parameters["action"]) { "delete" -> true; "restore" -> false; else -> notFound() }
            val row = WebRepo.studentRow(id, false) ?: notFound()
            WebRepo.setStudentDeleted(id, deleted, c.id)
            StudentService.refresh()
            AuditService.log(
                c.id, if (deleted) "Удалено" else "Восстановлено", "student", id, "${row.last} ${row.first}",
                listOf(Change("удалён", row.deleted, deleted)), DataSource.WEB, id,
            )
            call.respond(WebRepo.studentRow(id, c.seesAll)!!)
        }
    }
}
