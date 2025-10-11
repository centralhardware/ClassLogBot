package me.centralhardware.znatoki.telegram.statistic.api

import io.ktor.util.*
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions

/**
 * Проверяет, есть ли у пользователя требуемое право
 * ADMIN право автоматически даёт доступ ко всем остальным правам
 */
fun Permissions.check(userPermissions: List<Permissions>): Boolean {
    // ADMIN имеет все права
    if (userPermissions.contains(Permissions.ADMIN)) {
        return true
    }

    return userPermissions.contains(this)
}

/**
 * Ключ для хранения требуемого уровня прав в route attributes
 */
val RequiredPermissionKey = AttributeKey<Permissions>("RequiredPermission")
