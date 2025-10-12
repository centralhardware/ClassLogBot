package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.Serializable
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.exception.*
import java.io.File
import java.time.LocalDateTime
import java.util.*

@Serializable
data class UploadImageResponse(
    val path: String
)

fun Route.imageApi() {
    route("/api/image") {
        post("/upload") {
            val tutorId = call.authenticatedTutorId
            val multipart = call.receiveMultipart()
            var fileBytes: ByteArray? = null
            var fileName: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        fileName = part.originalFileName ?: "upload.jpg"
                        val source = part.provider()
                        val buffer = ByteArray(source.availableForRead)
                        source.readFully(buffer)
                        fileBytes = buffer
                    }

                    else -> {}
                }
                part.dispose()
            }

            if (fileBytes == null) {
                throw BadRequestException("No file provided")
            }

            val dateTime = LocalDateTime.now()
            val tempFile = File.createTempFile("upload-", ".tmp")
            try {
                tempFile.writeBytes(fileBytes!!)

                MinioService.upload(tempFile, dateTime)
                    .onSuccess { path ->
                        KSLog.info("ImageApi.POST: User ${tutorId.id} uploaded image $path")
                        call.respond(HttpStatusCode.OK, UploadImageResponse(path = path))
                    }
                    .onFailure { error ->
                        KSLog.error("ImageApi.POST: Error uploading image", error)
                        throw Exception("Upload failed")
                    }
            } finally {
                tempFile.delete()
            }
        }

        get("/{path...}") {
            val path = call.parameters.getAll("path")?.joinToString("/")
                ?: throw BadRequestException("Path is required")

            val tutorId = call.authenticatedTutorId
            KSLog.info("ImageApi.GET: User ${tutorId.id} accessing image $path")

            MinioService.get(path)
                .onSuccess { input ->
                    val bytes = input.readBytes()
                    call.respondBytes(
                        bytes = bytes,
                        contentType = ContentType.Image.JPEG,
                        status = HttpStatusCode.OK
                    )
                }
                .onFailure { error ->
                    KSLog.error("ImageApi.GET: Error fetching image $path", error)
                    throw NotFoundException("Image not found")
                }
        }
    }
}
