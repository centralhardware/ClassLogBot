package me.centralhardware.znatoki.telegram.statistic

fun String?.hashtag(): String? = this?.let { "#" + replace(" ", "_") }