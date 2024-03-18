package me.centralhardware.znatoki.telegram.statistic.service

import com.google.common.io.Files
import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.RemoveObjectArgs
import io.minio.UploadObjectArgs
import me.centralhardware.znatoki.telegram.statistic.Config
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*

@Component
class MinioService(private val minioClient: MinioClient) {

    fun upload(file: File, dateTime: LocalDateTime): Result<String> = runCatching {
        val fileNew = Paths.get("${Config.Minio.basePath}/${dateTime.year}/${dateTime.dayOfMonth}/${dateTime.hour}:${dateTime.minute}=${UUID.randomUUID()}")

        Files.createParentDirs(fileNew.toFile())
        Files.touch(fileNew.toFile())
        Files.move(file, fileNew.toFile())

        minioClient.uploadObject(
            UploadObjectArgs.builder()
                .bucket(Config.Minio.bucket)
                .filename(fileNew.toFile().absolutePath)
                .`object`(fileNew.toFile().absolutePath)
                .build())

        fileNew.toFile().delete()
        fileNew.toFile().absolutePath
    }

    fun delete(file: String): Result<Unit> = runCatching {
        minioClient.removeObject(RemoveObjectArgs.builder()
            .bucket(Config.Minio.bucket)
            .`object`(file)
            .build())
    }

    fun get(file: String): Result<InputStream>  = runCatching {
        ByteArrayInputStream(minioClient.getObject(GetObjectArgs
            .builder()
            .bucket(Config.Minio.bucket)
            .`object`(file)
            .build()).readAllBytes())
    }
}