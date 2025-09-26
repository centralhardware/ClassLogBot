package me.centralhardware.znatoki.telegram.statistic.integration

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

@Testcontainers
class LessonMapperIntegrationTest {

    companion object {
        private var studentId: StudentId? = null
        private val tutorId = TutorId(12345L)
        private val subjectId = SubjectId(1L)

        @JvmStatic
        @BeforeAll
        fun setupDatabase() {
            IntegrationTestSetup.initializeOnce()

            // Создаем тестового студента
            val student = Student(
                id = StudentId.None,
                name = "Тестовый",
                secondName = "Студент",
                lastName = "Для Занятий",
                createdBy = tutorId,
                updateBy = tutorId
            )
            studentId = StudentMapper.save(student)
        }
    }

    @Test
    fun shouldInsertAndFindLesson() {
        val lesson = Lesson(
            id = LessonId.random(),
            dateTime = LocalDateTime.now(),
            tutorId = tutorId,
            subjectId = subjectId,
            studentId = studentId!!,
            _amount = Amount(500),
            photoReport = "test-photo-url"
        )

        LessonMapper.insert(lesson)

        val foundLessons = LessonMapper.findById(lesson.id)
        assertTrue(foundLessons.isNotEmpty())
        assertEquals(1, foundLessons.size)

        val foundLesson = foundLessons.first()
        assertEquals(lesson.id, foundLesson.id)
        assertEquals(tutorId, foundLesson.tutorId)
        assertEquals(subjectId, foundLesson.subjectId)
        assertEquals(studentId, foundLesson.studentId)
        assertEquals(500.0, foundLesson.amount)
        assertEquals("test-photo-url", foundLesson.photoReport)
    }

    @Test
    fun shouldGetTodaysLessons() {
        val todayLesson = Lesson(
            id = LessonId.random(),
            dateTime = LocalDateTime.now(),
            tutorId = tutorId,
            subjectId = subjectId,
            studentId = studentId!!,
            _amount = Amount(300)
        )

        LessonMapper.insert(todayLesson)

        val todayLessons = LessonMapper.getTodayTimes(tutorId)
        assertTrue(todayLessons.isNotEmpty())
        assertTrue(todayLessons.any { it.id == todayLesson.id })
    }

    @Test
    fun shouldSoftDeleteLesson() {
        val lesson = Lesson(
            id = LessonId.random(),
            dateTime = LocalDateTime.now(),
            tutorId = tutorId,
            subjectId = subjectId,
            studentId = studentId!!,
            _amount = Amount(400)
        )

        LessonMapper.insert(lesson)
        LessonMapper.setDeleted(lesson.id, true)

        val foundLessons = LessonMapper.findById(lesson.id)
        assertTrue(foundLessons.isNotEmpty())
        assertTrue(foundLessons.first().deleted)
    }

    @Test
    fun shouldSetForceGroupFlag() {
        val lesson = Lesson(
            id = LessonId.random(),
            dateTime = LocalDateTime.now(),
            tutorId = tutorId,
            subjectId = subjectId,
            studentId = studentId!!,
            _amount = Amount(350)
        )

        LessonMapper.insert(lesson)
        LessonMapper.setForceGroup(lesson.id, true)

        val foundLessons = LessonMapper.findById(lesson.id)
        assertTrue(foundLessons.isNotEmpty())
        assertTrue(foundLessons.first().forceGroup)
    }

    @Test
    fun shouldSetExtraHalfHourFlag() {
        val lesson = Lesson(
            id = LessonId.random(),
            dateTime = LocalDateTime.now(),
            tutorId = tutorId,
            subjectId = subjectId,
            studentId = studentId!!,
            _amount = Amount(600)
        )

        LessonMapper.insert(lesson)
        LessonMapper.setExtraHalfHour(lesson.id, true)

        val foundLessons = LessonMapper.findById(lesson.id)
        assertTrue(foundLessons.isNotEmpty())
        val foundLesson = foundLessons.first()
        assertTrue(foundLesson.extraHalfHour)
        assertEquals(900.0, foundLesson.amount) // 600 * 1.5
    }

    @Test
    fun shouldGetSubjectIdsForStudent() {
        val subjectIds = LessonMapper.getSubjectIdsForStudent(studentId!!)
        assertTrue(subjectIds.isNotEmpty())
        assertTrue(subjectIds.contains(subjectId))
    }

    @Test
    fun shouldGetTutorIds() {
        val tutorIds = LessonMapper.getTutorIds()
        assertTrue(tutorIds.isNotEmpty())
        assertTrue(tutorIds.contains(tutorId))
    }
}