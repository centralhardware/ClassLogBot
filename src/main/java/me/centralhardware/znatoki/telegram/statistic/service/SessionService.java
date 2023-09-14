package me.centralhardware.znatoki.telegram.statistic.service;

import me.centralhardware.znatoki.telegram.statistic.entity.Pupil;
import me.centralhardware.znatoki.telegram.statistic.entity.Session;
import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser;
import me.centralhardware.znatoki.telegram.statistic.repository.SessionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public String create(Pupil pupil, TelegramUser telegramUser) {
        return sessionRepository.save(new Session(pupil, telegramUser)).getUuid();
    }

    public Optional<Session> findByUuid(String uuid) {
        return sessionRepository.findByUuid(uuid);
    }

}
