package me.centralhardware.znatoki.telegram.statistic.api

import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File
import java.security.MessageDigest

@Serializable
data class VersionResponse(val hash: String)

fun Route.versionApi() {
    route("/api/version") {
        get {
            val hash = calculateFilesHash()
            call.respond(VersionResponse(hash = hash))
        }
    }
}

private fun calculateFilesHash(): String {
    val staticDir = File("src/main/resources/static")
    val files = staticDir.walkTopDown()
        .filter { it.isFile && (it.extension == "js" || it.extension == "html" || it.extension == "css") }
        .sortedBy { it.path }
        .toList()

    val digest = MessageDigest.getInstance("MD5")
    files.forEach { file ->
        digest.update(file.readBytes())
    }

    return digest.digest().joinToString("") { "%02x".format(it) }
}
