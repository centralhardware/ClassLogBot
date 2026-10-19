package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.Session
import java.time.LocalDateTime
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.Lesson
import me.centralhardware.znatoki.telegram.statistic.entity.LessonId
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.parseTime
import me.centralhardware.znatoki.telegram.statistic.entity.toSubjectId
import me.centralhardware.znatoki.telegram.statistic.extensions.startOfNextMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.prevMonth
import me.centralhardware.znatoki.telegram.statistic.extensions.execute
import me.centralhardware.znatoki.telegram.statistic.extensions.transaction
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.update
import me.centralhardware.znatoki.telegram.statistic.extensions.startOfDay
import me.centralhardware.znatoki.telegram.statistic.extensions.startOfMonth
import java.time.YearMonth

private const val LESSON_COLUMNS =
    "id, date_time, update_time, tutor_id, subject_id, student_id, amount, force_group, extra_half_hour, photo_report, is_deleted, added_by_tutor_id, data_source"

object LessonMapper {

    context(_: Session)
    fun insert(lesson: Lesson) = execute(insertQuery(lesson))

    /** Inserts all rows of a (group) lesson atomically. */
    /** Several rows share one lesson id; callers run this inside [transaction] so they land together. */
    context(_: Session)
    fun insertAll(lessons: List<Lesson>) = lessons.forEach { execute(insertQuery(it)) }

    private fun insertQuery(lesson: Lesson) =
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
                added_by_tutor_id,
                data_source
            ) VALUES (
                :dateTime,
                :updateTime,
                :id,
                :tutorId,
                :subjectId,
                :amount,
                :studentId,
                :photo_report,
                :addedByTutorId,
                :dataSource
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
                "dataSource" to lesson.dataSource?.value,
            ),
        )

    context(_: Session)
    private fun findAllByTutorId(
        tutorId: TutorId,
        subjectId: SubjectId,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<Lesson> =
        runList(
            queryOf(
                """
                SELECT $LESSON_COLUMNS
                FROM lessons l
                WHERE l.tutor_id = :tutorId
                    AND l.subject_id = :subjectId
                    AND l.date_time >= :startDate AND l.date_time < :endDate
                    AND l.is_deleted=false
                """,
                    mapOf("tutorId" to tutorId.id,"subjectId" to subjectId.id, "startDate" to startDate, "endDate" to endDate),
                )
        ) { it.parseTime() }

    context(_: Session)
    private fun findAllByTutorId(
        tutorId: TutorId,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<Lesson> =
        runList(
            queryOf(
                """
                SELECT $LESSON_COLUMNS
                FROM lessons l
                WHERE l.tutor_id = :tutorId
                    AND l.date_time >= :startDate AND l.date_time < :endDate
                    AND l.is_deleted=false
                """,
                mapOf("tutorId" to tutorId.id, "startDate" to startDate, "endDate" to endDate),
            )
        ) { it.parseTime() }

    context(_: Session)
    fun findById(id: LessonId): List<Lesson> =
        runList(
            queryOf(
                """
                SELECT $LESSON_COLUMNS
                FROM lessons
                WHERE id = :id
                """,
                    mapOf("id" to id.id),
                )
        ) { it.parseTime() }

    context(_: Session)
    fun getTodayTimes(tutorId: TutorId): List<Lesson> =
        findAllByTutorId(tutorId, LocalDateTime.now().startOfDay(), LocalDateTime.now())

    context(_: Session)
    fun getCurrentMontTimes(tutorId: TutorId, subjectId: SubjectId): List<Lesson> = findAllByTutorId(
        tutorId,
        subjectId,
        LocalDateTime.now().startOfMonth(),
        LocalDateTime.now().startOfNextMonth(),
    )

    context(_: Session)
    fun getPrevMonthTimes(tutorId: TutorId, subjectId: SubjectId): List<Lesson> = findAllByTutorId(
        tutorId,
        subjectId,
        LocalDateTime.now().prevMonth().startOfMonth(),
        LocalDateTime.now().prevMonth().startOfNextMonth(),
    )

    context(_: Session)
    fun getTimesByMonth(tutorId: TutorId, subjectId: SubjectId, yearMonth: YearMonth): List<Lesson> = findAllByTutorId(
        tutorId,
        subjectId,
        yearMonth.atDay(1).atStartOfDay(),
        yearMonth.plusMonths(1).atDay(1).atStartOfDay(),
    )

    /** [startDate, endDate) — endDate is exclusive. */
    context(_: Session)
    fun getTimesByDateRange(tutorId: TutorId, subjectId: SubjectId, startDate: LocalDateTime, endDate: LocalDateTime): List<Lesson> = findAllByTutorId(
        tutorId,
        subjectId,
        startDate,
        endDate,
    )

    context(_: Session)
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

    context(_: Session)
    fun setDeleted(lessonId: LessonId, isDeleted: Boolean, studentId: StudentId? = null) =
        update(
            queryOf(
                if (studentId != null) {
                    """
                    UPDATE lessons
                    SET is_deleted = :is_deleted,
                        update_time = :update_time
                    WHERE id = :id AND student_id = :student_id
                    """
                } else {
                    """
                    UPDATE lessons
                    SET is_deleted = :is_deleted,
                        update_time = :update_time
                    WHERE id = :id
                    """
                },
                mapOf(
                    "id" to lessonId.id,
                    "is_deleted" to isDeleted,
                    "update_time" to LocalDateTime.now(),
                    "student_id" to studentId?.id,
                ).filterValues { it != null },
            )
        )

    context(_: Session)
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

    context(_: Session)
    fun findAllByStudent(studentId: StudentId): List<Lesson> =
        runList(
            queryOf(
                """
                SELECT $LESSON_COLUMNS
                FROM lessons
                WHERE student_id = :studentId AND is_deleted = false
                ORDER BY date_time DESC
                """,
                mapOf("studentId" to studentId.id)
            )
        ) { it.parseTime() }

    context(_: Session)
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

    context(_: Session)
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

    context(_: Session)
    fun setAmount(id: LessonId, amount: me.centralhardware.znatoki.telegram.statistic.entity.Amount) =
        update(
            queryOf(
                """
                UPDATE lessons
                SET amount = :amount,
                    update_time = :update_time
                WHERE id = :id
                """,
                mapOf(
                    "id" to id.id,
                    "amount" to amount.amount,
                    "update_time" to LocalDateTime.now(),
                ),
            )
        )

    context(_: Session)
    fun setSubjectId(id: LessonId, subjectId: SubjectId) =
        update(
            queryOf(
                """
                UPDATE lessons
                SET subject_id = :subject_id,
                    update_time = :update_time
                WHERE id = :id
                """,
                mapOf(
                    "id" to id.id,
                    "subject_id" to subjectId.id,
                    "update_time" to LocalDateTime.now(),
                ),
            )
        )

}
