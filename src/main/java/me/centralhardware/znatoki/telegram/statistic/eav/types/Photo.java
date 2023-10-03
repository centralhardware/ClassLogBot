package me.centralhardware.znatoki.telegram.statistic.eav.types;

import io.vavr.control.Validation;
import me.centralhardware.znatoki.telegram.statistic.minio.Minio;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import me.centralhardware.znatoki.telegram.statistic.utils.SpringBootUtils;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

public non-sealed class Photo implements Type {
    @Override
    public String format(String name, Boolean isOptional) {
        return STR."Отправьте фото \{name}. \{isOptional? optionalText: ""}";
    }

    @Override
    public Validation<String, Void> __validate(Update update, String...variants) {
        return update.hasMessage() && update.getMessage().hasPhoto() ?
                Validation.valid(null) :
                Validation.invalid("Отправьте фото");
    }

    @Override
    public Optional<String> extract(Update update) {
        var sender = SpringBootUtils.getBean(TelegramSender.class);
        var minio = SpringBootUtils.getBean(Minio.class);

        return update.getMessage().getPhoto()
                .stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .map(it -> GetFile.builder()
                        .fileId(it.getFileId())
                        .build())
                .flatMap(sender::downloadFile)
                .flatMap(it -> minio.upload(it, LocalDateTime.now()));
    }

}
