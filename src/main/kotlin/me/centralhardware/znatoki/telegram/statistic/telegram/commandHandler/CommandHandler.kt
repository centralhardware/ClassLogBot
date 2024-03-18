package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler

import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.Handler
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.userId
import org.apache.commons.lang3.StringUtils
import org.telegram.telegrambots.meta.api.objects.Update

abstract class CommandHandler(
    val sender: TelegramSender,
    val userMapper: UserMapper
) : Handler {

    abstract override fun isAcceptable(data: String): Boolean

    /**
     * @return True, if giving callbackQuery can be processed by this handler
     */
    override fun isAcceptable(update: Update): Boolean {
        if (!update.hasMessage()) return false

        val text = update.message.text
        if (StringUtils.isBlank(text)) return false

        return isAcceptable(update.message.text) && checkAuth(update)
    }

    abstract fun getRequiredRole(): Role?

    private fun checkAuth(update: Update): Boolean {
        val id = update.userId()
        val user = userMapper.getById(id)

        val requiredRole = getRequiredRole() ?: return true

        if (user == null || user.role == Role.BLOCK) {
            sendAccessDenied(update)
            return false
        }

        return when (requiredRole) {
            Role.ADMIN -> {
                if (user.role != Role.ADMIN) {
                    sendAccessDenied(update)
                    false
                } else true
            }

            Role.READ_WRITE -> {
                if (user.role != Role.ADMIN && user.role != Role.READ_WRITE) {
                    sendAccessDenied(update)
                    false
                } else true
            }

            else -> true
        }
    }

    private fun sendAccessDenied(update: Update) {
        sender.sendText("Недостаточно прав", update.userId())
    }
}