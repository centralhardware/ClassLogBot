package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Comparator;

@Component
public class PhotoValidator implements Validator<Update, PhotoSize>{

    @Override
    public Either<String, PhotoSize> validate(Update value) {
        if (!value.getMessage().hasPhoto()) {
            return Either.left("Отправьте фото");
        }

        PhotoSize res = value.getMessage().getPhoto()
                .stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElse(null);

        return Either.right(res);
    }

}
