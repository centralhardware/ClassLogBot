package me.centralhardware.znatoki.telegram.statistic.web

import kotliquery.Row
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle
import me.centralhardware.znatoki.telegram.statistic.extensions.update
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID


/** Billed price of one lessons row: the stored amount, ×1.5 for an extra half hour. */
const val PRICE_SQL = "(CASE WHEN extra_half_hour THEN amount * 1.5 ELSE amount END)"

fun YearMonth.start(): LocalDateTime = atDay(1).atStartOfDay()
fun YearMonth.end(): LocalDateTime = plusMonths(1).atDay(1).atStartOfDay()
fun parseMonth(s: String?): YearMonth? = s?.let { runCatching { YearMonth.parse(it) }.getOrNull() }

/** Student names as the web app shows them: last name, first name, patronymic. */
data class StudentName(val id: Int, val last: String, val first: String, val middle: String, val cls: Int?, val deleted: Boolean) {
    val fio get() = listOf(last, first, middle).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Неизвестно" }
    val short get() = listOf(last, first).filter { it.isNotBlank() }.joinToString(" ").ifBlank { fio }
    fun ref() = StudentRefDto(id, short, cls, deleted)
}

/** One lesson as the app sees it: the lessons rows sharing an id, one per student. */
data class LessonGroup(
    val id: UUID,
    val dateTime: LocalDateTime,
    val tutor: Long,
    val subject: Long,
    val amount: Int,
    val half: Boolean,
    val forceGroup: Boolean,
    val deleted: Boolean,
    val photo: String?,
    val addedBy: Long?,
    val source: String?,
    val students: List<Int>,
) {
    val isGroup get() = students.size > 1 || forceGroup
    val price get() = if (half) amount * 1.5 else amount.toDouble()
    val month: YearMonth get() = YearMonth.from(dateTime)
}

data class PaymentRow(
    val id: Int,
    val dateTime: LocalDateTime,
    val tutor: Long,
    val student: Int,
    val subject: Long,
    val amount: Int,
    val deleted: Boolean,
    val photo: String?,
    val addedBy: Long?,
    val source: String?,
) {
    val month: YearMonth get() = YearMonth.from(dateTime)
}

/** Subject names by id. */
fun subjectNames() = WebRepo.subjects().associate { it.id to it.name }

object WebRepo {

    // ---------- reference data ----------

    fun studentNames(): Map<Int, StudentName> = runList(
        queryOf("SELECT id, second_name, name, last_name, school_class, coalesce(deleted, false) AS deleted FROM students")
    ) {
        StudentName(
            it.int("id"), it.stringOrNull("second_name") ?: "", it.stringOrNull("name") ?: "",
            it.stringOrNull("last_name") ?: "", it.intOrNull("school_class"), it.boolean("deleted"),
        )
    }.associateBy { it.id }

    fun subjects(): List<SubjectDto> = runList(
        queryOf("SELECT id, name, coalesce(allow_multiply_clients, false) AS g FROM subjects ORDER BY name")
    ) { SubjectDto(it.long("id"), it.string("name"), it.boolean("g")) }

    fun firstMonth(): YearMonth? = runSingle(
        queryOf("SELECT min(date_time) AS m FROM lessons")
    ) { it.localDateTimeOrNull("m") }?.let { YearMonth.from(it) }

    fun usedSources(): List<String> = runList(
        queryOf(
            """
            SELECT source, count(*) AS n FROM students
            WHERE coalesce(deleted, false) = false AND coalesce(trim(source), '') <> ''
            GROUP BY source ORDER BY n DESC
            """
        )
    ) { it.string("source") }

    private fun tutorFilter(tutor: Long?, withAdded: Boolean) = when {
        tutor == null -> ""
        withAdded -> "AND (tutor_id = :p_t OR added_by_tutor_id = :p_t)"
        else -> "AND tutor_id = :p_t"
    }

    // ---------- lessons ----------

    private fun Row.toLessonGroup(): LessonGroup {
        val all = (array<Int>("students")).toList()
        val live = arrayOrNull<Int>("live")?.toList()?.filterNotNull().orEmpty()
        return LessonGroup(
            uuid("id"), localDateTime("dt"), long("tutor_id"), long("subject_id"), int("amount"),
            boolean("half"), boolean("fg"), boolean("deleted"), stringOrNull("photo"),
            longOrNull("added_by"), stringOrNull("src"), live.ifEmpty { all }.sorted(),
        )
    }

