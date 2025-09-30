package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row
import java.time.LocalDateTime

@JvmInline
value class PaymentId(val id: Int)

data class Payment(
    val id: PaymentId? = null,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val tutorId: TutorId,
    val studentId: StudentId,
    val amount: Amount,
    val subjectId: SubjectId,
    val deleted: Boolean = false,
    val photoReport: String? = null,
)

fun Row.parsePayment() = Payment(
    PaymentId(int("id")),
    localDateTime("date_time"),
    TutorId(long("tutor_id")),
    int("student_id").toStudentId(),
    int("amount").toAmount(),
    long("subject_id").toSubjectId(),
    boolean("is_deleted"),
    stringOrNull("photo_report")
)

class PaymentBuilder {
    var tutorId: TutorId? = null
    var studentId: StudentId? = null
    var amount: Amount? = null
    var subjectId: SubjectId? = null
    var photoReport: String? = null

    fun build(): Payment =
        Payment(
            tutorId = tutorId!!,
            studentId = studentId!!,
            amount = amount!!,
            subjectId = subjectId!!,
            photoReport = photoReport,
        )
}
