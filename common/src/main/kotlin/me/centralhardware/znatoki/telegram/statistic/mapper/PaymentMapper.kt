package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.Session
import java.time.LocalDateTime
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.PaymentId
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.parsePayment
import me.centralhardware.znatoki.telegram.statistic.extensions.startOfNextMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.prevMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle
import me.centralhardware.znatoki.telegram.statistic.extensions.update
import me.centralhardware.znatoki.telegram.statistic.extensions.startOfMonth
import java.time.YearMonth

private const val PAYMENT_COLUMNS =
    "id, date_time, tutor_id, student_id, amount, subject_id, is_deleted, photo_report, added_by_tutor_id, data_source"

object PaymentMapper {

    context(_: Session)
    fun insert(payment: Payment): PaymentId =
        runSingle(
            queryOf(
                """
                INSERT INTO payment(
                    date_time,
                    tutor_id,
                    student_id,
                    amount,
                    subject_id,
                    photo_report,
                    added_by_tutor_id,
                    data_source
                ) VALUES (
                    :dateTime,
                    :tutorId,
                    :studentId,
                    :amount,
                    :subjectId,
                    :photo_report,
                    :addedByTutorId,
                    :dataSource
                ) RETURNING id
                """,
                mapOf(
                    "dateTime" to payment.dateTime,
                    "tutorId" to payment.tutorId.id,
                    "studentId" to payment.studentId.id,
                    "amount" to payment.amount.amount,
                    "subjectId" to payment.subjectId.id,
                    "photo_report" to payment.photoReport,
                    "addedByTutorId" to payment.addedByTutorId?.id,
                    "dataSource" to payment.dataSource?.value,
                ),
            )
        ) { row -> PaymentId(row.int("id")) } ?: error("INSERT INTO payment returned no id")

    context(_: Session)
    fun setDelete(id: PaymentId, isDelete: Boolean) =
        update(
            queryOf(
                """
                UPDATE payment
                SET is_deleted = :is_delete
                WHERE id = :id
                """,
                mapOf("id" to id.id, "is_delete" to isDelete),
            )
        )

    context(_: Session)
    fun getPaymentsSumForStudent(
        tutorId: TutorId,
        subjectId: SubjectId,
        studentId: StudentId,
        date: LocalDateTime,
    ): Long =
        runSingle(
            queryOf(
                """
                SELECT sum(amount) as sum
                FROM payment
                WHERE tutor_id = :tutor_id
                    AND subject_id = :subject_id
                    AND student_id = :student_id
                    AND date_time >= :startDate AND date_time < :endDate
                    AND photo_report IS NOT NULL
                    AND is_deleted = false
                """,
                mapOf(
                    "tutor_id" to tutorId.id,
                    "subject_id" to subjectId.id,
                    "student_id" to studentId.id,
                    "startDate" to date.startOfMonth(),
                    "endDate" to date.startOfNextMonth(),
                ),
            )
        ) { row -> row.longOrNull("sum") }?: 0

    context(_: Session)
    fun getPaymentsSum(tutorId: TutorId, subjectId: SubjectId, date: LocalDateTime): Long =
        runSingle(
            queryOf(
                """
                SELECT sum(amount) as sum
                FROM payment
                WHERE tutor_id = :tutor_id
                    AND subject_id = :subject_id
                    AND date_time >= :startDate AND date_time < :endDate
                    AND is_deleted = false
                """,
                mapOf(
                    "tutor_id" to tutorId.id,
                    "subject_id" to subjectId.id,
                    "startDate" to date.startOfMonth(),
                    "endDate" to date.startOfNextMonth(),
                ),
            )
        ) { row -> row.longOrNull("sum") }?: 0

    context(_: Session)
    private fun getPayments(
        tutorId: TutorId,
        subjectId: SubjectId,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ) = runList(
        queryOf(
            """
            SELECT $PAYMENT_COLUMNS
            FROM payment p
            WHERE p.tutor_id = :tutor_id
                AND p.subject_id = :subject_id
                AND p.date_time >= :start_date AND p.date_time < :end_date
                AND p.is_deleted=false
            """, mapOf(
                "tutor_id" to tutorId.id,
                "subject_id" to subjectId.id,
                "start_date" to startDate,
                "end_date" to endDate)
    )) { it.parsePayment() }

    context(_: Session)
    fun getCurrentMonthPayments(tutorId: TutorId, subjectId: SubjectId) = getPayments(
        tutorId,
        subjectId,
        LocalDateTime.now().startOfMonth(),
        LocalDateTime.now().startOfNextMonth(),
    )

    context(_: Session)
    fun getPrevMonthPayments(tutorId: TutorId, subjectId: SubjectId) = getPayments(
        tutorId,
        subjectId,
        LocalDateTime.now().prevMonth().startOfMonth(),
        LocalDateTime.now().prevMonth().startOfNextMonth(),
    )

