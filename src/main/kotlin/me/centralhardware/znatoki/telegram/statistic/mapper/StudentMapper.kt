package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.entity.Student
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.parseClient
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle

object StudentMapper {

    fun existsByFio(fio: String): Boolean = session.runSingle(
        queryOf(
            """
            SELECT EXISTS(
            SELECT *
            FROM client
            WHERE lower(trim(concat(id, ' ', name, ' ', second_name, ' ', last_name))) = lower(:fio)
            ORDER BY create_date DESC
            ) as e
        """,
            mapOf("fio" to fio)
            )
    ) { row -> row.boolean("e") } ?: false

    fun save(student: Student): StudentId = session.runSingle(
        queryOf(
            """
               INSERT INTO client (
                        name,
                        last_name,
                        second_name,
               
                        klass,
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
                    
                    :klass,
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

                "klass" to student.schoolClass?.value,
                "record_date" to student.recordDate,
                "birth_date" to student.birthDate,
                "source" to student.source?.name,
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

    fun delete(id: StudentId) = session.update(queryOf(
        """
               UPDATE client 
               SET deleted = true
               WHERE id = :id
            """,
        mapOf("id" to id.id),
    ))

    fun findById(id: StudentId): Student =
        session.runSingle(
            queryOf(
                """
                SELECT c.*
                FROM client c
                WHERE id = :id
            """,
                mapOf("id" to id.id),
            )
        ) { it.parseClient() } ?: throw IllegalArgumentException("No client with id $id found")

    fun findAll(): List<Student> =
        session.runList(
            queryOf(
                """
                SELECT c.*
                FROM client c
                WHERE deleted = false
            """
            )
        ) { it.parseClient() }

    fun findAllByFio(name: String, secondName: String, lastName: String): List<Student> =
        session.runList(
            queryOf(
                """
                SELECT c.*
                FROM client c
                WHERE name = :name AND second_name = :secondName AND last_name = :lastName
            """,
                mapOf("name" to name, "second_name" to secondName, "last_name" to lastName),
            )
        ) { it.parseClient() }

    fun getFioById(id: StudentId): String =
        session.runSingle(
            queryOf(
                """
            SELECT concat(name, ' ', last_name, ' ', second_name) as fio
            from client
            WHERE id = :id
        """,
                mapOf("id" to id.id),
            )
        ) { row -> row.string("fio") } ?: ""
}
