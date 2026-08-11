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
import io.minio.Http
import korlibs.time.seconds
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
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
            .region(Config.Minio.region)
            .credentials(Config.Minio.accessKey, Config.Minio.secretKey)
            .build()

    fun upload(file: File, dateTime: LocalDateTime): Result<String> = runCatching {
        KSLog.info { "Uploading file=${file.name}, size=${file.length()} to MinIO at $dateTime" }

        val objectKey = "${dateTime.year}/${dateTime.month}/${dateTime.dayOfMonth}/${dateTime.hour}-${dateTime.minute}-${UUID.randomUUID()}.jpg"

        val localFile =
            Paths.get(
                "${Config.Minio.basePath}/$objectKey"
            )

        Files.createParentDirs(localFile.toFile())
        Files.touch(localFile.toFile())
        Files.move(file, localFile.toFile())

        minioClient.uploadObject(
            UploadObjectArgs.builder()
                .bucket(Config.Minio.bucket)
                .filename(localFile.toFile().absolutePath)
                .`object`(objectKey)
                .build()
        )

        localFile.toFile().delete()

        KSLog.info { "Successfully uploaded file to MinIO: $objectKey" }
        objectKey
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
        val url = minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Http.Method.GET)
                .bucket(Config.Minio.bucket)
                .`object`(file)
                .expiry(expire.seconds.toInt(), TimeUnit.SECONDS)
                .build()
        )
        withEncodedObjectPath(url, file)
    }.onFailure { KSLog.error(it) }

    /**
     * The signature covers the percent-encoded object key, but the URL built by minio-java keeps
     * reserved characters such as ':' and '=' literal in the path. S3 implementations that verify
     * against the path exactly as received then reject the link with "Invalid signature", so the
     * path is rewritten here to the form that was actually signed.
     */
    private fun withEncodedObjectPath(url: String, key: String): String {
        val encoded = encodeObjectKey(key)
        return if (encoded == key) url else url.replaceFirst("/$key", "/$encoded")
    }

    private fun encodeObjectKey(key: String): String =
        key.split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~")
        }
}
