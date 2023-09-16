package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FioValidator implements Validator<String, String> {

    private final PupilService pupilService;

    @Override
    public Either<String, String> validate(String value) {
        if (pupilService.findByFioAndId(value) == null){
            return Either.left("ФИО не найдено");
        }

         return Either.right(value);
    }

}
