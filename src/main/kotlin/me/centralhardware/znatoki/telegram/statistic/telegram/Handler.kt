package me.centralhardware.znatoki.telegram.statistic.telegram

import org.telegram.telegrambots.meta.api.objects.Update

interface Handler {
    fun handle(update: Update)

    fun isAcceptable(data: String): Boolean

    fun isAcceptable(update: Update): Boolean
}