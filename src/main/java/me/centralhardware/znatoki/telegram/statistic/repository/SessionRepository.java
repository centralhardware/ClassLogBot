package me.centralhardware.znatoki.telegram.statistic.repository;


import me.centralhardware.znatoki.telegram.statistic.entity.Session;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface SessionRepository extends CrudRepository<Session, String> {

    Optional<Session> findByUuid(String uuid);

}
