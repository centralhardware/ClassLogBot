package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.Configuration;
import me.centralhardware.znatoki.telegram.statistic.mapper.PupilMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FioValidator implements Validator<String, String> {

    private final PupilMapper pupilMapper;
    private final Configuration configuration;

    @Override
    public Either<String, String> validate(String value) {
        if (configuration.getIsDemo()) return Either.right(value);

        if (!pupilMapper.exist(value)){
            return Either.left("ФИО не найдено");
        }

         return Either.right(value);
    }

}
