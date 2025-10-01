package me.centralhardware.znatoki.telegram.statistic.extensions

import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor

fun Tutor?.hasReadRight(): Boolean =
    (this?.permissions?.size ?: 0) > 0

fun Tutor?.hasPaymentPermission(): Boolean =
    this?.permissions?.containsAny(Permissions.ADD_PAYMENT, Permissions.ADMIN) == true

fun Tutor?.hasTimePermission(): Boolean =
    this?.permissions?.containsAny(Permissions.ADD_TIME, Permissions.ADMIN) == true

fun Tutor?.hasClientPermission(): Boolean =
    this?.permissions?.containsAny(Permissions.ADD_CLIENT, Permissions.ADMIN) == true

fun Tutor?.hasAdminPermission(): Boolean =
    this?.permissions?.contains(Permissions.ADMIN) == true

fun Tutor?.hasForceGroup(): Boolean =
    this?.permissions?.contains(Permissions.FORCE_GROUP) == true

fun Tutor?.hasExtraHalfHour(): Boolean =
    this?.permissions?.contains(Permissions.EXTRA_HALF_HOUR) == true

fun Tutor?.canAddPaymentForOthers(): Boolean =
    this?.permissions?.containsAny(Permissions.ADD_PAYMENT_FOR_OTHERS, Permissions.ADMIN) == true