    private const val LESSON_GROUP_SELECT = """
        SELECT id, min(date_time) AS dt, max(tutor_id) AS tutor_id, max(subject_id) AS subject_id, max(amount) AS amount,
               bool_or(coalesce(extra_half_hour, false)) AS half, bool_or(force_group) AS fg,
               bool_and(coalesce(is_deleted, false)) AS deleted, max(photo_report) AS photo,
               max(added_by_tutor_id) AS added_by, max(data_source) AS src,
               array_agg(student_id) AS students,
               array_agg(student_id) FILTER (WHERE NOT coalesce(is_deleted, false)) AS live
        FROM lessons
    """

    /** [withAdded] also returns the lessons [tutor] entered on behalf of someone else (feeds, not totals). */
    fun lessons(from: LocalDateTime, to: LocalDateTime, tutor: Long?, withAdded: Boolean = false): List<LessonGroup> = runList(
        queryOf(
            "$LESSON_GROUP_SELECT WHERE date_time >= :from AND date_time < :to ${tutorFilter(tutor, withAdded)} GROUP BY id ORDER BY dt DESC",
            mapOf("from" to from, "to" to to, "p_t" to tutor),
        )
    ) { it.toLessonGroup() }

    fun lesson(id: UUID): LessonGroup? = runSingle(
        queryOf("$LESSON_GROUP_SELECT WHERE id = :id GROUP BY id", mapOf("id" to id))
    ) { it.toLessonGroup() }

    fun lessonsOfStudents(ids: Collection<Int>, tutor: Long?, limit: Int): List<LessonGroup> = if (ids.isEmpty()) emptyList() else runList(
        queryOf(
            """
            $LESSON_GROUP_SELECT
            WHERE id IN (SELECT id FROM lessons WHERE student_id = ANY(:ids) AND NOT coalesce(is_deleted, false))
            ${if (tutor != null) "AND tutor_id = :p_t AND date_time >= :from" else ""}
            GROUP BY id ORDER BY dt DESC LIMIT $limit
            """,
            mapOf("ids" to ids.toTypedArray(), "p_t" to tutor, "from" to YearMonth.now().start()),
        )
    ) { it.toLessonGroup() }

    /** Median stored amount per subject over the last half year, for the "unusual price" flag. */
    fun medianAmounts(): Map<Long, Double> = runList(
        queryOf(
            """
            SELECT subject_id, percentile_cont(0.5) WITHIN GROUP (ORDER BY amount) AS med
            FROM lessons WHERE NOT coalesce(is_deleted, false) AND date_time > now() - interval '180 days'
            GROUP BY subject_id
            """
        )
    ) { it.long("subject_id") to it.double("med") }.toMap()

    fun lastLessonAmount(student: Int, subject: Long, tutor: Long): Pair<Int, LocalDateTime>? = runSingle(
        queryOf(
            """
            SELECT amount, date_time FROM lessons
            WHERE student_id = :p_s AND subject_id = :subj AND tutor_id = :p_t AND NOT coalesce(is_deleted, false)
            ORDER BY date_time DESC LIMIT 1
            """,
            mapOf("p_s" to student, "subj" to subject, "p_t" to tutor),
        )
    ) { it.int("amount") to it.localDateTime("date_time") }

    fun setLessonFields(id: UUID, fields: Map<String, Any?>) {
        if (fields.isEmpty()) return
        val set = fields.keys.joinToString(", ") { "$it = :$it" }
        update(queryOf("UPDATE lessons SET $set, update_time = now() WHERE id = :id", fields + ("id" to id)))
    }

    fun setLessonDeleted(id: UUID, deleted: Boolean) =
        update(queryOf("UPDATE lessons SET is_deleted = :p_d, update_time = now() WHERE id = :id", mapOf("id" to id, "p_d" to deleted)))

    // ---------- payments ----------

    private fun Row.toPayment() = PaymentRow(
        int("id"), localDateTime("date_time"), long("tutor_id"), int("student_id"), long("subject_id"), int("amount"),
        boolean("is_deleted"), stringOrNull("photo_report"), longOrNull("added_by_tutor_id"), stringOrNull("data_source"),
    )

    private const val PAYMENT_SELECT = """
        SELECT id, date_time, tutor_id, student_id, subject_id, amount, coalesce(is_deleted, false) AS is_deleted,
               photo_report, added_by_tutor_id, data_source
        FROM payment
    """

