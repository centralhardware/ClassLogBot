package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import me.centralhardware.znatoki.telegram.statistic.utils.TelephoneUtils;

public class TelephoneValidator implements Validator<String, String>{
    @Override
    public Either<String, String> validate(String value) {
        if (!TelephoneUtils.validate(value)){
            return Either.left("Введите номер телефона без + или иных символов");
        }

        return Either.right(value);
    }
}
