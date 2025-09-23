package me.centralhardware.znatoki.telegram.statistic.mapper

import java.time.LocalDateTime
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.Lesson
import me.centralhardware.znatoki.telegram.statistic.entity.LessonId
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.parseTime
import me.centralhardware.znatoki.telegram.statistic.entity.toSubjectId
import me.centralhardware.znatoki.telegram.statistic.extensions.endOfMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.prevMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.startOfDay
import me.centralhardware.znatoki.telegram.statistic.extensions.startOfMonth

object LessonMapper {

    fun insert(lesson: Lesson) =
        session.execute(
            queryOf(
                """
            INSERT INTO service (
                date_time,
                update_time,
                id,
                chat_id,
                service_id,
                amount,
                pupil_id,
                photo_report
            ) VALUES (
                :dateTime,
                :updateTime,
                :id,
                :chatId,
                :serviceId,
                :amount,
                :clientId,
                :photo_report
            )
            """,
                mapOf(
                    "dateTime" to lesson.dateTime,
                    "updateTime" to lesson.updateTime,
                    "id" to lesson.id.id,
                    "chatId" to lesson.tutorId.id,
                    "serviceId" to lesson.subjectId.id,
                    "amount" to lesson.amount,
                    "clientId" to lesson.studentId.id,
                    "photo_report" to lesson.photoReport,
                ),
            )
        )

    private fun findAllByTutorId(
        tutorId: TutorId,
        subjectId: SubjectId,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<Lesson> =
        session.runList(
            queryOf(
                """
            SELECT s.id,
                   s.date_time,
                   s.update_time,
                   s.chat_id,
                   s.service_id,
                   s.pupil_id,
                   s.amount,
                   s.force_group,
                   s.extra_half_hour,
                   s.photo_report,
                   s.is_deleted
            FROM service s
            WHERE s.chat_id = :userId
                AND s.service_id = :serviceId
                AND s.date_time between :startDate and :endDate
                AND s.is_deleted=false
            """,
                    mapOf("userId" to tutorId.id,"serviceId" to subjectId.id, "startDate" to startDate, "endDate" to endDate),
                )
        ) { it.parseTime() }

    private fun findAllByTutorId(
        tutorId: TutorId,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<Lesson> =
        session.runList(
            queryOf(
                """
            SELECT s.id,
                   s.date_time,
                   s.update_time,
                   s.chat_id,
                   s.service_id,
                   s.pupil_id,
                   s.amount,
                   s.force_group,
                   s.extra_half_hour,
                   s.photo_report,
                   s.is_deleted
            FROM service s
            WHERE s.chat_id = :userId
                AND s.date_time between :startDate and :endDate
                AND s.is_deleted=false
            """,
                mapOf("userId" to tutorId.id, "startDate" to startDate, "endDate" to endDate),
            )
        ) { it.parseTime() }

    fun findById(id: LessonId): List<Lesson> =
        session.runList(
            queryOf(
                """
            SELECT id,
                   date_time,
                   update_time,
                   chat_id,
                   service_id,
                   pupil_id,
                   amount,
                   photo_report,
                   force_group,
                   extra_half_hour,
                   is_deleted
            FROM service
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
        session.runList(
            queryOf(
                """
            SELECT DISTINCT chat_id
            FROM service
            WHERE is_deleted = false
            """
                )
        ) {
            row -> TutorId(row.long("chat_id"))
        }

    fun setDeleted(lessonId: LessonId, isDeleted: Boolean) =
        session.update(
            queryOf(
                """
            UPDATE service
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
        session.runList(
            queryOf(
                """
            SELECT DISTINCT service_id
            FROM service
            WHERE pupil_id = :id ANd is_deleted=false
            """,
                    mapOf("id" to id.id),
                )
        ) {
            row -> row.long("service_id").toSubjectId()
        }

    fun setForceGroup(id: LessonId, forceGroup: Boolean) =
        session.update(
            queryOf(
                """
        UPDATE service 
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

    fun setExtraHalfHour(id: LessonId, forceGroup: Boolean) =
        session.update(
            queryOf(
                """
        UPDATE service 
        SET extra_half_hour = :extra_half_hour,
            update_time = :update_time
        WHERE id = :id
    """,
                mapOf(
                    "id" to id.id,
                    "extra_half_hour" to forceGroup,
                    "update_time" to LocalDateTime.now(),
                ),
            )
        )

}
