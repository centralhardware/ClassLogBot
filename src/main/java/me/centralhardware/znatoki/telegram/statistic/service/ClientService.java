package me.centralhardware.znatoki.telegram.statistic.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.entity.Client;
import me.centralhardware.znatoki.telegram.statistic.repository.ClientRepository;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.util.function.Predicate.not;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientService {


    private final ClientRepository repository;
    private final EntityManager entityManager;

    public String getFioById(Integer id){
        return findById(id)
                .map(Client::getFio)
                .orElse("");
    }

    public List<Client> getAll(){
        return repository.findAll()
                .stream()
                .filter(result -> !result.isDeleted())
                .toList();
    }

    public Client save(Client client){
        CompletableFuture.runAsync(this::updateIndex);
        return repository.save(client);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public List<Client> search(String text) {
        var query  = text.toLowerCase();

        SearchSession session = Search.session(entityManager);

        return session.search(Client.class)
                .where(it -> it.match()
                        .field("name")
                        .field("secondName")
                        .field("lastName")
                        .matching(query)
                        .fuzzy())
                .fetch(10)
                .hits();
    }

    @Scheduled(fixedRate = 60, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void updateIndex() {
        try {
            log.info("update lucene index");
            Search.session(entityManager).massIndexer().startAndWait();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Client> findById(Integer id){
        return repository.findById(id)
                .filter(not(Client::isDeleted));
    }

    public Client findByFioAndId(String fio){
        return repository.findByFioAndId(fio);
    }

    public boolean checkExistenceByFio(String name, String secondName, String lastName){
        return repository.findAllByNameAndSecondNameAndLastName(name, secondName, lastName)
                .stream()
                .anyMatch(it -> !it.isDeleted());

    }


}