    /** [withAdded] also returns the payments [tutor] entered on behalf of someone else (feeds, not totals). */
    fun payments(from: LocalDateTime, to: LocalDateTime, tutor: Long?, withAdded: Boolean = false): List<PaymentRow> = runList(
        queryOf(
            "$PAYMENT_SELECT WHERE date_time >= :from AND date_time < :to ${tutorFilter(tutor, withAdded)} ORDER BY date_time DESC",
            mapOf("from" to from, "to" to to, "p_t" to tutor),
        )
    ) { it.toPayment() }

    fun payment(id: Int): PaymentRow? =
        runSingle(queryOf("$PAYMENT_SELECT WHERE id = :id", mapOf("id" to id))) { it.toPayment() }

    fun paymentsOfStudents(ids: Collection<Int>, tutor: Long?, limit: Int): List<PaymentRow> = if (ids.isEmpty()) emptyList() else runList(
        queryOf(
            """
            $PAYMENT_SELECT WHERE student_id = ANY(:ids) AND NOT coalesce(is_deleted, false)
            ${if (tutor != null) "AND tutor_id = :p_t AND date_time >= :from" else ""}
            ORDER BY date_time DESC LIMIT $limit
            """,
            mapOf("ids" to ids.toTypedArray(), "p_t" to tutor, "from" to YearMonth.now().start()),
        )
    ) { it.toPayment() }

    fun updatePayment(id: Int, fields: Map<String, Any?>) {
        if (fields.isEmpty()) return
        val set = fields.keys.joinToString(", ") { "$it = :$it" }
        update(queryOf("UPDATE payment SET $set WHERE id = :id", fields + ("id" to id)))
    }

    // ---------- students ----------

    private fun Row.toStudentRow(withBalance: Boolean) = StudentRowDto(
        id = int("id"),
        last = stringOrNull("second_name") ?: "",
        first = stringOrNull("name") ?: "",
        middle = stringOrNull("last_name") ?: "",
        cls = intOrNull("school_class"),
        status = stringOrNull("status") ?: "active",
        reason = stringOrNull("status_reason"),
        since = localDateOrNull("status_since")?.toString(),
        deleted = boolean("deleted"),
        monthCount = intOrNull("month_count") ?: 0,
        subjects = arrayOrNull<Long>("subjects")?.toList()?.filterNotNull().orEmpty(),
        lastDate = localDateTimeOrNull("last_dt")?.toLocalDate()?.toString(),
        balance = if (withBalance) ((longOrNull("paid") ?: 0L) - (doubleOrNull("billed") ?: 0.0)).toLong() else null,
        source = stringOrNull("source"),
    )

    private fun studentRowsQuery(where: String) = """
        WITH l AS (
            SELECT student_id,
                   count(DISTINCT id) FILTER (WHERE date_time >= :ms AND date_time < :me) AS month_count,
                   max(date_time) AS last_dt,
                   array_agg(DISTINCT CAST(subject_id AS bigint)) AS subjects,
                   sum($PRICE_SQL) AS billed
            FROM lessons WHERE NOT coalesce(is_deleted, false) GROUP BY student_id
        ), p AS (
            SELECT student_id, sum(amount) AS paid FROM payment WHERE NOT coalesce(is_deleted, false) GROUP BY student_id
        )
        SELECT s.id, s.second_name, s.name, s.last_name, s.school_class, s.status, s.status_reason, s.status_since,
               s.source, coalesce(s.deleted, false) AS deleted,
               l.month_count, l.last_dt, l.subjects, l.billed, p.paid
        FROM students s LEFT JOIN l ON l.student_id = s.id LEFT JOIN p ON p.student_id = s.id
        $where
        ORDER BY s.second_name, s.name
    """

    fun studentRows(includeDeleted: Boolean, withBalance: Boolean): List<StudentRowDto> {
        val m = YearMonth.now()
        return runList(
            queryOf(
                studentRowsQuery(if (includeDeleted) "" else "WHERE NOT coalesce(s.deleted, false)"),
                mapOf("ms" to m.start(), "me" to m.end()),
            )
        ) { it.toStudentRow(withBalance) }
    }

    fun studentRow(id: Int, withBalance: Boolean): StudentRowDto? {
        val m = YearMonth.now()
        return runSingle(
            queryOf(studentRowsQuery("WHERE s.id = :id"), mapOf("ms" to m.start(), "me" to m.end(), "id" to id))
        ) { it.toStudentRow(withBalance) }
    }

