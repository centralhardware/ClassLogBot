package me.centralhardware.znatoki.telegram.statistic.api

import io.ktor.util.*
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions

/**
 * Checks if user has the required permission.
 * ADMIN permission automatically grants access to all other permissions.
 */
fun Permissions.check(userPermissions: List<Permissions>): Boolean {
    if (userPermissions.contains(Permissions.ADMIN)) {
        return true
    }

    return userPermissions.contains(this)
}

val RequiredPermissionKey = AttributeKey<Permissions>("RequiredPermission")
