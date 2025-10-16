package me.centralhardware.znatoki.telegram.statistic.api

import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.centralhardware.znatoki.telegram.statistic.dto.*
import java.io.File
import java.security.MessageDigest

fun Route.versionApi() {
    route("/api/version") {
        get {
            val hash = calculateFilesHash()
            call.respond(VersionResponse(version = hash))
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
