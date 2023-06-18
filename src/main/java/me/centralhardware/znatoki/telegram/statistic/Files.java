package me.centralhardware.znatoki.telegram.statistic;

import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Files {

    private final MinioClient minioClient;

    public String upload(String file){
        try {
            return minioClient.uploadObject(
                    UploadObjectArgs
                            .builder()
                            .bucket("znatoki")
                            .filename(file)
                            .object(file)
                            .build()
            ).etag();
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
