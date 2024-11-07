package me.centralhardware.znatoki.telegram.statistic.extensions

fun String?.hashtag(): String? = this?.let { "#" + replace(" ", "_") }
