package me.centralhardware.znatoki.telegram.statistic.web.service

import io.ktor.http.HttpStatusCode
import me.centralhardware.znatoki.telegram.statistic.entity.Amount
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.Lesson
import me.centralhardware.znatoki.telegram.statistic.entity.LessonId
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import kotliquery.Session
import me.centralhardware.znatoki.telegram.statistic.extensions.db
import me.centralhardware.znatoki.telegram.statistic.extensions.transaction
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change
import me.centralhardware.znatoki.telegram.statistic.service.LessonCreator
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.service.NewLesson
import me.centralhardware.znatoki.telegram.statistic.web.*
import me.centralhardware.znatoki.telegram.statistic.web.http.HttpError
import me.centralhardware.znatoki.telegram.statistic.web.http.badRequest
import me.centralhardware.znatoki.telegram.statistic.web.http.invalidAsBadRequest
import me.centralhardware.znatoki.telegram.statistic.web.http.forbidden
import me.centralhardware.znatoki.telegram.statistic.web.http.notFound
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID

/** Lessons: the feed, the lesson card and its toggles, the add-lesson form and admin bulk edits. */
object LessonService {

    context(_: Session)
    fun list(c: Caller, month: YearMonth, tutor: Long?): List<LessonDto> {
        val list = WebRepo.lessons(month.start(), month.end(), tutor, withAdded = !c.seesAll)
        val flags = if (c.seesAll) anomalies(list) else emptyMap()
        val names = WebRepo.studentNames()
        return list.map { it.toDto(names, flags[it.id].orEmpty()) }
    }

    context(_: Session)
    fun get(c: Caller, id: UUID): LessonDto {
        val l = visible(c, id)
        val flags = if (c.seesAll) anomalies(listOf(l))[l.id].orEmpty() else emptyList()
        return l.toDto(WebRepo.studentNames(), flags)
    }

    /** The price of this student's last lesson in the subject, which the form offers again. */
    context(_: Session)
    fun lastPrice(c: Caller, student: Int, subject: Long, tutor: Long?): LastPriceDto {
        val last = WebRepo.lastLessonAmount(student, subject, tutor?.takeIf { c.canTimeForOthers } ?: c.id)
        return LastPriceDto(last?.first?.toString(), last?.second?.toLocalDate()?.toString())
    }

    context(_: Session)
    suspend fun create(c: Caller, input: LessonInput, photo: ByteArray?): LessonDto {
        photo ?: badRequest("Приложите фото отчётности.")
        val tutor = targetTutor(c, input.tutor)
        val names = WebRepo.studentNames()
        val slot = input.slot?.let { WebRepo.slot(it, names) }?.also { if (it.tutor != c.id && !c.isAdmin) forbidden() }
        val dateTime = lessonTime(c, input, slot)
        val new = NewLesson(
            tutor = tutor, subject = SubjectId(input.subject), students = input.students, amount = input.amount,
            dateTime = dateTime, photoKey = "", enteredBy = c.tutorId, byAdmin = c.isAdmin, source = DataSource.WEB,
        )
        // Checked before the upload too, so a rejected form leaves no photo behind.
        invalidAsBadRequest { LessonCreator.validate(new) }
        val key = MinioService.upload(photo, LocalDateTime.now()).getOrElse { throw HttpError(HttpStatusCode.BadGateway, "Не удалось сохранить фото. Попробуйте ещё раз.") }

        val rows = invalidAsBadRequest {
            LessonCreator.create(
                Notifications.bot, new.copy(photoKey = key),
                extraChanges = listOfNotNull(
                    slot?.let { Change("из расписания", null, it.time) },
                    Change("дата", null, ruDay(dateTime.toLocalDate()) + " " + dateTime.format(HM)).takeIf { input.date != null && c.isAdmin },
                ),
                alsoInTransaction = { rows -> slot?.let { WebRepo.setMark(it.id, LocalDate.now(), "done", rows.first().id.id) } },
            )
        }
        val lesson = WebRepo.lesson(rows.first().id.id)!!
        val title = lessonTitle(lesson, names, subjectNames())
        if (lesson.month != YearMonth.now()) AuditAlerts.closedMonthEdit(c.id, c.tutor.name, "добавлено занятие $title", lesson.month)
        return lesson.toDto(names)
    }

