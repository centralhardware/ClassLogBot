package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Client;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FioValidator implements Validator<String, String> {
    private final ClientService clientService;

    @Override
    public Either<String, String> validate(String fio) {
        Client exists = clientService.findByFioAndId(fio);
        if (exists == null) {
            return Either.left("ФИО не найдено");
        }
        return Either.right(fio);
    }
}
