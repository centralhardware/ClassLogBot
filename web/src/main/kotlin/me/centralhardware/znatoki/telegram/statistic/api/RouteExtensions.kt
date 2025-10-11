package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper

/**
 * Route-scoped plugin для проверки прав доступа
 */
private val PermissionCheckPlugin = createRouteScopedPlugin(
    name = "PermissionCheck",
    createConfiguration = ::PermissionCheckConfiguration
) {
    val permission = pluginConfig.permission

    on(CallFailed) { call, _ -> }

    onCall { call ->
        // Получаем авторизованного пользователя
        val tutorId = call.authenticatedTutorIdOrNull
            ?: run {
                KSLog.warning("PermissionCheck: No authenticated user for ${call.request.local.uri}")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authorization required"))
                return@onCall
            }

        val tutor = TutorMapper.findByIdOrNull(tutorId)!!

        // Проверяем права
        if (!permission.check(tutor.permissions)) {
            KSLog.warning("PermissionCheck: User ${tutorId.id} lacks required permission $permission for ${call.request.local.uri}")
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
        }
    }
}

/**
 * Конфигурация для PermissionCheckPlugin
 */
class PermissionCheckConfiguration {
    lateinit var permission: Permissions
}

/**
 * Extension функция для установки требуемого уровня прав на Route с автоматической проверкой
 */
fun Route.requires(permission: Permissions): Route {
    attributes.put(RequiredPermissionKey, permission)

    install(PermissionCheckPlugin) {
        this.permission = permission
    }

    return this
}

/**
 * Получает требуемый уровень прав для Route
 */
fun Route.getRequiredPermission(): Permissions? {
    return attributes.getOrNull(RequiredPermissionKey)
}

/**
 * Получает требуемый уровень прав, проверяя всю иерархию routes
 */
fun Route.getInheritedPermission(): Permissions? {
    // Проверяем текущий route
    getRequiredPermission()?.let { return it }

    // Если не найдено, проверяем родительские routes
    var current: Route? = parent
    while (current != null) {
        current.getRequiredPermission()?.let { return it }
        current = current.parent
    }

    // По умолчанию авторизация без специфичного права
    return null
}