    /** Students are written here rather than through StudentMapper so free-text sources are kept as typed. */
    fun insertStudent(input: StudentInput, phone: String?, parent: String?, by: Long): Int = runSingle(
        queryOf(
            """
            INSERT INTO students (name, second_name, last_name, school_class, record_date, source, phone, responsible_phone,
                                  created_by, update_by, create_date, modify_date, deleted)
            VALUES (:first, :last, :middle, :cls, CURRENT_DATE, :src, :phone, :parent, :by, :by, now(), now(), false)
            RETURNING id
            """,
            mapOf("first" to input.first, "last" to input.last, "middle" to input.middle, "cls" to input.cls,
                "src" to input.source?.trim()?.takeIf { it.isNotEmpty() }, "phone" to phone, "parent" to parent, "by" to by),
        )
    ) { it.int("id") }!!

    fun updateStudent(id: Int, input: StudentInput, phone: String?, parent: String?, by: Long) = update(
        queryOf(
            """
            UPDATE students SET name = :first, second_name = :last, last_name = :middle, school_class = :cls, source = :src,
                   phone = :phone, responsible_phone = :parent, update_by = :by, modify_date = now()
            WHERE id = :id
            """,
            mapOf("id" to id, "first" to input.first, "last" to input.last, "middle" to input.middle, "cls" to input.cls,
                "src" to input.source?.trim()?.takeIf { it.isNotEmpty() }, "phone" to phone, "parent" to parent, "by" to by),
        )
    )

    fun setStudentStatus(id: Int, status: String, reason: String?, by: Long) = update(
        queryOf(
            """
            UPDATE students SET status = :st, status_reason = :p_r, status_since = :since, modify_date = now(), update_by = :by
            WHERE id = :id
            """,
            mapOf("id" to id, "st" to status, "p_r" to reason, "since" to LocalDate.now().withDayOfMonth(1), "by" to by),
        )
    )

    fun setStudentDeleted(id: Int, deleted: Boolean, by: Long) = update(
        queryOf(
            "UPDATE students SET deleted = :p_d, modify_date = now(), update_by = :by WHERE id = :id",
            mapOf("id" to id, "p_d" to deleted, "by" to by),
        )
    )

    fun notes(student: Int): List<NoteDto> = runList(
        queryOf(
            """
            SELECT n.text, n.created_at, coalesce(t.name, CAST(n.tutor_id AS text)) AS by
            FROM student_notes n LEFT JOIN tutors t ON t.id = n.tutor_id
            WHERE n.student_id = :p_s ORDER BY n.created_at DESC
            """,
            mapOf("p_s" to student),
        )
    ) { NoteDto(it.localDateTime("created_at").toString(), it.string("by"), it.string("text")) }

    fun addNote(student: Int, tutor: Long, text: String) = update(
        queryOf("INSERT INTO student_notes (student_id, tutor_id, text) VALUES (:p_s, :p_t, :p_x)", mapOf("p_s" to student, "p_t" to tutor, "p_x" to text))
    )

    fun studentLessonStats(student: Int, tutor: Long?): Pair<Int, List<RecentLessonDto>> {
        val rows = runList(
            queryOf(
                """
                SELECT l.id, min(l.date_time) AS dt, max(l.subject_id) AS subj,
                       (SELECT count(*) FROM lessons x WHERE x.id = l.id AND NOT coalesce(x.is_deleted, false)) AS n,
                       bool_or(l.force_group) AS fg
                FROM lessons l
                WHERE l.student_id = :p_s AND NOT coalesce(l.is_deleted, false) ${if (tutor != null) "AND l.tutor_id = :p_t" else ""}
                GROUP BY l.id ORDER BY dt DESC
                """,
                mapOf("p_s" to student, "p_t" to tutor),
            )
        ) { RecentLessonDto(it.localDateTime("dt").toLocalDate().toString(), it.long("subj"), it.int("n") > 1 || it.boolean("fg")) }
        return rows.size to rows.take(3)
    }

    fun paidTotal(student: Int): Long = runSingle(
        queryOf("SELECT coalesce(sum(amount), 0) AS s FROM payment WHERE student_id = :p_s AND NOT coalesce(is_deleted, false)", mapOf("p_s" to student))
    ) { it.long("s") } ?: 0

    // ---------- schedule ----------

