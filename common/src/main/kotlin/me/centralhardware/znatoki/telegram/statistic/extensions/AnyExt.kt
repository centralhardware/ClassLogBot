package me.centralhardware.znatoki.telegram.statistic.extensions

fun Any?.makeBold() = this.let { "*$this*" }
fun Any?.hashtag() = this?.toString()?.let { "#" + it.replace(" ", "_") }