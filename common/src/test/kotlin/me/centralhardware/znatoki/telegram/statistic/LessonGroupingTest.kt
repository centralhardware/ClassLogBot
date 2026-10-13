package me.centralhardware.znatoki.telegram.statistic

import me.centralhardware.znatoki.telegram.statistic.entity.Lesson
import me.centralhardware.znatoki.telegram.statistic.entity.LessonId
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.isGroup
import me.centralhardware.znatoki.telegram.statistic.entity.isIndividual
import me.centralhardware.znatoki.telegram.statistic.entity.toAmount
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The rules the monthly report uses to count individual vs group lessons and to price them. */
class LessonGroupingTest {

    private val id = LessonId.random()

    private fun lesson(student: Int, amount: Int = 1000, forceGroup: Boolean = false, extraHalfHour: Boolean = false) =
        Lesson(
            id = id,
            tutorId = TutorId(1),
            subjectId = SubjectId(1L),
            studentId = student.toStudentId(),
            _amount = amount.toAmount(),
            forceGroup = forceGroup,
            extraHalfHour = extraHalfHour,
        )

    @Test
    fun `one student is individual unless forced to group`() {
        assertTrue(listOf(lesson(1)).isIndividual())
        assertFalse(listOf(lesson(1)).isGroup())
        assertTrue(listOf(lesson(1, forceGroup = true)).isGroup())
        assertFalse(listOf(lesson(1, forceGroup = true)).isIndividual())
    }

    @Test
    fun `several students sharing a lesson id are a group`() {
        val group = listOf(lesson(1), lesson(2))
        assertTrue(group.isGroup())
        assertFalse(group.isIndividual())
    }

    @Test
    fun `extra half hour multiplies the price by 1_5`() {
        assertEquals(1000.0, lesson(1).amount)
        assertEquals(1500.0, lesson(1, extraHalfHour = true).amount)
    }
}
