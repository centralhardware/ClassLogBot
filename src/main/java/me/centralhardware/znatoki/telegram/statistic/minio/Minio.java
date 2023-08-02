package me.centralhardware.znatoki.telegram.statistic.minio;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import com.google.common.io.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Minio {

    private final MinioClient minioClient;

    public String upload(File file, LocalDateTime dateTime, String fio, String subject){
        try {
            var fileNew = Paths.get(String.format("%s/%s/%s/%s:%s-%s-%s-%s.jpg",
                    System.getenv("BASE_PATH"),
                    dateTime.getYear(),
                    dateTime.getMonth(),
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
            fileNew.toFile().delete();
            return fileNew.toFile().getAbsolutePath();
        } catch (ErrorResponseException |
                 InsufficientDataException |
                 InternalException |
                 InvalidKeyException |
                 InvalidResponseException |
                 IOException |
                 NoSuchAlgorithmException |
                 ServerException |
                 XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(String file){
        try {
            minioClient.removeObject(RemoveObjectArgs
                    .builder()
                    .bucket("znatoki")
                    .object(file)
                    .build());
        } catch (ErrorResponseException |
                 InsufficientDataException |
                 InternalException |
                 InvalidKeyException |
                 InvalidResponseException |
                 IOException |
                 NoSuchAlgorithmException |
                 ServerException |
                 XmlParserException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream get(String file){
        try {
            return new ByteArrayInputStream(minioClient.getObject(GetObjectArgs
                    .builder()
                    .bucket("znatoki")
                    .object(file)
                    .build()).readAllBytes());
        } catch (ServerException |
                 InsufficientDataException |
                 ErrorResponseException |
                 IOException |
                 NoSuchAlgorithmException |
                 InvalidKeyException |
                 InvalidResponseException |
                 XmlParserException |
                 InternalException e) {
            throw new RuntimeException(e);
        }
    }

}
