package me.centralhardware.znatoki.telegram.statistic.web

import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.utils.io.toByteArray
import kotlinx.serialization.Serializable
import me.centralhardware.znatoki.telegram.statistic.entity.Amount
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.Lesson
import me.centralhardware.znatoki.telegram.statistic.entity.LessonId
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

private val HM = DateTimeFormatter.ofPattern("HH:mm")
private val RU_MONTHS = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
fun ruDay(d: LocalDate) = "${d.dayOfMonth} ${RU_MONTHS[d.monthValue - 1]}"
fun rub(v: Number) = "%,d ₽".format(v.toLong()).replace(',', ' ')

@Serializable
data class LessonInput(
    val tutor: Long? = null,
    val subject: Long,
    val students: List<Int>,
    val amount: Int,
    val date: String? = null,
    val time: String? = null,
    val slot: Int? = null,
)

/** Reads a multipart form with a JSON `data` field and an optional `photo` file. */
suspend fun ApplicationCall.receiveForm(): Pair<String, ByteArray?> {
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
    return (data ?: badRequest("Нет данных формы.")) to photo
}

fun lessonTitle(l: LessonGroup, names: Map<Int, StudentName>, subjects: Map<Long, String>) =
    l.students.joinToString(", ") { names[it]?.short ?: "#$it" } + " · " + (subjects[l.subject] ?: "")

fun subjectNames() = WebRepo.subjects().associate { it.id to it.name }

/** Lessons that look wrong to an admin: an exact duplicate, a price far from usual, a Sunday. */
fun anomalies(list: List<LessonGroup>): Map<UUID, List<String>> {
    val medians = WebRepo.medianAmounts()
    val live = list.filterNot { it.deleted }
    val dupKeys = live.groupBy { listOf(it.tutor, it.subject, it.dateTime.withSecond(0).withNano(0), it.students) }
        .filterValues { it.size > 1 }.values.flatten().map { it.id }.toSet()
    return live.associate { l ->
        val flags = mutableListOf<String>()
        if (l.id in dupKeys) flags += "дубль"
        val med = medians[l.subject]
        if (med != null && med > 0 && (l.amount >= med * 2 || l.amount <= med / 2)) flags += "необычная цена"
        if (WebRepo.isSunday(l.dateTime)) flags += "воскресенье"
        l.id to flags
    }.filterValues { it.isNotEmpty() }
}

