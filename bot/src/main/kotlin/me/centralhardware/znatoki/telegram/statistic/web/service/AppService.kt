package me.centralhardware.znatoki.telegram.statistic.web.service

import kotliquery.Session
import io.ktor.http.HttpStatusCode
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.SourceOption
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.service.StudentService
import me.centralhardware.znatoki.telegram.statistic.web.*
import me.centralhardware.znatoki.telegram.statistic.web.http.HttpError
import me.centralhardware.znatoki.telegram.statistic.web.http.forbidden
import me.centralhardware.znatoki.telegram.statistic.web.http.notFound
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** What the app loads once on start, plus the bits that belong to no single section. */
object AppService {

    private val accessRequests = ConcurrentHashMap<Long, LocalDateTime>()

    /** Who the user is, what they may do, and the reference data every screen needs. */
    context(_: Session)
    fun boot(c: Caller): BootDto {
        val first = WebRepo.firstMonth() ?: YearMonth.now()
        val now = YearMonth.now()
        return BootDto(
            me = MeDto(c.id, c.tutor.name, c.tutor.permissions.map { it.name }, c.tutor.subjects.map { it.id }),
            caps = CapsDto(
                time = c.canTime, payment = c.canPayment, readPayments = c.readsPayments, client = c.canClient,
                admin = c.isAdmin, director = c.isDirector, manageTutors = c.canManageTutors,
                forceGroup = c.canForceGroup, extraHalfHour = c.canExtraHalfHour,
                timeForOthers = c.canTimeForOthers, paymentForOthers = c.canPaymentForOthers, seesAll = c.seesAll,
            ),
            today = LocalDate.now().toString(),
            months = generateSequence(first) { it.plusMonths(1) }.takeWhile { !it.isAfter(now) }.map { it.toString() }.toList(),
            subjects = WebRepo.subjects(),
            tutors = TutorMapper.getAll().map {
                TutorBriefDto(it.id.id, it.name, it.subjects.map { s -> s.id }, it.subjects.isNotEmpty() || Permissions.ADD_TIME in it.permissions, it.permissions.isNotEmpty())
            }.sortedBy { it.name },
            sources = (WebRepo.usedSources() + SourceOption.options()).distinctBy { it.lowercase() },
            reasons = STATUS_REASONS,
        )
    }

    /** "Запросить доступ" on the no-access screen: tells the admins who is asking, at most once an hour. */
    context(_: Session)
    fun requestAccess(initData: String): OkDto {
        val u = InitDataValidator.validate(initData)?.user
            ?: throw HttpError(HttpStatusCode.Unauthorized, "Не удалось подтвердить вход через Telegram.", "auth")
        if (accessRequests[u.id]?.isAfter(LocalDateTime.now().minusHours(1)) == true) return OkDto()
        accessRequests[u.id] = LocalDateTime.now()
        val known = TutorMapper.findByIdOrNull(TutorId(u.id))
        Notifications.sendTo(
            Permissions.ADMIN,
            buildString {
                append("🔑 Запрос доступа к веб-интерфейсу: ${u.displayName}")
                u.username?.let { append(" (@$it)") }
                append(", Telegram ID ${u.id}.")
                append(if (known != null) " Уже в команде как «${known.name}», нет права WEB_INTERFACE." else " Пока не в команде.")
            },
            "tab=tutors",
        )
        return OkDto()
    }

    /** The lesson photo or payment receipt from MinIO, for the record's card. */
    context(_: Session)
    fun photo(c: Caller, kind: String, id: String): ByteArray {
        val key = when (kind) {
            "lesson" -> WebRepo.lesson(runCatching { UUID.fromString(id) }.getOrNull() ?: notFound())
                ?.also { if (!c.seesAll && it.tutor != c.id && it.addedBy != c.id) forbidden() }?.photo
            "payment" -> {
                if (!c.readsPayments) forbidden()
                WebRepo.payment(id.toIntOrNull() ?: notFound())?.also { if (!c.seesAll && it.tutor != c.id && it.addedBy != c.id) forbidden() }?.photo
            }
            else -> null
        } ?: notFound("Фото нет.")
        return MinioService.getBytes(key).getOrElse { notFound("Фото не найдено в хранилище.") }
    }

    /** Global search: students by fuzzy name, plus the lessons and payments the caller may see. */
    context(_: Session)
    fun search(c: Caller, raw: String?): SearchDto {
        val q = raw?.trim().orEmpty()
        if (q.length < 2) return SearchDto(emptyList(), emptyList(), emptyList())
        val names = WebRepo.studentNames()
        val scope = if (c.seesAll) null else c.id
        val month = YearMonth.now()
        val found = StudentService.search(q).map { it.id.id }.filter { names[it]?.deleted == false }
        val rows = WebRepo.studentRows(includeDeleted = false, withBalance = c.seesAll).associateBy { it.id }
        val subjectHit = WebRepo.subjects().filter { it.name.contains(q, true) }.map { it.id }.toSet()
        val amount = q.toIntOrNull()

        val lessons = WebRepo.lessonsOfStudents(found, scope, 6) +
            if (subjectHit.isEmpty() && amount == null) emptyList()
            else WebRepo.lessons(month.start(), month.end(), scope).filter { !it.deleted && (it.subject in subjectHit || it.amount == amount) }
        val payments = if (!c.readsPayments) emptyList() else WebRepo.paymentsOfStudents(found, scope, 5) +
            if (amount == null) emptyList() else WebRepo.payments(month.start(), month.end(), scope).filter { !it.deleted && it.amount == amount }

        return SearchDto(
            found.mapNotNull { rows[it] }.take(5),
            lessons.distinctBy { it.id }.take(8).map { it.toDto(names) },
            payments.distinctBy { it.id }.take(6).map { it.toDto(names) },
        )
    }
}
