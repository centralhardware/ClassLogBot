package me.centralhardware.znatoki.telegram.statistic.minio;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Component
@RequiredArgsConstructor
public class Minio {

    private final MinioClient minioClient;

    public String upload(String file){
        try {
            minioClient.uploadObject(
                    UploadObjectArgs
                            .builder()
                            .bucket("znatoki")
                            .filename(file)
                            .object(file)
                            .build()
            );
            return file;
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

}
