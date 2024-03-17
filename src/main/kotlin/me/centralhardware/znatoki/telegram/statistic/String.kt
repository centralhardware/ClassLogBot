package me.centralhardware.znatoki.telegram.statistic

fun String?.escapeHashtag(): String? = this?.replace(" ", "_")