    context(_: Session)
    fun getPaymentsByMonth(tutorId: TutorId, subjectId: SubjectId, yearMonth: YearMonth) = getPayments(
        tutorId,
        subjectId,
        yearMonth.atDay(1).atStartOfDay(),
        yearMonth.plusMonths(1).atDay(1).atStartOfDay(),
    )

    /** [startDate, endDate) — endDate is exclusive. */
    context(_: Session)
    fun getPaymentsByDateRange(tutorId: TutorId, subjectId: SubjectId, startDate: LocalDateTime, endDate: LocalDateTime) = getPayments(
        tutorId,
        subjectId,
        startDate,
        endDate,
    )

    /** [startDate, endDate) — endDate is exclusive. */
    context(_: Session)
    fun getPaymentsSumByDateRange(tutorId: TutorId, subjectId: SubjectId, startDate: LocalDateTime, endDate: LocalDateTime): Long =
        runSingle(
            queryOf(
                """
                SELECT sum(amount) as sum
                FROM payment
                WHERE tutor_id = :tutor_id
                    AND subject_id = :subject_id
                    AND date_time >= :startDate AND date_time < :endDate
                    AND is_deleted = false
                """,
                mapOf(
                    "tutor_id" to tutorId.id,
                    "subject_id" to subjectId.id,
                    "startDate" to startDate,
                    "endDate" to endDate,
                ),
            )
        ) { row -> row.longOrNull("sum") }?: 0

    context(_: Session)
    private fun getPaymentsByTutorId(
        tutorId: TutorId,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ) = runList(
        queryOf(
            """
            SELECT $PAYMENT_COLUMNS
            FROM payment p
            WHERE p.tutor_id = :tutor_id
                AND p.date_time >= :start_date AND p.date_time < :end_date
                AND p.is_deleted=false
            """, mapOf(
                "tutor_id" to tutorId.id,
                "start_date" to startDate,
                "end_date" to endDate)
    )) { it.parsePayment() }

    context(_: Session)
    fun getTodayPayments(tutorId: TutorId): List<Payment> {
        val now = LocalDateTime.now()
        val startOfDay = now.toLocalDate().atStartOfDay()
        return getPaymentsByTutorId(tutorId, startOfDay, now)
    }

    context(_: Session)
    fun findById(id: PaymentId): Payment? = runSingle(
        queryOf(
            """
            SELECT $PAYMENT_COLUMNS
            FROM payment p
            WHERE p.id = :id
                AND p.is_deleted=false
            """, mapOf("id" to id.id)
        )
    ) { it.parsePayment() }

    context(_: Session)
    fun markFireflyExported(id: PaymentId) =
        update(queryOf("UPDATE payment SET firefly_exported_at = now() WHERE id = :id", mapOf("id" to id.id)))

    /** Live payments whose Firefly export has not succeeded yet, oldest first. */
    context(_: Session)
    fun findFireflyPending(limit: Int = 50): List<Payment> = runList(
        queryOf(
            """
            SELECT $PAYMENT_COLUMNS
            FROM payment p
            WHERE p.firefly_exported_at IS NULL
                AND p.is_deleted=false
            ORDER BY p.id
            LIMIT :limit
            """, mapOf("limit" to limit)
        )
    ) { it.parsePayment() }

    context(_: Session)
    fun findByIdIncludingDeleted(id: PaymentId): Payment? = runSingle(
        queryOf(
            """
            SELECT $PAYMENT_COLUMNS
            FROM payment p
            WHERE p.id = :id
            """, mapOf("id" to id.id)
        )
    ) { it.parsePayment() }

    context(_: Session)
    fun updateAmount(id: PaymentId, amount: me.centralhardware.znatoki.telegram.statistic.entity.Amount) =
        update(
            queryOf(
                """
                UPDATE payment
                SET amount = :amount
                WHERE id = :id
                """,
                mapOf("id" to id.id, "amount" to amount.amount),
            )
        )

    context(_: Session)
    fun setSubjectId(id: PaymentId, subjectId: SubjectId) =
        update(
            queryOf(
                """
                UPDATE payment
                SET subject_id = :subject_id
                WHERE id = :id
                """,
                mapOf("id" to id.id, "subject_id" to subjectId.id),
            )
        )

    context(_: Session)
    fun findAllByStudent(studentId: StudentId): List<Payment> =
        runList(
            queryOf(
                """
                SELECT $PAYMENT_COLUMNS
                FROM payment p
                WHERE p.student_id = :studentId AND p.is_deleted = false
                ORDER BY p.date_time DESC
                """,
                mapOf("studentId" to studentId.id)
            )
        ) { it.parsePayment() }

}
