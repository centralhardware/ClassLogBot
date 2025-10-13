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
        val tutorId = call.authenticatedTutorIdOrNull
            ?: run {
                KSLog.warning("PermissionCheck: No authenticated user for ${call.request.local.uri}")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authorization required"))
                return@onCall
            }

        val tutor = TutorMapper.findByIdOrNull(tutorId)!!

        if (!permission.check(tutor.permissions)) {
            KSLog.warning("PermissionCheck: User ${tutorId.id} lacks required permission $permission for ${call.request.local.uri}")
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Insufficient permissions"))
        }
    }
}

class PermissionCheckConfiguration {
    lateinit var permission: Permissions
}

fun Route.requires(permission: Permissions): Route {
    attributes.put(RequiredPermissionKey, permission)

    install(PermissionCheckPlugin) {
        this.permission = permission
    }

    return this
}