    context(_: Session)
    fun update(c: Caller, id: UUID, p: LessonPatch): LessonDto = transaction {
        val l = WebRepo.lesson(id) ?: notFound()
        c.editDenied(l.month, l.tutor, c.canTime)?.let { forbidden(it) }
        val changes = mutableListOf<Change>()
        var action = "Изменено"
        p.half?.takeIf { it != l.half }?.let { action = "Полтора часа"; changes += setHalf(c, l, it) }
        p.forceGroup?.takeIf { it != l.forceGroup }?.let { action = "Тип занятия"; changes += setForceGroup(c, l, it) }
        p.deleted?.takeIf { it != l.deleted }?.let {
            WebRepo.setLessonDeleted(l.id, it)
            changes += Change("удалено", l.deleted, it); action = if (it) "Удалено" else "Восстановлено"
        }
        if (p.amount != null || p.subject != null || p.tutor != null || p.date != null || p.time != null) {
            if (!c.isAdmin) forbidden("Изменять занятие может только админ.")
            changes += adminEdit(l, p)
        }
        val after = WebRepo.lesson(l.id)!!
        val names = WebRepo.studentNames()
        if (changes.isNotEmpty()) {
            val title = lessonTitle(after, names, subjectNames())
            AuditService.log(c.id, if (changes.size > 1) "Изменено" else action, "lesson", l.id, title, changes, DataSource.WEB, l.students.first(), after.subject)
            if (l.month != YearMonth.now()) AuditAlerts.closedMonthEdit(c.id, c.tutor.name, "занятие $title", l.month)
        }
        after.toDto(names)
    }

    /** Admin "Управление": delete, restore, re-assign, re-price or move several lessons at once. */
    context(_: Session)
    fun bulk(c: Caller, input: BulkInput): ChangedDto = transaction { bulkTx(c, input) }

