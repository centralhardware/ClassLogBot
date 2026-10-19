package me.centralhardware.znatoki.telegram.statistic

import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.extensions.db
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

/** Month filters against a real Postgres: boundaries are [1st 00:00, next 1st 00:00), deleted rows excluded. */
class MonthRangeMapperTest {

    private val tutor = TutorId(1001)
    private val subject = SubjectId(1L)
    private val october = YearMonth.of(2026, 10)

    private val inside = listOf(
        LocalDateTime.of(2026, 10, 1, 0, 0),
        LocalDateTime.of(2026, 10, 15, 12, 30),
        LocalDateTime.of(2026, 10, 31, 23, 59, 59, 500_000_000),
    )
    private val outside = listOf(
        LocalDateTime.of(2026, 9, 30, 23, 59, 59, 900_000_000),
        LocalDateTime.of(2026, 11, 1, 0, 0),
    )

    @BeforeEach
    fun setUp() {
        assumeTrue(TestDatabase.dockerAvailable, "Docker is not available")
        TestDatabase.start()
        TestDatabase.exec("TRUNCATE lessons, payment, students, tutors, subjects CASCADE")
        TestDatabase.exec("INSERT INTO subjects (id, name, allow_multiply_clients) VALUES (1, 'Math', false)")
        TestDatabase.exec("INSERT INTO tutors (id, subjects, name, permissions) VALUES (1001, '1', 'Tutor', '{ADMIN}')")
        TestDatabase.exec("INSERT INTO students (id, name, last_name, second_name, deleted) VALUES (1, 'Ivan', 'Ivanov', '', false)")
    }

    private fun lesson(at: LocalDateTime, amount: Int = 1000, deleted: Boolean = false) =
        TestDatabase.exec(
            """
            INSERT INTO lessons (id, tutor_id, amount, student_id, subject_id, date_time, is_deleted)
            VALUES (:id, 1001, :amount, 1, 1, :at, :deleted)
            """,
            mapOf("id" to UUID.randomUUID(), "amount" to amount, "at" to at, "deleted" to deleted),
        )

    private fun payment(at: LocalDateTime, amount: Int = 1000, deleted: Boolean = false) =
        TestDatabase.exec(
            """
            INSERT INTO payment (tutor_id, student_id, amount, subject_id, date_time, is_deleted, photo_report)
            VALUES (1001, 1, :amount, 1, :at, :deleted, 'photo.jpg')
            """,
            mapOf("amount" to amount, "at" to at, "deleted" to deleted),
        )

    @Test
    fun `lessons by month include both ends of the month and nothing outside`() {
        (inside + outside).forEach { lesson(it) }

        val found = db { LessonMapper.getTimesByMonth(tutor, subject, october).map { it.dateTime }.sorted() }

        assertEquals(inside, found)
    }

    @Test
    fun `deleted lessons are excluded`() {
        lesson(LocalDateTime.of(2026, 10, 10, 10, 0))
        lesson(LocalDateTime.of(2026, 10, 11, 10, 0), deleted = true)

        assertEquals(1, db { LessonMapper.getTimesByMonth(tutor, subject, october).size })
    }

    @Test
    fun `payments by month and their sum use the same boundaries`() {
        inside.forEach { payment(it, amount = 100) }
        outside.forEach { payment(it, amount = 10_000) }
        payment(LocalDateTime.of(2026, 10, 20, 9, 0), amount = 5_000, deleted = true)

        assertEquals(3, db { PaymentMapper.getPaymentsByMonth(tutor, subject, october).size })
        assertEquals(
            300,
            db {
                PaymentMapper.getPaymentsSumByDateRange(
                    tutor, subject, october.atDay(1).atStartOfDay(), october.plusMonths(1).atDay(1).atStartOfDay(),
                )
            },
        )
    }
}
