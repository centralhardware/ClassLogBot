package me.centralhardware.znatoki.telegram.statistic.extensions

import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser

fun TelegramUser?.hasReadRight(): Boolean =
    (this?.permissions?.size ?: 0) > 0

fun TelegramUser?.hasPaymentPermission(): Boolean =
    this?.permissions?.containsAny(Permissions.ADD_PAYMENT, Permissions.ADMIN) == true

fun TelegramUser?.hasTimePermission(): Boolean =
    this?.permissions?.containsAny(Permissions.ADD_TIME, Permissions.ADMIN) == true

fun TelegramUser?.hasClientPermission(): Boolean =
    this?.permissions?.containsAny(Permissions.ADD_CLIENT, Permissions.ADMIN) == true

fun TelegramUser?.hasAdminPermission(): Boolean =
    this?.permissions?.contains(Permissions.ADMIN) == true

fun TelegramUser?.hasForceGroup(): Boolean =
    this?.permissions?.contains(Permissions.FORCE_GROUP) == true