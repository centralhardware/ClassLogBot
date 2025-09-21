package me.centralhardware.znatoki.telegram.statistic.service

import com.google.common.io.Files
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import io.ktor.utils.io.core.*
import io.minio.GetObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.RemoveObjectArgs
import io.minio.UploadObjectArgs
import io.minio.http.Method
import korlibs.time.seconds
import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*
import me.centralhardware.znatoki.telegram.statistic.Config
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

object MinioService {

    private val minioClient =
        MinioClient.builder()
            .endpoint(Config.Minio.url)
            .credentials(Config.Minio.accessKey, Config.Minio.secretKey)
            .build()

    fun upload(file: File, dateTime: LocalDateTime): Result<String> = runCatching {
        KSLog.info { "Uploading file=${file.name}, size=${file.length()} to MinIO at $dateTime" }

        val fileNew =
            Paths.get(
                "${Config.Minio.basePath}/${dateTime.year}/${dateTime.month}/${dateTime.dayOfMonth}/${dateTime.hour}:${dateTime.minute}=${UUID.randomUUID()}.jpg"
            )

        Files.createParentDirs(fileNew.toFile())
        Files.touch(fileNew.toFile())
        Files.move(file, fileNew.toFile())

        minioClient.uploadObject(
            UploadObjectArgs.builder()
                .bucket(Config.Minio.bucket)
                .filename(fileNew.toFile().absolutePath)
                .`object`(fileNew.toFile().absolutePath)
                .build()
        )

        fileNew.toFile().delete()

        KSLog.info { "Successfully uploaded file to MinIO: ${fileNew.toFile().absolutePath}" }
        fileNew.toFile().absolutePath
    }.onFailure { KSLog.error(it) }

    fun delete(file: String): Result<Unit> = runCatching {
        KSLog.info { "Deleting file from MinIO: $file" }
        minioClient.removeObject(
            RemoveObjectArgs.builder().bucket(Config.Minio.bucket).`object`(file).build()
        )
    }.onFailure { KSLog.error(it) }

    fun get(file: String): Result<Input> = runCatching {
        KSLog.info { "Getting file from MinIO: $file" }
        val input = minioClient
            .getObject(GetObjectArgs.builder().bucket(Config.Minio.bucket).`object`(file).build())
            .readAllBytes()
            .asMultipartFile("Отчет")
            .input
        KSLog.info { "Successfully fetched file from MinIO: $file" }
        input
    }.onFailure { KSLog.error(it) }

    fun getLink(file: String, expire: Duration): Result<String> = runCatching {
        KSLog.info { "Generating presigned link for file=$file, expire=${expire.inWholeSeconds}s" }
        minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(Config.Minio.bucket)
                .`object`(file)
                .expiry(expire.seconds.toInt(), TimeUnit.SECONDS)
                .build()
        )
    }.onFailure { KSLog.error(it) }
}
