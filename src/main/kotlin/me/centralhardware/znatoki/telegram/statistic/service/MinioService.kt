package me.centralhardware.znatoki.telegram.statistic.service

import com.google.common.io.Files
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import io.ktor.utils.io.core.*
import io.minio.GetObjectArgs
import io.minio.MinioClient
import io.minio.RemoveObjectArgs
import io.minio.UploadObjectArgs
import me.centralhardware.znatoki.telegram.statistic.Config
import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*

object MinioService {

    val minioClient = MinioClient.builder()
        .endpoint(Config.Minio.url, Config.Minio.port, false)
        .credentials(Config.Minio.accessKey,
            Config.Minio.secretKey)
        .build()

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

    fun get(file: String): Result<Input> = runCatching {
        minioClient.getObject(GetObjectArgs
            .builder()
            .bucket(Config.Minio.bucket)
            .`object`(file)
            .build()).readAllBytes()
            .asMultipartFile("Отчет")
            .input
    }
}