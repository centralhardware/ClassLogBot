package me.centralhardware.znatoki.telegram.statistic.service

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import dev.inmo.tgbotapi.requests.abstracts.asMultipartFile
import io.ktor.utils.io.core.*
import io.minio.GetObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.Http
import korlibs.time.seconds
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
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

    /** Telegram always delivers photos as JPEG, hence the fixed extension and content type. */
    fun upload(bytes: ByteArray, dateTime: LocalDateTime): Result<String> = runCatching {
        KSLog.info { "Uploading ${bytes.size} bytes to MinIO at $dateTime" }

        val objectKey = "${dateTime.year}/${dateTime.month}/${dateTime.dayOfMonth}/${dateTime.hour}-${dateTime.minute}-${UUID.randomUUID()}.jpg"

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(Config.Minio.bucket)
                .`object`(objectKey)
                .stream(ByteArrayInputStream(bytes), bytes.size.toLong(), -1)
                .contentType("image/jpeg")
                .build()
        )

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

    fun getBytes(file: String): Result<ByteArray> = runCatching {
        KSLog.info { "Getting file from MinIO: $file" }
        val bytes = minioClient
            .getObject(GetObjectArgs.builder().bucket(Config.Minio.bucket).`object`(file).build())
            .readAllBytes()
        KSLog.info { "Successfully fetched file from MinIO: $file" }
        bytes
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
