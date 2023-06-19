package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.Clickhouse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FioValidator implements Validator<String, String> {

    private final Clickhouse clickhouse;

    @Override
    public Either<String, String> validate(String value) {
        if (!clickhouse.exist(value)){
            return Either.right("ФИО не найдено");
        }

         return Either.left(value);
    }

}