    context(_: Session)
    private fun bulkTx(c: Caller, input: BulkInput): ChangedDto {
        val names = WebRepo.studentNames()
        val subjects = subjectNames()
        val tutors = TutorMapper.getAll().associate { it.id.id to it.name }
        val lessons = input.ids.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull()?.let { WebRepo.lesson(it) } }
        val action = when (input.action) { "delete" -> "Удалено"; "restore" -> "Восстановлено"; else -> "Изменено" }
        var changed = 0
        lessons.forEach { l ->
            val change = when (input.action) {
                "delete", "restore" -> bulkDelete(l, input.action == "delete")
                "tutor" -> bulkTutor(l, input.value?.toLongOrNull()?.takeIf { it in tutors } ?: badRequest("Репетитор не найден."), tutors)
                "price" -> bulkPrice(l, input.value?.toIntOrNull()?.takeIf { Amount.validate(it) } ?: badRequest("Стоимость должна быть больше нуля."))
                "day" -> bulkDay(l, input.value?.toIntOrNull() ?: badRequest("Укажите день."))
                else -> badRequest("Неизвестное действие.")
            } ?: return@forEach
            changed++
            AuditService.log(c.id, action, "lesson", l.id, lessonTitle(l, names, subjects), listOf(change), DataSource.WEB, l.students.first(), l.subject)
        }
        lessons.map { it.month }.distinct().filter { it != YearMonth.now() }
            .forEach { AuditAlerts.closedMonthEdit(c.id, c.tutor.name, "массовое изменение занятий ($changed)", it) }
        return ChangedDto(changed)
    }

    // ---------- create ----------

    context(_: Session)
    private fun targetTutor(c: Caller, requested: Long?): Tutor {
        val id = requested?.takeIf { it != c.id } ?: return c.tutor
        if (!c.canTimeForOthers) forbidden("Нет права вносить занятия за других.")
        return TutorMapper.findByIdOrNull(TutorId(id)) ?: badRequest("Репетитор не найден.")
    }

    /** Now; the slot's time for a lesson entered from the schedule; any past date for an admin. */
    private fun lessonTime(c: Caller, input: LessonInput, slot: SlotDto?): LocalDateTime {
        val now = LocalDateTime.now()
        if (c.isAdmin && input.date != null) {
            val d = parseDate(input.date)
            if (d.isAfter(LocalDate.now())) badRequest("Дата не может быть в будущем.")
            return d.atTime(parseTime(input.time) ?: now.toLocalTime().withSecond(0).withNano(0))
        }
        return slot?.let { LocalDate.now().atTime(parseTime(it.time) ?: now.toLocalTime()) } ?: now
    }

    // ---------- update ----------

    context(_: Session)
    private fun visible(c: Caller, id: UUID): LessonGroup =
        (WebRepo.lesson(id) ?: notFound()).also { if (!c.seesAll && it.tutor != c.id && it.addedBy != c.id) forbidden() }

    context(_: Session)
    private fun setHalf(c: Caller, l: LessonGroup, value: Boolean): Change {
        if (!c.canExtraHalfHour) forbidden("Нет права ставить полтора часа.")
        LessonMapper.setExtraHalfHour(LessonId(l.id), value)
        return Change("полтора часа", l.half, value)
    }

    context(_: Session)
    private fun setForceGroup(c: Caller, l: LessonGroup, value: Boolean): Change {
        if (!c.canForceGroup) forbidden("Нет права делать занятие групповым.")
        if (l.students.size > 1) badRequest("В занятии несколько учеников, оно и так групповое.")
        LessonMapper.setForceGroup(LessonId(l.id), value)
        return Change("групповое", l.forceGroup, value)
    }

    /** Price, subject, tutor, date and time: admins only, in any month. */
    context(_: Session)
    private fun adminEdit(l: LessonGroup, p: LessonPatch): List<Change> {
        val subjects = subjectNames()
        val tutors = TutorMapper.getAll().associate { it.id.id to it.name }
        val fields = mutableMapOf<String, Any?>()
        val changes = mutableListOf<Change>()
        p.amount?.takeIf { it != l.amount }?.let {
            if (!Amount.validate(it)) badRequest("Стоимость должна быть больше нуля.")
            fields["amount"] = it; changes += Change("стоимость", l.amount, it)
        }
        p.subject?.takeIf { it != l.subject }?.let {
            if (it !in subjects) badRequest("Предмет не найден.")
            fields["subject_id"] = it; changes += Change("предмет", subjects[l.subject], subjects[it])
        }
        p.tutor?.takeIf { it != l.tutor }?.let {
            if (it !in tutors) badRequest("Репетитор не найден.")
            fields["tutor_id"] = it; changes += Change("репетитор", tutors[l.tutor], tutors[it])
        }
        val date = p.date?.let(::parseDate) ?: l.dateTime.toLocalDate()
        val time = parseTime(p.time) ?: l.dateTime.toLocalTime()
        if (date.atTime(time) != l.dateTime) {
            if (date.isAfter(LocalDate.now())) badRequest("Дата не может быть в будущем.")
            fields["date_time"] = date.atTime(time)
            if (date != l.dateTime.toLocalDate()) changes += Change("дата", ruDay(l.dateTime.toLocalDate()), ruDay(date))
            if (time != l.dateTime.toLocalTime()) changes += Change("время", l.dateTime.format(HM), time.format(HM))
        }
        WebRepo.setLessonFields(l.id, fields)
        return changes
    }

    // ---------- bulk ----------

    context(_: Session)
    private fun bulkDelete(l: LessonGroup, deleted: Boolean): Change? =
        if (l.deleted == deleted) null else { WebRepo.setLessonDeleted(l.id, deleted); Change("удалено", l.deleted, deleted) }

    context(_: Session)
    private fun bulkTutor(l: LessonGroup, tutor: Long, names: Map<Long, String>): Change? =
        if (tutor == l.tutor) null else { WebRepo.setLessonFields(l.id, mapOf("tutor_id" to tutor)); Change("репетитор", names[l.tutor], names[tutor]) }

    context(_: Session)
    private fun bulkPrice(l: LessonGroup, amount: Int): Change? =
        if (amount == l.amount) null else { WebRepo.setLessonFields(l.id, mapOf("amount" to amount)); Change("стоимость", l.amount, amount) }

    /** Moves the lesson to another day of its own month, keeping the time. */
    context(_: Session)
    private fun bulkDay(l: LessonGroup, day: Int): Change? {
        if (day !in 1..l.month.lengthOfMonth()) badRequest("День от 1 до ${l.month.lengthOfMonth()}.")
        val d = l.month.atDay(day)
        if (d.isAfter(LocalDate.now())) badRequest("Дата не может быть в будущем.")
        if (d == l.dateTime.toLocalDate()) return null
        WebRepo.setLessonFields(l.id, mapOf("date_time" to d.atTime(l.dateTime.toLocalTime())))
        return Change("дата", ruDay(l.dateTime.toLocalDate()), ruDay(d))
    }

    // ---------- presentation ----------

    fun lessonTitle(l: LessonGroup, names: Map<Int, StudentName>, subjects: Map<Long, String>) =
        l.students.joinToString(", ") { names[it]?.short ?: "#$it" } + " · " + (subjects[l.subject] ?: "")

    /** Lessons that look wrong to an admin: an exact duplicate, a price far from usual, a Sunday. */
    context(_: Session)
    fun anomalies(list: List<LessonGroup>): Map<UUID, List<String>> {
        val medians = WebRepo.medianAmounts()
        val live = list.filterNot { it.deleted }
        val duplicates = live.groupBy { listOf(it.tutor, it.subject, it.dateTime.withSecond(0).withNano(0), it.students) }
            .filterValues { it.size > 1 }.values.flatten().map { it.id }.toSet()
        return live.associate { l ->
            val med = medians[l.subject]
            l.id to listOfNotNull(
                "дубль".takeIf { l.id in duplicates },
                "необычная цена".takeIf { med != null && med > 0 && (l.amount >= med * 2 || l.amount <= med / 2) },
            )
        }.filterValues { it.isNotEmpty() }
    }
}

fun LessonGroup.toDto(names: Map<Int, StudentName>, flags: List<String> = emptyList()) = LessonDto(
    id = id.toString(),
    date = dateTime.toLocalDate().toString(),
    time = dateTime.format(HM),
    subject = subject,
    students = students.map { names[it]?.ref() ?: StudentRefDto(it, "#$it") },
    amount = amount,
    half = half,
    forceGroup = forceGroup,
    deleted = deleted,
    tutor = tutor,
    addedBy = addedBy,
    source = source,
    hasPhoto = photo != null,
    flags = flags,
)

fun parseDate(s: String): LocalDate = runCatching { LocalDate.parse(s) }.getOrNull() ?: badRequest("Неверная дата.")
fun parseTime(s: String?): LocalTime? = s?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
