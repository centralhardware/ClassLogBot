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

/**
 * Ключ для хранения авторизованного пользователя в call attributes
 */
val AuthenticatedTutorKey = AttributeKey<TutorId>("AuthenticatedTutor")

/**
 * Plugin для базовой авторизации через Telegram Web App и проверки прав доступа
 */
val TelegramAuthPlugin = createApplicationPlugin(name = "TelegramAuth") {
    onCall { call ->
        // Пропускаем статические файлы
        if (call.request.local.uri.startsWith("/static") ||
            call.request.local.uri == "/" ||
            call.request.local.uri.startsWith("/index") ||
            call.request.local.uri.startsWith("/pages") ||
            call.request.local.uri.startsWith("/modals") ||
            call.request.local.uri.endsWith(".js") ||
            call.request.local.uri.endsWith(".css") ||
            call.request.local.uri.endsWith(".html")) {
            return@onCall
        }

        // Пропускаем эндпоинт версии (публичный)
        if (call.request.local.uri.startsWith("/api/version")) {
            return@onCall
        }

        // Проверяем авторизацию для всех остальных API эндпоинтов
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

            // Проверяем наличие permission WEB_INTERFACE
            if (!tutor.hasWebInterfacePermission()) {
                KSLog.warning("TelegramAuth: User ${tutorId.id} doesn't have WEB_INTERFACE permission for ${call.request.local.uri}")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Web interface access denied"))
                return@onCall
            }

            // Сохраняем авторизованного пользователя в call attributes
            call.attributes.put(AuthenticatedTutorKey, tutorId)
        }
    }
}

/**
 * Extension для получения авторизованного пользователя
 */
val ApplicationCall.authenticatedTutorId: TutorId
    get() = attributes.getOrNull(AuthenticatedTutorKey)
        ?: throw IllegalStateException("User not authenticated")

/**
 * Extension для безопасного получения авторизованного пользователя
 */
val ApplicationCall.authenticatedTutorIdOrNull: TutorId?
    get() = attributes.getOrNull(AuthenticatedTutorKey)
