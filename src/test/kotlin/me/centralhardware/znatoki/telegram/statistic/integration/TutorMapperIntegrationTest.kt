package me.centralhardware.znatoki.telegram.statistic.integration

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class TutorMapperIntegrationTest {

    companion object {
        private val testTutorId = TutorId(12345L)

        @JvmStatic
        @BeforeAll
        fun setupDatabase() {
            IntegrationTestSetup.initializeOnce()
        }
    }

    @Test
    fun shouldGetAllTutors() {
        val allTutors = TutorMapper.getAll()

        assertTrue(allTutors.isNotEmpty())

        // Проверяем что наш тестовый пользователь есть в списке
        val testTutor = allTutors.find { it.id == testTutorId }
        assertNotNull(testTutor)
        assertEquals("Test Tutor", testTutor?.name)
    }

    @Test
    fun shouldGetAdminIds() {
        val adminIds = TutorMapper.getAdminsId()

        assertTrue(adminIds.isNotEmpty())

        // Наш тестовый пользователь должен быть админом
        assertTrue(adminIds.contains(testTutorId))
    }

    @Test
    fun shouldFindTutorById() {
        val tutor = TutorMapper.findByIdOrNull(testTutorId)

        assertNotNull(tutor)
        assertEquals(testTutorId, tutor?.id)
        assertEquals("Test Tutor", tutor?.name)

        // Проверяем разрешения
        assertTrue(tutor?.permissions?.contains(Permissions.ADMIN) == true)

        // Проверяем предметы (должны содержать Математика и Физика)
        assertTrue(tutor?.subjects?.isNotEmpty() == true)
    }

    @Test
    fun shouldReturnNullForNonExistentTutor() {
        val nonExistentId = TutorId(999999L)
        val tutor = TutorMapper.findByIdOrNull(nonExistentId)

        assertNull(tutor)
    }

    @Test
    fun shouldVerifyTutorPermissions() {
        val tutor = TutorMapper.findByIdOrNull(testTutorId)

        assertNotNull(tutor)

        // Проверяем что у тестового пользователя есть права ADMIN
        assertTrue(tutor?.permissions?.contains(Permissions.ADMIN) == true)

        // Проверяем что permissions не пуст
        assertTrue(tutor?.permissions?.isNotEmpty() == true)
    }

    @Test
    fun shouldVerifyTutorSubjects() {
        val tutor = TutorMapper.findByIdOrNull(testTutorId)

        assertNotNull(tutor)

        // Проверяем что у тестового пользователя есть предметы
        assertTrue(tutor?.subjects?.isNotEmpty() == true)

        // Согласно тестовым данным, должны быть Математика (1) и Физика (2)
        val subjectIds = tutor?.subjects?.map { it.id } ?: emptyList()
        assertTrue(subjectIds.contains(1L))
        assertTrue(subjectIds.contains(2L))
    }

    @Test
    fun shouldGetAllTutorsWithCorrectStructure() {
        val allTutors = TutorMapper.getAll()

        assertTrue(allTutors.isNotEmpty())

        for (tutor in allTutors) {
            // Проверяем базовую структуру каждого тьютора
            assertNotNull(tutor.id)
            assertNotNull(tutor.name)
            assertNotNull(tutor.permissions)
            assertNotNull(tutor.subjects)

            // ID должен быть положительным
            assertTrue(tutor.id.id > 0)

            // Имя не должно быть пустым
            assertTrue(tutor.name.isNotBlank())
        }
    }

    @Test
    fun shouldGetOnlyAdminTutorsInAdminIds() {
        val allTutors = TutorMapper.getAll()
        val adminIds = TutorMapper.getAdminsId()

        // Все ID в adminIds должны принадлежать тьюторам с правами ADMIN
        for (adminId in adminIds) {
            val tutor = allTutors.find { it.id == adminId }
            assertNotNull(tutor, "Admin ID $adminId not found in tutors list")
            assertTrue(tutor?.permissions?.contains(Permissions.ADMIN) == true,
                "Tutor with ID $adminId doesn't have ADMIN permission")
        }
    }
}