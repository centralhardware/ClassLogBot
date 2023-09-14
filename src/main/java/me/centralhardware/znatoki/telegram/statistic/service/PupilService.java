package me.centralhardware.znatoki.telegram.statistic.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.entity.Pupil;
import me.centralhardware.znatoki.telegram.statistic.repository.PupilRepository;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PupilService {


    private final PupilRepository repository;
    private final EntityManager entityManager;

    public PupilService(PupilRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public Map<String, String> getTelephone(){
        List<Pupil> list = repository.findAll();
        Map<String, String> result = new HashMap<>();
        list.forEach(it -> {
            if (it.getTelephone() == null) return;
            if (it.isDeleted()) return;
            result.put(it.getTelephone(),  String.format("%s %s %s", it.getName(), it.getSecondName(), it.getLastName()));
        });
        return result;
    }

    public List<Pupil> getAll(){
        return repository.findAll()
                .stream()
                .filter(result -> !result.isDeleted())
                .toList();
    }

    public Pupil save(Pupil pupil){
        CompletableFuture.runAsync(this::updateIndex);
        return repository.save(pupil);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public List<Pupil> search(String text) throws InterruptedException {
        var query  = text.toLowerCase();

        SearchSession session = Search.session(entityManager);

        return session.search(Pupil.class)
                .where(it -> it.match()
                        .field("secondName")
                        .field("lastName")
                        .field("name")
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

    public Optional<Pupil> findById(Integer id){
        var result =  repository.findById(id);
        if (result.isEmpty())           return Optional.empty();
        if (result.get().isDeleted())   return Optional.empty();
        return result;
    }

    public Pupil findByFio(String fio){
        return repository.findByFio(fio);
    }

    public boolean checkExistenceByFio(String name, String secondName, String lastName){
        return repository.findAllByNameAndSecondNameAndLastName(name, secondName, lastName)
                .stream()
                .anyMatch(it -> !it.isDeleted());

    }

    public boolean existByTelephone(String telephone){
        return !repository.findByTelephone(telephone).isEmpty() ||
                !repository.findByTelephoneResponsible(telephone).isEmpty();
    }

}
