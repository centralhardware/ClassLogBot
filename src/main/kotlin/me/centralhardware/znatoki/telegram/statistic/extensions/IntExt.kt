package me.centralhardware.znatoki.telegram.statistic.extensions

fun Int?.makeBold() = this.let { "*$this*" }