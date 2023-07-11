package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.mapper.PupilMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FioValidator implements Validator<String, String> {

    private final PupilMapper pupilMapper;

    @Override
    public Either<String, String> validate(String value) {
        if (!pupilMapper.exist(value)){
            return Either.left("ФИО не найдено");
        }

         return Either.right(value);
    }

}
