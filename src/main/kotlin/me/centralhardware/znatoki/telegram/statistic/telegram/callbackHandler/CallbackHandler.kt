package me.centralhardware.znatoki.telegram.statistic.telegram.callbackHandler

import me.centralhardware.znatoki.telegram.statistic.entity.TelegramUser
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService
import me.centralhardware.znatoki.telegram.statistic.telegram.Handler
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import org.apache.commons.lang3.StringUtils
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User

abstract class CallbackHandler(
    val sender: TelegramSender,
    val telegramService: TelegramService,
    val userMapper: UserMapper
) : Handler {

    fun handle(callbackQuery: CallbackQuery) {
        handle(callbackQuery, callbackQuery.from, callbackQuery.data)
    }

    override fun handle(update: Update) {
        handle(update.callbackQuery)
    }

    abstract fun handle(callbackQuery: CallbackQuery, from: User, data: String)

    abstract override fun isAcceptable(data: String): Boolean

    override fun isAcceptable(update: Update): Boolean {
        if (!update.hasCallbackQuery()) return false

        val text = update.callbackQuery.data
        if (StringUtils.isBlank(text)) return false

        return isAcceptable(update.callbackQuery.data)
    }

    protected fun getTelegramUser(from: User): TelegramUser? = userMapper.getById(from.id)
}