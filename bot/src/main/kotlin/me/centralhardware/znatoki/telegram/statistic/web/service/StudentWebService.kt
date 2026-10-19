package me.centralhardware.znatoki.telegram.statistic.web.service

import kotliquery.Session
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.PhoneNumber
import me.centralhardware.znatoki.telegram.statistic.entity.SchoolClass
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change
import me.centralhardware.znatoki.telegram.statistic.service.StudentService
import me.centralhardware.znatoki.telegram.statistic.web.*
import me.centralhardware.znatoki.telegram.statistic.web.http.badRequest
import me.centralhardware.znatoki.telegram.statistic.web.http.notFound

val STATUS_REASONS = listOf("Каникулы", "Болезнь", "Переезд", "Закончил школу", "Дорого", "Не подошёл формат", "Другое")
private val STATUS_TITLES = mapOf("active" to "Занимается", "paused" to "Пауза", "left" to "Ушёл")

/** Students: the list, the student card, the add/edit form, status, notes, deleting and restoring. */
object StudentWebService {

    context(_: Session)
    fun list(c: Caller): List<StudentRowDto> = WebRepo.studentRows(includeDeleted = c.isAdmin, withBalance = c.seesAll)

    context(_: Session)
    fun get(c: Caller, id: Int): StudentDetailDto {
        val row = WebRepo.studentRow(id, c.seesAll)?.takeIf { !it.deleted || c.isAdmin } ?: notFound("Ученик не найден.")
        val s = StudentMapper.findById(id.toStudentId())
        val (count, recent) = WebRepo.studentLessonStats(id, if (c.seesAll) null else c.id)
        return StudentDetailDto(
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
    }

    context(_: Session)
    fun create(c: Caller, raw: StudentInput): StudentRowDto {
        val input = clean(raw)
        validate(input, null)
        val id = WebRepo.insertStudent(input, phone(input.phone), phone(input.parent), c.id)
        StudentService.refresh()
        AuditService.log(
            c.id, "Создано", "student", id, "${input.last} ${input.first}",
            listOfNotNull(Change("класс", null, input.cls), input.source?.let { Change("источник", null, it) }), DataSource.WEB, id,
        )
        return WebRepo.studentRow(id, c.seesAll) ?: notFound()
    }

    context(_: Session)
    fun update(c: Caller, id: Int, raw: StudentInput): StudentRowDto {
        val input = clean(raw)
        validate(input, id)
        val old = StudentMapper.findById(id.toStudentId())
        val oldSource = WebRepo.studentRow(id, false)?.source
        val phone = phone(input.phone)
        val parent = phone(input.parent)
        WebRepo.updateStudent(id, input, phone, parent, c.id)
        StudentService.refresh()
        val changes = listOf(
            Change("фамилия", old.secondName, input.last),
            Change("имя", old.name, input.first),
            Change("отчество", old.lastName, input.middle),
            Change("класс", old.schoolClass?.value, input.cls),
            Change("телефон", fmtPhone(old.phone?.value), fmtPhone(phone)),
            Change("телефон родителя", fmtPhone(old.responsiblePhone?.value), fmtPhone(parent)),
            Change("источник", oldSource, input.source),
        ).filter { (it.old?.toString() ?: "") != (it.new?.toString() ?: "") }
        if (changes.isNotEmpty()) AuditService.log(c.id, "Изменено", "student", id, "${input.last} ${input.first}", changes, DataSource.WEB, id)
        return WebRepo.studentRow(id, c.seesAll) ?: notFound()
    }

    /** Active, paused or left; a pause or a leave carries one of [STATUS_REASONS]. */
    context(_: Session)
    fun setStatus(c: Caller, id: Int, input: StatusInput): StudentRowDto {
        val title = STATUS_TITLES[input.status] ?: badRequest("Неизвестный статус.")
        val old = WebRepo.studentRow(id, false) ?: notFound()
        val reason = if (input.status == "active") null else input.reason?.takeIf { it in STATUS_REASONS } ?: "Другое"
        WebRepo.setStudentStatus(id, input.status, reason, c.id)
        AuditService.log(
            c.id, "Статус", "student", id, "${old.last} ${old.first}",
            listOf(Change("статус", STATUS_TITLES[old.status], title + (reason?.let { " · $it" } ?: ""))), DataSource.WEB, id,
        )
        return WebRepo.studentRow(id, c.seesAll) ?: notFound()
    }

    context(_: Session)
    fun addNote(c: Caller, id: Int, raw: String): List<NoteDto> {
        val text = raw.trim().take(2000).ifEmpty { badRequest("Пустая заметка.") }
        val row = WebRepo.studentRow(id, false) ?: notFound()
        WebRepo.addNote(id, c.id, text)
        AuditService.log(
            c.id, "Заметка", "student", id, "${row.last} ${row.first}",
            listOf(Change("заметка", null, if (text.length > 40) text.take(40) + "…" else text)), DataSource.WEB, id,
        )
        return WebRepo.notes(id)
    }

    /** Soft delete: the student leaves search and forms, their lessons stay in the reports. */
    context(_: Session)
    fun setDeleted(c: Caller, id: Int, deleted: Boolean): StudentRowDto {
        val row = WebRepo.studentRow(id, false) ?: notFound()
        WebRepo.setStudentDeleted(id, deleted, c.id)
        StudentService.refresh()
        AuditService.log(
            c.id, if (deleted) "Удалено" else "Восстановлено", "student", id, "${row.last} ${row.first}",
            listOf(Change("удалён", row.deleted, deleted)), DataSource.WEB, id,
        )
        return WebRepo.studentRow(id, c.seesAll) ?: notFound()
    }

    private fun clean(i: StudentInput) =
        i.copy(last = i.last.trim(), first = i.first.trim(), middle = i.middle.trim(), source = i.source?.trim()?.takeIf { it.isNotEmpty() })

    context(_: Session)
    private fun validate(input: StudentInput, except: Int?) {
        if (input.last.isBlank() || input.first.isBlank()) badRequest("Введите фамилию и имя.")
        if (input.cls == null || !SchoolClass.validate(input.cls)) badRequest("Класс от 1 до 11.")
        val duplicate = StudentMapper.findAll().any {
            it.id.id != except && it.secondName.trim().equals(input.last, true) && it.name.trim().equals(input.first, true)
        }
        if (duplicate) badRequest("Ученик с такими фамилией и именем уже есть.")
    }

    /** Phone digits as the bot stores them, 7XXXXXXXXXX; null for an empty field. */
    private fun phone(raw: String?): String? {
        var d = raw.orEmpty().filter { it.isDigit() }
        if (d.isEmpty()) return null
        if (d.startsWith("8")) d = "7" + d.drop(1) else if (d.startsWith("9")) d = "7$d"
        if (!PhoneNumber.validate(d)) badRequest("Телефон $raw: нужно 10 цифр после +7.")
        return d
    }
}
