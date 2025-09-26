package me.centralhardware.znatoki.telegram.statistic.integration

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class SubjectMapperIntegrationTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupDatabase() {
            IntegrationTestSetup.initializeOnce()
        }
    }

    @Test
    fun shouldGetIdByExistingName() {
        // Тестовые данные уже добавлены в TestDatabaseConfiguration
        val subjectId = SubjectMapper.getIdByName("Математика")

        assertNotNull(subjectId)
        assertEquals(1L, subjectId.id)
    }

    @Test
    fun shouldGetNameByExistingId() {
        val subjectId = SubjectId(1L)
        val subjectName = SubjectMapper.getNameById(subjectId)

        assertEquals("Математика", subjectName)
    }

    @Test
    fun shouldThrowExceptionForNonExistentName() {
        assertThrows<IllegalArgumentException> {
            SubjectMapper.getIdByName("Несуществующий Предмет")
        }
    }

    @Test
    fun shouldThrowExceptionForNonExistentId() {
        val nonExistentId = SubjectId(999L)

        assertThrows<IllegalArgumentException> {
            SubjectMapper.getNameById(nonExistentId)
        }
    }

    @Test
    fun shouldCheckIfAllowGroupForMath() {
        // Математика не должна разрешать групповые занятия (согласно тестовым данным)
        val subjectId = SubjectId(1L)
        val allowGroup = SubjectMapper.isAllowGroup(subjectId)

        assertFalse(allowGroup)
    }

    @Test
    fun shouldCheckIfAllowGroupForPhysics() {
        // Физика должна разрешать групповые занятия (согласно тестовым данным)
        val subjectId = SubjectId(2L)
        val allowGroup = SubjectMapper.isAllowGroup(subjectId)

        assertTrue(allowGroup)
    }

    @Test
    fun shouldThrowExceptionForNonExistentIdInIsAllowGroup() {
        val nonExistentId = SubjectId(999L)

        assertThrows<IllegalArgumentException> {
            SubjectMapper.isAllowGroup(nonExistentId)
        }
    }

    @Test
    fun shouldWorkWithAllTestSubjects() {
        // Проверяем все тестовые предметы
        val mathId = SubjectMapper.getIdByName("Математика")
        val physicsId = SubjectMapper.getIdByName("Физика")
        val chemistryId = SubjectMapper.getIdByName("Химия")

        assertEquals(1L, mathId.id)
        assertEquals(2L, physicsId.id)
        assertEquals(3L, chemistryId.id)

        assertEquals("Математика", SubjectMapper.getNameById(mathId))
        assertEquals("Физика", SubjectMapper.getNameById(physicsId))
        assertEquals("Химия", SubjectMapper.getNameById(chemistryId))

        // Проверяем групповые настройки
        assertFalse(SubjectMapper.isAllowGroup(mathId))    // Математика - индивидуальные
        assertTrue(SubjectMapper.isAllowGroup(physicsId))  // Физика - групповые
        assertFalse(SubjectMapper.isAllowGroup(chemistryId)) // Химия - индивидуальные
    }
}