    private fun Row.toSlot(names: Map<Int, StudentName>) = SlotDto(
        id = int("id"),
        weekday = intOrNull("weekday"),
        onceDate = localDateOrNull("once_date")?.toString(),
        time = localTime("time").format(HM),
        subject = long("subject_id"),
        students = array<Int>("student_ids").map { names[it]?.ref() ?: StudentRefDto(it, "#$it") },
        amount = int("amount"),
        tutor = long("tutor_id"),
    )

    fun slots(tutor: Long?, names: Map<Int, StudentName>, includeDeleted: Boolean = false): List<SlotDto> = runList(
        queryOf(
            """
            SELECT * FROM schedule_slots
            WHERE ${if (includeDeleted) "true" else "NOT is_deleted"} ${if (tutor != null) "AND tutor_id = :p_t" else ""}
              AND (once_date IS NULL OR once_date >= :from)
            ORDER BY time
            """,
            mapOf("p_t" to tutor, "from" to LocalDate.now().minusDays(62)),
        )
    ) { it.toSlot(names) }

    fun slot(id: Int, names: Map<Int, StudentName>): SlotDto? =
        runSingle(queryOf("SELECT * FROM schedule_slots WHERE id = :id", mapOf("id" to id))) { it.toSlot(names) }

    fun slotDeleted(id: Int): Boolean = runSingle(
        queryOf("SELECT is_deleted FROM schedule_slots WHERE id = :id", mapOf("id" to id))
    ) { it.boolean("is_deleted") } ?: true

    fun addSlot(tutor: Long, input: SlotInput): Int = runSingle(
        queryOf(
            """
            INSERT INTO schedule_slots (tutor_id, subject_id, student_ids, weekday, once_date, time, amount)
            VALUES (:p_t, :p_s, :st, :wd, :p_d, CAST(:time AS time), :p_a) RETURNING id
            """,
            mapOf(
                "p_t" to tutor, "p_s" to input.subject, "st" to input.students.toTypedArray(), "wd" to input.weekday,
                "p_d" to input.date?.let { LocalDate.parse(it) }, "time" to input.time, "p_a" to input.amount,
            ),
        )
    ) { it.int("id") }!!

    fun setSlotDeleted(id: Int, deleted: Boolean) =
        update(queryOf("UPDATE schedule_slots SET is_deleted = :p_d WHERE id = :id", mapOf("id" to id, "p_d" to deleted)))

    fun marks(from: LocalDate, to: LocalDate, slotIds: Collection<Int>): List<SlotMarkDto> = if (slotIds.isEmpty()) emptyList() else runList(
        queryOf(
            "SELECT * FROM schedule_marks WHERE date >= :p_f AND date <= :p_t AND slot_id = ANY(:ids)",
            mapOf("p_f" to from, "p_t" to to, "ids" to slotIds.toTypedArray()),
        )
    ) { SlotMarkDto(it.int("slot_id"), it.localDate("date").toString(), it.string("status"), it.uuidOrNull("lesson_id")?.toString()) }

    fun setMark(slot: Int, date: LocalDate, status: String, lesson: UUID?) = update(
        queryOf(
            """
            INSERT INTO schedule_marks (slot_id, date, status, lesson_id) VALUES (:p_s, :p_d, :st, :p_l)
            ON CONFLICT (slot_id, date) DO UPDATE SET status = excluded.status, lesson_id = excluded.lesson_id
            """,
            mapOf("p_s" to slot, "p_d" to date, "st" to status, "p_l" to lesson),
        )
    )

    fun clearMark(slot: Int, date: LocalDate) =
        update(queryOf("DELETE FROM schedule_marks WHERE slot_id = :p_s AND date = :p_d", mapOf("p_s" to slot, "p_d" to date)))

    /** Slots that fall on [date]: weekly ones on its weekday, plus one-offs on that date. */
    fun slotsOn(date: LocalDate, all: List<SlotDto>): List<SlotDto> {
        val wd = date.dayOfWeek.value - 1
        val iso = date.toString()
        return all.filter { if (it.onceDate != null) it.onceDate == iso else it.weekday == wd }
    }

    fun reminders(tutor: Long): RemindersDto = runSingle(
        queryOf("SELECT * FROM reminder_settings WHERE tutor_id = :p_t", mapOf("p_t" to tutor))
    ) { RemindersDto(it.boolean("evening"), it.string("at"), it.boolean("month_end")) } ?: RemindersDto(true, "21:00", true)

