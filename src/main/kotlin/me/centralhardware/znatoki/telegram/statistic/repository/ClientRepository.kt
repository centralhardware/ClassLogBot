package me.centralhardware.znatoki.telegram.statistic.repository

import me.centralhardware.znatoki.telegram.statistic.entity.Client
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*


@Repository
interface ClientRepository : CrudRepository<Client, Int> {

    override fun findAll(): List<Client>

    fun findAllByNameAndSecondNameAndLastName(name: String, secondName: String, lastName: String): List<Client>

    @Query(value = """
        SELECT *
        FROM client
        WHERE lower(trim(concat(id, ' ', name, ' ', second_name, ' ', last_name))) = lower(:fio)
        ORDER BY create_date DESC
        LIMIT 1
    """, nativeQuery = true)
    fun findByFioAndId(@Param("fio") fio: String): Client?

}