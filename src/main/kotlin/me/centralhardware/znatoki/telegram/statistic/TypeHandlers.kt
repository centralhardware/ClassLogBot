package me.centralhardware.znatoki.telegram.statistic

fun String.parseLongList() = split(":").map { it.toLong() }
