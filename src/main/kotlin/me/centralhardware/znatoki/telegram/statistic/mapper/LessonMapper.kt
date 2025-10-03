package me.centralhardware.znatoki.telegram.statistic.mapper

import java.time.LocalDateTime
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.Lesson
import me.centralhardware.znatoki.telegram.statistic.entity.LessonId
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.parseTime
import me.centralhardware.znatoki.telegram.statistic.entity.toSubjectId
import me.centralhardware.znatoki.telegram.statistic.extensions.endOfMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.prevMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.execute
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.update
import me.centralhardware.znatoki.telegram.statistic.extensions.startOfDay
import me.centralhardware.znatoki.telegram.statistic.extensions.startOfMonth

object LessonMapper {

    fun insert(lesson: Lesson) =
        execute(
            queryOf(
                """
                INSERT INTO lessons (
                    date_time,
                    update_time,
                    id,
                    tutor_id,
                    subject_id,
                    amount,
                    student_id,
                    photo_report,
                    added_by_tutor_id
                ) VALUES (
                    :dateTime,
                    :updateTime,
                    :id,
                    :tutorId,
                    :subjectId,
                    :amount,
                    :studentId,
                    :photo_report,
                    :addedByTutorId
                )
                """,
                mapOf(
                    "dateTime" to lesson.dateTime,
                    "updateTime" to lesson.updateTime,
                    "id" to lesson.id.id,
                    "tutorId" to lesson.tutorId.id,
                    "subjectId" to lesson.subjectId.id,
                    "amount" to lesson.amount,
                    "studentId" to lesson.studentId.id,
                    "photo_report" to lesson.photoReport,
                    "addedByTutorId" to lesson.addedByTutorId?.id,
                ),
            )
        )

    private fun findAllByTutorId(
        tutorId: TutorId,
        subjectId: SubjectId,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<Lesson> =
        runList(
            queryOf(
                """
                SELECT l.id,
                       l.date_time,
                       l.update_time,
                       l.tutor_id,
                       l.subject_id,
                       l.student_id,
                       l.amount,
                       l.force_group,
                       l.extra_half_hour,
                       l.photo_report,
                       l.is_deleted,
                       l.added_by_tutor_id
                FROM lessons l
                WHERE l.tutor_id = :tutorId
                    AND l.subject_id = :subjectId
                    AND l.date_time between :startDate and :endDate
                    AND l.is_deleted=false
                """,
                    mapOf("tutorId" to tutorId.id,"subjectId" to subjectId.id, "startDate" to startDate, "endDate" to endDate),
                )
        ) { it.parseTime() }

    private fun findAllByTutorId(
        tutorId: TutorId,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<Lesson> =
        runList(
            queryOf(
                """
                SELECT l.id,
                       l.date_time,
                       l.update_time,
                       l.tutor_id,
                       l.subject_id,
                       l.student_id,
                       l.amount,
                       l.force_group,
                       l.extra_half_hour,
                       l.photo_report,
                       l.is_deleted,
                       l.added_by_tutor_id
                FROM lessons l
                WHERE l.tutor_id = :tutorId
                    AND l.date_time between :startDate and :endDate
                    AND l.is_deleted=false
                """,
                mapOf("tutorId" to tutorId.id, "startDate" to startDate, "endDate" to endDate),
            )
        ) { it.parseTime() }

    fun findById(id: LessonId): List<Lesson> =
        runList(
            queryOf(
                """
                SELECT id,
                       date_time,
                       update_time,
                       tutor_id,
                       subject_id,
                       student_id,
                       amount,
                       photo_report,
                       force_group,
                       extra_half_hour,
                       is_deleted,
                       added_by_tutor_id
                FROM lessons
                WHERE id = :id
                """,
                    mapOf("id" to id.id),
                )
        ) { it.parseTime() }

    fun getTodayTimes(tutorId: TutorId): List<Lesson> =
        findAllByTutorId(tutorId, LocalDateTime.now().startOfDay(), LocalDateTime.now())

    fun getCurrentMontTimes(tutorId: TutorId, subjectId: SubjectId): List<Lesson> = findAllByTutorId(
        tutorId,
        subjectId,
        LocalDateTime.now().startOfMonth(),
        LocalDateTime.now().endOfMonth(),
    )

    fun getPrevMonthTimes(tutorId: TutorId, subjectId: SubjectId): List<Lesson> = findAllByTutorId(
        tutorId,
        subjectId,
        LocalDateTime.now().prevMonth().startOfMonth(),
        LocalDateTime.now().prevMonth().endOfMonth(),
    )

    fun getTutorIds(): List<TutorId> =
        runList(
            queryOf(
                """
                SELECT DISTINCT tutor_id
                FROM lessons
                WHERE is_deleted = false
                """
            )
        ) {
            row -> TutorId(row.long("tutor_id"))
        }

    fun setDeleted(lessonId: LessonId, isDeleted: Boolean) =
        update(
            queryOf(
                """
                UPDATE lessons
                SET is_deleted = :is_deleted,
                    update_time = :update_time
                WHERE id = :id
                """,
                    mapOf(
                        "id" to lessonId.id,
                        "is_deleted" to isDeleted,
                        "update_time" to LocalDateTime.now(),
                    ),
                )
        )

    fun getSubjectIdsForStudent(id: StudentId): List<SubjectId> =
        runList(
            queryOf(
                """
                SELECT DISTINCT subject_id
                FROM lessons
                WHERE student_id = :id AND is_deleted=false
                """,
                    mapOf("id" to id.id),
                )
        ) {
            row -> row.long("subject_id").toSubjectId()
        }

    fun setForceGroup(id: LessonId, forceGroup: Boolean) =
        update(
            queryOf(
                """
                UPDATE lessons
                SET force_group = :force_group,
                    update_time = :update_time
                WHERE id = :id
                """,
                    mapOf(
                        "id" to id.id,
                        "force_group" to forceGroup,
                        "update_time" to LocalDateTime.now(),
                    ),
                )
        )

    fun setExtraHalfHour(id: LessonId, extraHalfHour: Boolean) =
        update(
            queryOf(
                """
                UPDATE lessons
                SET extra_half_hour = :extra_half_hour,
                    update_time = :update_time
                WHERE id = :id
                """,
                mapOf(
                    "id" to id.id,
                    "extra_half_hour" to extraHalfHour,
                    "update_time" to LocalDateTime.now(),
                ),
            )
        )

}
