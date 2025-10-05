package me.centralhardware.znatoki.telegram.statistic.mapper

import java.time.LocalDateTime
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.PaymentId
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.parsePayment
import me.centralhardware.znatoki.telegram.statistic.extensions.endOfMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.prevMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle
import me.centralhardware.znatoki.telegram.statistic.extensions.update
import me.centralhardware.znatoki.telegram.statistic.extensions.startOfMonth
import java.time.YearMonth

object PaymentMapper {

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
                    added_by_tutor_id
                ) VALUES (
                    :dateTime,
                    :tutorId,
                    :studentId,
                    :amount,
                    :subjectId,
                    :photo_report,
                    :addedByTutorId
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
                ),
            )
        ) { row -> PaymentId(row.int("id")) }!!

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
                    AND date_time between :startDate and :endDate
                    AND photo_report IS NOT NULL
                    AND is_deleted = false
                """,
                mapOf(
                    "tutor_id" to tutorId.id,
                    "subject_id" to subjectId.id,
                    "student_id" to studentId.id,
                    "startDate" to date.startOfMonth(),
                    "endDate" to date.endOfMonth(),
                ),
            )
        ) { row -> row.longOrNull("sum") }?: 0

    fun getPaymentsSum(tutorId: TutorId, subjectId: SubjectId, date: LocalDateTime): Long =
        runSingle(
            queryOf(
                """
                SELECT sum(amount) as sum
                FROM payment
                WHERE tutor_id = :tutor_id
                    AND subject_id = :subject_id
                    AND date_time between :startDate and :endDate
                    AND is_deleted = false
                """,
                mapOf(
                    "tutor_id" to tutorId.id,
                    "subject_id" to subjectId.id,
                    "startDate" to date.startOfMonth(),
                    "endDate" to date.endOfMonth(),
                ),
            )
        ) { row -> row.longOrNull("sum") }?: 0

    private fun getPayments(
        tutorId: TutorId,
        subjectId: SubjectId,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ) = runList(
        queryOf(
            """
            SELECT p.id,
                   p.date_time,
                   p.tutor_id,
                   p.student_id,
                   p.amount,
                   p.subject_id,
                   p.is_deleted,
                   p.photo_report,
                   p.added_by_tutor_id
            FROM payment p
            WHERE p.tutor_id = :tutor_id
                AND p.subject_id = :subject_id
                AND p.date_time between :start_date and :end_date
                AND p.is_deleted=false
            """, mapOf(
                "tutor_id" to tutorId.id,
                "subject_id" to subjectId.id,
                "start_date" to startDate,
                "end_date" to endDate)
    )) { it.parsePayment() }

    fun getCurrentMonthPayments(tutorId: TutorId, subjectId: SubjectId) = getPayments(
        tutorId,
        subjectId,
        LocalDateTime.now().startOfMonth(),
        LocalDateTime.now().endOfMonth(),
    )

    fun getPrevMonthPayments(tutorId: TutorId, subjectId: SubjectId) = getPayments(
        tutorId,
        subjectId,
        LocalDateTime.now().prevMonth().startOfMonth(),
        LocalDateTime.now().prevMonth().endOfMonth(),
    )

    fun getPaymentsByMonth(tutorId: TutorId, subjectId: SubjectId, yearMonth: YearMonth) = getPayments(
        tutorId,
        subjectId,
        yearMonth.atDay(1).atStartOfDay(),
        yearMonth.atEndOfMonth().atTime(23, 59, 59),
    )

    fun getPaymentsByDateRange(tutorId: TutorId, subjectId: SubjectId, startDate: LocalDateTime, endDate: LocalDateTime) = getPayments(
        tutorId,
        subjectId,
        startDate,
        endDate,
    )

    fun getPaymentsSumByDateRange(tutorId: TutorId, subjectId: SubjectId, startDate: LocalDateTime, endDate: LocalDateTime): Long =
        runSingle(
            queryOf(
                """
                SELECT sum(amount) as sum
                FROM payment
                WHERE tutor_id = :tutor_id
                    AND subject_id = :subject_id
                    AND date_time between :startDate and :endDate
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

}
