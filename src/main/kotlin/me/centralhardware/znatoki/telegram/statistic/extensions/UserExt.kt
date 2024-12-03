package me.centralhardware.znatoki.telegram.statistic.extensions

import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser

fun TelegramUser.hasReadRight(): Boolean =
    (permissions.size ?: 0) > 0

fun TelegramUser.hasPaymentPermission(): Boolean =
    permissions.containsAny(Permissions.ADD_PAYMENT, Permissions.ADMIN) == true

fun TelegramUser.hasTimePermission(): Boolean =
    permissions.containsAny(Permissions.ADD_TIME, Permissions.ADMIN) == true

fun TelegramUser.hasClientPermission(): Boolean =
    permissions.containsAny(Permissions.ADD_CLIENT, Permissions.ADMIN) == true

fun TelegramUser.hasAdminPermission(): Boolean =
    permissions.contains(Permissions.ADMIN) == true

fun TelegramUser.hasForceGroup(): Boolean =
    permissions.contains(Permissions.FORCE_GROUP) == true