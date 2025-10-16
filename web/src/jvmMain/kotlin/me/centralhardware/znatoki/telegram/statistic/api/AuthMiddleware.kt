package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.extensions.hasWebInterfacePermission
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper

val AuthenticatedTutorKey = AttributeKey<TutorId>("AuthenticatedTutor")

/**
 * Authentication plugin for Telegram Web App.
 * Validates Telegram user tokens from Authorization header,
 * checks WEB_INTERFACE permission, and stores authenticated user in call attributes.
 */
val TelegramAuthPlugin = createApplicationPlugin(name = "TelegramAuth") {
    onCall { call ->
        if (call.request.local.uri.startsWith("/static") ||
            call.request.local.uri == "/" ||
            call.request.local.uri.startsWith("/index") ||
            call.request.local.uri.startsWith("/pages") ||
            call.request.local.uri.startsWith("/modals") ||
            call.request.local.uri.endsWith(".js") ||
            call.request.local.uri.endsWith(".css") ||
            call.request.local.uri.endsWith(".html")
        ) {
            return@onCall
        }

        if (call.request.local.uri.startsWith("/api/version")) {
            return@onCall
        }

        if (call.request.local.uri.startsWith("/api/")) {
            val authHeader = call.request.headers["Authorization"]

            if (authHeader == null) {
                KSLog.warning("TelegramAuth: No Authorization header for ${call.request.local.uri}")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authorization required"))
                return@onCall
            }

            val tutorId = extractAndValidateTelegramUser(
                authHeader,
                0,
                "TelegramAuth: ${call.request.local.uri}"
            )

            if (tutorId == null) {
                KSLog.warning("TelegramAuth: Invalid authorization for ${call.request.local.uri}")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid authorization"))
                return@onCall
            }

            val tutor = TutorMapper.findByIdOrNull(tutorId)
            if (tutor == null) {
                KSLog.warning("TelegramAuth: Unknown user ${tutorId.id} for ${call.request.local.uri}")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@onCall
            }

            if (!tutor.hasWebInterfacePermission()) {
                KSLog.warning("TelegramAuth: User ${tutorId.id} doesn't have WEB_INTERFACE permission for ${call.request.local.uri}")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Web interface access denied"))
                return@onCall
            }

            call.attributes.put(AuthenticatedTutorKey, tutorId)
        }
    }
}

val ApplicationCall.authenticatedTutorId: TutorId
    get() = attributes.getOrNull(AuthenticatedTutorKey)
        ?: throw IllegalStateException("User not authenticated")

val ApplicationCall.authenticatedTutorIdOrNull: TutorId?
    get() = attributes.getOrNull(AuthenticatedTutorKey)
