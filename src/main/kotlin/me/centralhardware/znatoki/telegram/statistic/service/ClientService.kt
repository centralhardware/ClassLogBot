package me.centralhardware.znatoki.telegram.statistic.service

import jakarta.persistence.EntityManager
import me.centralhardware.znatoki.telegram.statistic.entity.Client
import me.centralhardware.znatoki.telegram.statistic.repository.ClientRepository
import jakarta.transaction.Transactional
import me.centralhardware.znatoki.telegram.statistic.Open
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import org.hibernate.search.mapper.orm.Search
import org.hibernate.search.mapper.orm.session.SearchSession
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull

@Service
@Open
class ClientService(
    private val repository: ClientRepository,
    private val entityManager: EntityManager,
    private val log: Logger = LoggerFactory.getLogger(ClientService::class.java)
)
{
    fun getFioById(id: Int) = findById(id)?.fio() ?: ""

    fun save(client: Client): Client {
        CompletableFuture.runAsync(this::updateIndex)
        return repository.save(client)
    }

    @Transactional
    fun search(text: String): List<Client>{
        val query = text.lowercase()
        val session: SearchSession = Search.session(entityManager)
        return session.search(Client::class.java)
            .where { f -> f.match()
                .fields("name", "secondName", "lastName")
                .matching(query)
                .fuzzy()
            }
            .fetchHits(10) as List<Client>
    }

    @Scheduled(fixedRate = 60, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    @Transactional
    fun updateIndex() {
        try {
            log.info("update lucene index")
            Search.session(entityManager).massIndexer().startAndWait()
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }

    fun findById(id: Int): Client? = repository.findById(id)
        .filter { !it.deleted }.getOrNull()


    fun checkExistenceByFio(name: String, secondName: String, lastName: String)
    = repository.findAllByNameAndSecondNameAndLastName(name, secondName, lastName).any { !it.deleted }
}