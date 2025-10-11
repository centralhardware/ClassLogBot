package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.Student
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.parseClient
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle
import me.centralhardware.znatoki.telegram.statistic.extensions.update

object StudentMapper {

    fun existsByFio(fio: String): Boolean = runSingle(
        queryOf(
            """
            SELECT EXISTS(
                SELECT *
                FROM students
                WHERE lower(trim(regexp_replace(concat(id, ' ', second_name, ' ', name, ' ', last_name), '\s+', ' ', 'g'))) = 
                      lower(trim(regexp_replace(:fio, '\s+', ' ', 'g')))
                  AND deleted = false
            ) as e
            """,
            mapOf("fio" to fio)
            )
    ) { row -> row.boolean("e") } ?: false

    fun save(student: Student): StudentId = runSingle(
        queryOf(
            """
            INSERT INTO students (
                name,
                last_name,
                second_name,
                school_class,
                record_date,
                birth_date,
                source,
                phone,
                responsible_phone,
                mother_fio,
                created_by,
                create_date,
                modify_date,
                deleted
            ) VALUES (
                :name,
                :last_name,
                :second_name,
                :school_class,
                :record_date,
                :birth_date,
                :source,
                :phone,
                :responsible_phone,
                :mother_fio,
                :created_by,
                :create_date,
                :modify_date,
                false
            ) RETURNING id
            """,
            mapOf(
                "name" to student.name,
                "last_name" to student.lastName,
                "second_name" to student.secondName,

                "school_class" to student.schoolClass?.value,
                "record_date" to student.recordDate,
                "birth_date" to student.birthDate,
                "source" to student.source?.title,
                "phone" to student.phone?.value,
                "responsible_phone" to student.responsiblePhone?.value,
                "mother_fio" to student.motherFio,

                "created_by" to student.createdBy.id,
                "create_date" to student.createDate,
                "modify_date" to student.modifyDate,
            )
        )
    ) {
        it.int("id").toStudentId()
    }!!

    fun delete(id: StudentId) = update(queryOf(
        """
        UPDATE students
        SET deleted = true
        WHERE id = :id
        """,
        mapOf("id" to id.id),
    ))

    fun update(student: Student) = update(queryOf(
        """
        UPDATE students
        SET name = :name,
            last_name = :last_name,
            second_name = :second_name,
            school_class = :school_class,
            record_date = :record_date,
            birth_date = :birth_date,
            source = :source,
            phone = :phone,
            responsible_phone = :responsible_phone,
            mother_fio = :mother_fio,
            modify_date = :modify_date,
            update_by = :update_by
        WHERE id = :id
        """,
        mapOf(
            "id" to student.id.id,
            "name" to student.name,
            "last_name" to student.lastName,
            "second_name" to student.secondName,
            "school_class" to student.schoolClass?.value,
            "record_date" to student.recordDate,
            "birth_date" to student.birthDate,
            "source" to student.source?.title,
            "phone" to student.phone?.value,
            "responsible_phone" to student.responsiblePhone?.value,
            "mother_fio" to student.motherFio,
            "modify_date" to student.modifyDate,
            "update_by" to student.updateBy?.id,
        )
    ))

    fun findById(id: StudentId): Student =
        runSingle(
            queryOf(
                """
                SELECT s.*
                FROM students s
                WHERE id = :id
                """,
                mapOf("id" to id.id),
            )
        ) { it.parseClient() } ?: throw IllegalArgumentException("No client with id $id found")

    fun findAll(): List<Student> =
        runList(
            queryOf(
                """
                SELECT s.*
                FROM students s
                WHERE deleted = false
                """
            )
        ) { it.parseClient() }

    fun findAllByFio(name: String, secondName: String, lastName: String): List<Student> =
        runList(
            queryOf(
                """
                SELECT s.*
                FROM students s
                WHERE name = :name AND second_name = :secondName AND last_name = :lastName
                """,
                mapOf("name" to name, "secondName" to secondName, "lastName" to lastName),
            )
        ) { it.parseClient() }

    fun getFioById(id: StudentId): String =
        runSingle(
            queryOf(
                """
                SELECT concat(name, ' ', last_name, ' ', second_name) as fio
                FROM students
                WHERE id = :id
                """,
                mapOf("id" to id.id),
            )
        ) { row -> row.string("fio") } ?: ""
}
