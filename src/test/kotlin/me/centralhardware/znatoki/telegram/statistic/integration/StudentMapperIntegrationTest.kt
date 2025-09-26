package me.centralhardware.znatoki.telegram.statistic.integration

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate

@Testcontainers
class StudentMapperIntegrationTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupDatabase() {
            IntegrationTestSetup.initializeOnce()
        }
    }

    @Test
    fun shouldSaveAndFindStudentById() {
        val student = Student(
            id = StudentId.None,
            name = "Иван",
            secondName = "Иванович",
            lastName = "Иванов",
            schoolClass = SchoolClass(5),
            recordDate = LocalDate.of(2024, 1, 15),
            birthDate = LocalDate.of(2010, 3, 20),
            source = SourceOption.SIGNBOARD,
            phone = PhoneNumber("71234567890"),
            responsiblePhone = PhoneNumber("79876543210"),
            motherFio = "Мария Петровна Иванова",
            createdBy = TutorId(12345),
            updateBy = TutorId(12345)
        )

        val savedId = StudentMapper.save(student)
        assertNotNull(savedId)

        val foundStudent = StudentMapper.findById(savedId)
        assertEquals("Иван", foundStudent.name)
        assertEquals("Иванович", foundStudent.secondName)
        assertEquals("Иванов", foundStudent.lastName)
        assertEquals(5, foundStudent.schoolClass?.value)
        assertEquals(SourceOption.SIGNBOARD, foundStudent.source)
        assertEquals("71234567890", foundStudent.phone?.value)
        assertEquals("79876543210", foundStudent.responsiblePhone?.value)
        assertEquals("Мария Петровна Иванова", foundStudent.motherFio)
        assertEquals(TutorId(12345), foundStudent.createdBy)
    }

    @Test
    fun shouldFindAllNonDeletedStudents() {
        val student1 = Student(
            id = StudentId.None,
            name = "Петр",
            secondName = "Петрович",
            lastName = "Петров",
            schoolClass = SchoolClass(7),
            createdBy = TutorId(12345),
            updateBy = TutorId(12345)
        )

        val student2 = Student(
            id = StudentId.None,
            name = "Сидор",
            secondName = "Сидорович",
            lastName = "Сидоров",
            schoolClass = SchoolClass(9),
            createdBy = TutorId(12345),
            updateBy = TutorId(12345)
        )

        val id1 = StudentMapper.save(student1)
        val id2 = StudentMapper.save(student2)

        val allStudents = StudentMapper.findAll()
        assertTrue(allStudents.size >= 2)

        val studentNames = allStudents.map { it.name }
        assertTrue(studentNames.contains("Петр"))
        assertTrue(studentNames.contains("Сидор"))
    }

    @Test
    fun shouldDeleteStudentSoftDelete() {
        val student = Student(
            id = StudentId.None,
            name = "Удаляемый",
            secondName = "Удаляемович",
            lastName = "Удаляемов",
            createdBy = TutorId(12345),
            updateBy = TutorId(12345)
        )

        val savedId = StudentMapper.save(student)
        StudentMapper.delete(savedId)

        val allStudents = StudentMapper.findAll()
        val deletedStudent = allStudents.find { it.id == savedId }
        assertNull(deletedStudent)
    }

    @Test
    fun shouldCheckIfStudentExistsByFIO() {
        val allStudents = StudentMapper.findAll()
        if (allStudents.isNotEmpty()) {
            val existingStudent = allStudents.first()
            val fio = "${existingStudent.id?.id} ${existingStudent.name} ${existingStudent.secondName} ${existingStudent.lastName}"

            val exists = StudentMapper.existsByFio(fio)
            assertTrue(exists)
        }

        val nonExistentFio = "999 Несуществующий Несуществующий Несуществующий"
        val notExists = StudentMapper.existsByFio(nonExistentFio)
        assertFalse(notExists)
    }
}