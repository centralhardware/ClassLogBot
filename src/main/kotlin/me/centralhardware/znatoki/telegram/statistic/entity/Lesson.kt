package me.centralhardware.znatoki.telegram.statistic.entity

import java.time.LocalDateTime
import java.util.*
import kotliquery.Row

@JvmInline
value class LessonId(val id: UUID) {

    companion object {
        fun random() = LessonId(UUID.randomUUID())
    }

}

fun String.toLessonId() = LessonId(UUID.fromString(this))

class Lesson(
    val id: LessonId,
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val updateTime: LocalDateTime = LocalDateTime.now(),
    val tutorId: TutorId,
    val subjectId: SubjectId,
    val studentId: StudentId,
    private val _amount: Amount,
    val forceGroup: Boolean = false,
    val extraHalfHour: Boolean = false,
    val photoReport: String? = null,
    val deleted: Boolean = false,
) {
    val amount: Double
        get() = if (extraHalfHour) _amount.amount * 1.5 else _amount.amount * 1.0
}

fun Row.parseTime() =
    Lesson(
        LessonId(uuid("id")),
        localDateTime("date_time"),
        localDateTime("update_time"),
        TutorId(long("chat_id")),
        long("service_id").toSubjectId(),
        int("pupil_id").toStudentId(),
        int("amount").toAmount(),
        boolean("force_group"),
        boolean("extra_half_hour"),
        stringOrNull("photo_report"),
        boolean("is_deleted"),
    )

class ServiceBuilder {
    var id: LessonId = LessonId.random()
    var tutorId: TutorId? = null
    var subjectId: SubjectId? = null
    var amount: Amount? = null
    var studentIds: Set<StudentId> = mutableSetOf()
    var photoReport: String? = null

    fun build(): List<Lesson> =
        studentIds.map {
            Lesson(
                id = id,
                tutorId = tutorId!!,
                subjectId = subjectId!!,
                studentId = it,
                _amount = amount!!,
                photoReport = photoReport,
            )
        }
}

fun Collection<Lesson>.toStudentIds(): List<StudentId> = this.map { it.studentId }.toList()

fun Collection<Lesson>.isIndividual() = size == 1 && !first().forceGroup

fun Collection<Lesson>.isGroup() = size > 1 || (size == 1 && first().forceGroup)
