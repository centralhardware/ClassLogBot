package me.centralhardware.znatoki.telegram.statistic.minio;

import com.google.common.io.Files;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.Config;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Minio {

    private final MinioClient minioClient;

    public Try<String> upload(File file, LocalDateTime dateTime, String fio, String subject){
        return Try.of(() -> {
            var fileNew = Paths.get(String.format("%s/%s/%s/%s/%s:%s-%s-%s-%s.jpg",
                    Config.getMinioBasePath(),
                    dateTime.getYear(),
                    dateTime.getMonth(),
                    dateTime.getDayOfMonth(),
                    dateTime.getHour(),
                    dateTime.getMinute(),
                    fio,
                    subject,
                    UUID.randomUUID()));

            Files.createParentDirs(fileNew.toFile());
            Files.touch(fileNew.toFile());
            Files.move(file, fileNew.toFile());

            minioClient.uploadObject(
                    UploadObjectArgs
                            .builder()
                            .bucket("znatoki")
                            .filename(fileNew.toFile().getAbsolutePath())
                            .object(fileNew.toFile().getAbsolutePath())
                            .build()
            );
            //noinspection ResultOfMethodCallIgnored
            fileNew.toFile().delete();
            return fileNew.toFile().getAbsolutePath();
        });
    }

    public Try<Void> delete(String file){
        return Try.of(() -> {
            minioClient.removeObject(RemoveObjectArgs
                    .builder()
                    .bucket("znatoki")
                    .object(file)
                    .build());
            return null;
        });
    }

    public Try<InputStream> get(String file){
        return Try.of(() -> new ByteArrayInputStream(minioClient.getObject(GetObjectArgs
                .builder()
                .bucket("znatoki")
                .object(file)
                .build()).readAllBytes()));
    }

}
