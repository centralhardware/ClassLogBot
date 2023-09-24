package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class ClassNumberValidator implements Validator<String, Integer> {

    @Override
    public Either<String, Integer> validate(String text) {
        if (!StringUtils.isNumeric(text)){
            return Either.left("Необходимо ввести число");
        }

        var classNumber = Integer.parseInt(text);
        if (classNumber > 11 || classNumber < 1){
            return Either.left("Класс должен быть в диапазоне 1-11");
        }

        return Either.right(classNumber);
    }

}