    fun saveReminders(tutor: Long, r: RemindersDto) = update(
        queryOf(
            """
            INSERT INTO reminder_settings (tutor_id, evening, at, month_end) VALUES (:p_t, :p_e, :p_a, :p_m)
            ON CONFLICT (tutor_id) DO UPDATE SET evening = excluded.evening, at = excluded.at, month_end = excluded.month_end
            """,
            mapOf("p_t" to tutor, "p_e" to r.evening, "p_a" to r.at, "p_m" to r.monthEnd),
        )
    )

    // ---------- report submissions ----------

    private fun Row.toSubmission() = SubmissionDto(
        long("tutor_id"), string("month"), string("status"), stringOrNull("note"), stringOrNull("comment"),
        localDateTimeOrNull("submitted_at")?.toString(), localDateTimeOrNull("decided_at")?.toString(), boolean("self_accepted"),
    )

    fun submission(tutor: Long, month: String): SubmissionDto? = runSingle(
        queryOf("SELECT * FROM report_submissions WHERE tutor_id = :p_t AND month = :p_m", mapOf("p_t" to tutor, "p_m" to month))
    ) { it.toSubmission() }

    fun submissions(month: String): List<SubmissionDto> = runList(
        queryOf("SELECT * FROM report_submissions WHERE month = :p_m", mapOf("p_m" to month))
    ) { it.toSubmission() }

    fun saveSubmission(s: SubmissionDto, decidedBy: Long?) = update(
        queryOf(
            """
            INSERT INTO report_submissions (tutor_id, month, status, note, comment, submitted_at, decided_at, decided_by, self_accepted)
            VALUES (:p_t, :p_m, :st, :note, :p_c, CAST(:sa AS timestamp), CAST(:da AS timestamp), :by, :self)
            ON CONFLICT (tutor_id, month) DO UPDATE SET status = excluded.status, note = excluded.note, comment = excluded.comment,
                submitted_at = excluded.submitted_at, decided_at = excluded.decided_at, decided_by = excluded.decided_by,
                self_accepted = excluded.self_accepted
            """,
            mapOf(
                "p_t" to s.tutor, "p_m" to s.month, "st" to s.status, "note" to s.note, "p_c" to s.comment,
                "sa" to s.submittedAt, "da" to s.decidedAt, "by" to decidedBy, "self" to s.selfAccepted,
            ),
        )
    )

    fun monthTotals(tutor: Long, month: YearMonth): MonthTotalsDto {
        val ls = lessons(month.start(), month.end(), tutor).filterNot { it.deleted }
        val received = runSingle(
            queryOf(
                "SELECT coalesce(sum(amount), 0) AS s FROM payment WHERE tutor_id = :p_t AND date_time >= :p_f AND date_time < :p_e AND NOT coalesce(is_deleted, false)",
                mapOf("p_t" to tutor, "p_f" to month.start(), "p_e" to month.end()),
            )
        ) { it.long("s") } ?: 0
        return MonthTotalsDto(ls.size, ls.sumOf { it.price * it.students.size }.toLong(), received, ls.map { it.subject }.distinct())
    }

    // ---------- settings ----------

    fun setting(key: String): String? =
        runSingle(queryOf("SELECT value FROM app_settings WHERE key = :p_k", mapOf("p_k" to key))) { it.string("value") }

    fun saveSetting(key: String, value: String) = update(
        queryOf(
            "INSERT INTO app_settings (key, value) VALUES (:p_k, :p_v) ON CONFLICT (key) DO UPDATE SET value = excluded.value",
            mapOf("p_k" to key, "p_v" to value),
        )
    )

    // ---------- tutors ----------

    fun insertTutor(input: TutorInput) = update(
        queryOf(
            "INSERT INTO tutors (id, name, permissions, subjects, rate) VALUES (:id, :p_n, CAST(:p_p AS text[]), :p_s, :p_r)",
            mapOf(
                "id" to input.id, "p_n" to input.name, "p_p" to input.perms.toTypedArray(),
                "p_s" to input.subjects.joinToString(":"), "p_r" to input.rate,
            ),
        )
    )

    fun updateTutor(input: TutorInput) = update(
        queryOf(
            "UPDATE tutors SET name = :p_n, permissions = CAST(:p_p AS text[]), subjects = :p_s, rate = :p_r WHERE id = :id",
            mapOf(
                "id" to input.id, "p_n" to input.name, "p_p" to input.perms.toTypedArray(),
                "p_s" to input.subjects.joinToString(":"), "p_r" to input.rate,
            ),
        )
    )

}
