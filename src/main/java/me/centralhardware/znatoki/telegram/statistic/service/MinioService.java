package me.centralhardware.znatoki.telegram.statistic.service;

import com.google.common.io.Files;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.Config;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    public Optional<String> upload(File file, LocalDateTime dateTime){
        return Try.of(() -> {
                    var fileNew = Paths.get(String.format("%s/%s/%s/%s/%s:%s-%s.jpg",
                            Config.getMinioBasePath(),
                            dateTime.getYear(),
                            dateTime.getMonth(),
                            dateTime.getDayOfMonth(),
                            dateTime.getHour(),
                            dateTime.getMinute(),
                            UUID.randomUUID()));

                    Files.createParentDirs(fileNew.toFile());
                    Files.touch(fileNew.toFile());
                    Files.move(file, fileNew.toFile());

                    minioClient.uploadObject(
                            UploadObjectArgs
                                    .builder()
                                    .bucket(Config.getMinioBucket())
                                    .filename(fileNew.toFile().getAbsolutePath())
                                    .object(fileNew.toFile().getAbsolutePath())
                                    .build()
                    );
                    //noinspection ResultOfMethodCallIgnored
                    fileNew.toFile().delete();
                    return fileNew.toFile().getAbsolutePath();
                })
                .onFailure(error -> log.warn("", error))
                .toJavaOptional();
    }

    public Try<Void> delete(String file){
        return Try.of(() -> {
            minioClient.removeObject(RemoveObjectArgs
                    .builder()
                    .bucket(Config.getMinioBucket())
                    .object(file)
                    .build());
            return null;
        });
    }

    public Try<InputStream> get(String file){
        return Try.of(() -> new ByteArrayInputStream(minioClient.getObject(GetObjectArgs
                .builder()
                .bucket(Config.getMinioBucket())
                .object(file)
                .build()).readAllBytes()));
    }

}