fun LessonGroup.toDto(names: Map<Int, StudentName>, flags: List<String> = emptyList()) = LessonDto(
    id = id.toString(),
    date = dateTime.toLocalDate().toString(),
    time = dateTime.toLocalTime().format(HM),
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

/** Which tutor's records a list shows: "all" and other tutors only for admins and the director. */
fun Caller.tutorFilter(param: String?): Long? = when {
    !seesAll -> id
    param == null || param == "all" -> null
    param == "me" -> id
    else -> param.toLongOrNull() ?: id
}

/** Tutors and regular staff see the current month only; admins and the director any month. */
fun Caller.monthParam(param: String?): YearMonth =
    if (seesAll) parseMonth(param) ?: YearMonth.now() else YearMonth.now()

fun uuidParam(s: String?): UUID = s?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: notFound()

private fun parseTime(s: String?): LocalTime? = s?.let { runCatching { LocalTime.parse(it) }.getOrNull() }

fun Route.lessonRoutes(ctx: WebContext) {
    apiGet("/lessons") { c ->
        val month = c.monthParam(call.request.queryParameters["month"])
        val list = WebRepo.lessons(month.start(), month.end(), c.tutorFilter(call.request.queryParameters["tutor"]))
        val names = WebRepo.studentNames()
        val flags = if (c.seesAll) anomalies(list) else emptyMap()
        call.respond(list.map { it.toDto(names, flags[it.id].orEmpty()) })
    }

    apiGet("/lessons/last-price") { c ->
        val q = call.request.queryParameters
        val student = q["student"]?.toIntOrNull() ?: badRequest("Нет ученика.")
        val subject = q["subject"]?.toLongOrNull() ?: badRequest("Нет предмета.")
        val tutor = q["tutor"]?.toLongOrNull()?.takeIf { c.canTimeForOthers } ?: c.id
        val last = WebRepo.lastLessonAmount(student, subject, tutor)
        call.respond(mapOf("amount" to last?.first?.toString(), "date" to last?.second?.toLocalDate()?.toString()))
    }

    apiGet("/lessons/{id}") { c ->
        val l = WebRepo.lesson(uuidParam(call.parameters["id"])) ?: notFound()
        if (!c.seesAll && l.tutor != c.id) forbidden()
        val flags = if (c.seesAll) anomalies(listOf(l))[l.id].orEmpty() else emptyList()
        call.respond(l.toDto(WebRepo.studentNames(), flags))
    }

    post("/lessons") {
        val c = call.caller
        if (!c.canTime) forbidden("Нет права вносить занятия.")
        val (raw, photo) = call.receiveForm()
        val input = WebServer.json.decodeFromString<LessonInput>(raw)
        photo ?: badRequest("Приложите фото отчётности.")

        val tutorId = input.tutor?.takeIf { it != c.id }?.also { if (!c.canTimeForOthers) forbidden("Нет права вносить занятия за других.") } ?: c.id
        val tutor = TutorMapper.findByIdOrNull(TutorId(tutorId)) ?: badRequest("Репетитор не найден.")
        val subjects = WebRepo.subjects().associateBy { it.id }
        val subject = subjects[input.subject] ?: badRequest("Предмет не найден.")
        if (!c.isAdmin && tutor.subjects.isNotEmpty() && SubjectId(subject.id) !in tutor.subjects) badRequest("Этот предмет не закреплён за репетитором.")
        if (input.students.isEmpty()) badRequest("Выберите ученика.")
        if (input.students.size > 1 && !subject.group) badRequest("${subject.name}: только индивидуальные занятия.")
        if (!Amount.validate(input.amount)) badRequest("Стоимость должна быть больше нуля.")
        val names = WebRepo.studentNames()
        input.students.forEach { if (names[it]?.deleted != false) badRequest("Ученик не найден или удалён.") }

        val slot = input.slot?.let { WebRepo.slot(it, names) }?.also { if (it.tutor != c.id && !c.isAdmin) forbidden() }
        val now = LocalDateTime.now()
        val dateTime = when {
            c.isAdmin && input.date != null -> {
                val d = runCatching { LocalDate.parse(input.date) }.getOrNull() ?: badRequest("Неверная дата.")
                if (d.isAfter(LocalDate.now())) badRequest("Дата не может быть в будущем.")
                d.atTime(parseTime(input.time) ?: now.toLocalTime().withSecond(0).withNano(0))
            }
            slot != null -> LocalDate.now().atTime(parseTime(slot.time) ?: now.toLocalTime())
            else -> now
        }

        val key = MinioService.upload(photo, now).getOrElse { throw HttpError(io.ktor.http.HttpStatusCode.BadGateway, "Не удалось сохранить фото. Попробуйте ещё раз.") }
        val id = LessonId.random()
        val rows = input.students.distinct().map {
            Lesson(
                id = id, dateTime = dateTime, updateTime = now, tutorId = TutorId(tutorId), subjectId = SubjectId(subject.id),
                studentId = it.toStudentId(), _amount = Amount(input.amount), photoReport = key,
                addedByTutorId = if (tutorId != c.id) c.tutorId else null, dataSource = DataSource.WEB,
            )
        }
        runCatching { LessonMapper.insertAll(rows) }.onFailure { MinioService.delete(key); throw it }
        slot?.let { WebRepo.setMark(it.id, LocalDate.now(), "done", id.id) }

        val group = WebRepo.lesson(id.id)!!
        val title = lessonTitle(group, names, subjects.mapValues { it.value.name })
        AuditService.log(
            c.id, "Создано", "lesson", id.id, title,
            listOfNotNull(
                Change("стоимость", null, input.amount), Change("учеников", null, rows.size),
                Change("репетитор", null, tutor.name).takeIf { tutorId != c.id },
                Change("из расписания", null, slot?.let { "${it.time}" }).takeIf { slot != null },
                Change("дата", null, ruDay(dateTime.toLocalDate()) + " " + dateTime.toLocalTime().format(HM)).takeIf { c.isAdmin && input.date != null },
            ),
            DataSource.WEB, input.students.first(), subject.id,
        )
        if (c.isAdmin && YearMonth.from(dateTime) != YearMonth.now()) {
            ctx.alerts.closedMonthEdit(c.id, c.tutor.name, "добавлено занятие $title", YearMonth.from(dateTime).toString())
        }
        ctx.notifier.launch { sendWebLessonLog(rows, if (tutorId != c.id) c.tutorId else null) }
        call.respond(group.toDto(names))
    }

    patch("/lessons/{id}") {
        val c = call.caller
        val l = WebRepo.lesson(uuidParam(call.parameters["id"])) ?: notFound()
        val p = call.receive<LessonPatch>()
        val names = WebRepo.studentNames()
        val subjects = subjectNames()
        val tutorNames = TutorMapper.getAll().associate { it.id.id to it.name }
        c.editDenied(l.month, l.tutor, c.canTime)?.let { forbidden(it) }
        val changes = mutableListOf<Change>()
        var action = "Изменено"

        p.half?.takeIf { it != l.half }?.let {
            if (!c.canExtraHalfHour) forbidden("Нет права ставить полтора часа.")
            LessonMapper.setExtraHalfHour(LessonId(l.id), it)
            changes += Change("полтора часа", l.half, it); action = "Полтора часа"
        }
        p.forceGroup?.takeIf { it != l.forceGroup }?.let {
            if (!c.canForceGroup) forbidden("Нет права делать занятие групповым.")
            if (l.students.size > 1) badRequest("В занятии несколько учеников, оно и так групповое.")
            LessonMapper.setForceGroup(LessonId(l.id), it)
            changes += Change("групповое", l.forceGroup, it); action = "Тип занятия"
        }
        p.deleted?.takeIf { it != l.deleted }?.let {
            WebRepo.setLessonDeleted(l.id, it)
            changes += Change("удалено", l.deleted, it); action = if (it) "Удалено" else "Восстановлено"
        }
        if (p.amount != null || p.subject != null || p.tutor != null || p.date != null || p.time != null) {
            if (!c.isAdmin) forbidden("Изменять занятие может только админ.")
            val fields = mutableMapOf<String, Any?>()
            p.amount?.takeIf { it != l.amount }?.let {
                if (!Amount.validate(it)) badRequest("Стоимость должна быть больше нуля.")
                fields["amount"] = it; changes += Change("стоимость", l.amount, it)
            }
            p.subject?.takeIf { it != l.subject }?.let {
                if (it !in subjects) badRequest("Предмет не найден.")
                fields["subject_id"] = it; changes += Change("предмет", subjects[l.subject], subjects[it])
            }
            p.tutor?.takeIf { it != l.tutor }?.let {
                if (it !in tutorNames) badRequest("Репетитор не найден.")
                fields["tutor_id"] = it; changes += Change("репетитор", tutorNames[l.tutor], tutorNames[it])
            }
            val date = p.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() ?: badRequest("Неверная дата.") } ?: l.dateTime.toLocalDate()
            val time = parseTime(p.time) ?: l.dateTime.toLocalTime()
            val dt = date.atTime(time)
            if (dt != l.dateTime) {
                if (date.isAfter(LocalDate.now())) badRequest("Дата не может быть в будущем.")
                fields["date_time"] = dt
                if (date != l.dateTime.toLocalDate()) changes += Change("дата", ruDay(l.dateTime.toLocalDate()), ruDay(date))
                if (time != l.dateTime.toLocalTime()) changes += Change("время", l.dateTime.toLocalTime().format(HM), time.format(HM))
            }
            WebRepo.setLessonFields(l.id, fields)
        }
        if (changes.isNotEmpty()) {
            val after = WebRepo.lesson(l.id)!!
            val title = lessonTitle(after, names, subjects)
            AuditService.log(c.id, if (changes.size > 1) "Изменено" else action, "lesson", l.id, title, changes, DataSource.WEB, l.students.first(), after.subject)
            if (c.isAdmin && l.month != YearMonth.now()) ctx.alerts.closedMonthEdit(c.id, c.tutor.name, "занятие $title", l.month.toString())
        }
        call.respond(WebRepo.lesson(l.id)!!.toDto(names))
    }

    /** Admin "Управление": delete, restore, re-assign, re-price or move several lessons at once. */
    post("/lessons/bulk") {
        val c = call.caller
        if (!c.isAdmin) forbidden("Массовые действия доступны только админу.")
        val input = call.receive<BulkInput>()
        val names = WebRepo.studentNames()
        val subjects = subjectNames()
        val tutorNames = TutorMapper.getAll().associate { it.id.id to it.name }
        val lessons = input.ids.mapNotNull { WebRepo.lesson(uuidParam(it)) }
        var changed = 0
        lessons.forEach { l ->
            val change: Change? = when (input.action) {
                "delete", "restore" -> {
                    val v = input.action == "delete"
                    if (l.deleted == v) null else { WebRepo.setLessonDeleted(l.id, v); Change("удалено", l.deleted, v) }
                }
                "tutor" -> {
                    val t = input.value?.toLongOrNull()?.takeIf { it in tutorNames } ?: badRequest("Репетитор не найден.")
                    if (t == l.tutor) null else { WebRepo.setLessonFields(l.id, mapOf("tutor_id" to t)); Change("репетитор", tutorNames[l.tutor], tutorNames[t]) }
                }
                "price" -> {
                    val a = input.value?.toIntOrNull()?.takeIf { Amount.validate(it) } ?: badRequest("Стоимость должна быть больше нуля.")
                    if (a == l.amount) null else { WebRepo.setLessonFields(l.id, mapOf("amount" to a)); Change("стоимость", l.amount, a) }
                }
                "day" -> {
                    val day = input.value?.toIntOrNull() ?: badRequest("Укажите день.")
                    val m = l.month
                    if (day !in 1..m.lengthOfMonth()) badRequest("День от 1 до ${m.lengthOfMonth()}.")
                    val d = m.atDay(day)
                    if (d.isAfter(LocalDate.now())) badRequest("Дата не может быть в будущем.")
                    if (d == l.dateTime.toLocalDate()) null
                    else { WebRepo.setLessonFields(l.id, mapOf("date_time" to d.atTime(l.dateTime.toLocalTime()))); Change("дата", ruDay(l.dateTime.toLocalDate()), ruDay(d)) }
                }
                else -> badRequest("Неизвестное действие.")
            }
            if (change != null) {
                changed++
                val action = when (input.action) { "delete" -> "Удалено"; "restore" -> "Восстановлено"; else -> "Изменено" }
                AuditService.log(c.id, action, "lesson", l.id, lessonTitle(l, names, subjects), listOf(change), DataSource.WEB, l.students.first(), l.subject)
            }
        }
        lessons.map { it.month }.distinct().filter { it != YearMonth.now() }.forEach {
            ctx.alerts.closedMonthEdit(c.id, c.tutor.name, "массовое изменение занятий ($changed)", it.toString())
        }
        call.respond(mapOf("changed" to changed))
    }
}
