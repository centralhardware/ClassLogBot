package me.centralhardware.znatoki.telegram.statistic.repository;

import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelegramUserRepository extends CrudRepository<TelegramUser, Long> {

    List<TelegramUser> findAll();

}
