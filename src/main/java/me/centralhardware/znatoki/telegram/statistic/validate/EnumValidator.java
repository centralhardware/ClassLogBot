package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import org.springframework.stereotype.Component;

@Component
public class EnumValidator implements Validator<String, Subject>{

    @Override
    public Either<Subject, String> validate(String value) {
        Subject val = Subject.of(value);

        if (val == null){
            return Either.right("Введите значение использую клавиатуру");
        }

        return Either.left(val);

    }

}
