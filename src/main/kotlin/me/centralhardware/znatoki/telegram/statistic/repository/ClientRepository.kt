package me.centralhardware.znatoki.telegram.statistic.repository

import me.centralhardware.znatoki.telegram.statistic.entity.Client
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository


@Repository
interface ClientRepository : CrudRepository<Client, Int> {

    override fun findAll(): List<Client>

    fun findAllByNameAndSecondNameAndLastName(name: String, secondName: String, lastName: String): List<Client>

}