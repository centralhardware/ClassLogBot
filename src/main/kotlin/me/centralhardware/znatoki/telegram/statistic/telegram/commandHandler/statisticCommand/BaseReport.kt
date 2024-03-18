package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.CommandHandler
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.userId
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Update
import java.io.File

@Component
abstract class BaseReport(
    private val serviceMapper: ServiceMapper,
    private val storage: Storage,
    private val telegramService: TelegramService,
    sender: TelegramSender,
    userMapper: UserMapper
) : CommandHandler(sender, userMapper) {

    override fun handle(update: Update) {
        val id = update.userId()

        if (storage.contain(id)) { return }

        if (userMapper.getById(id) != null && !telegramService.isAdmin(id)) {
            getTime().invoke(id).forEach{
                send(it, update.userId())
            }
            return
        } else if (telegramService.isAdmin(id)){
            serviceMapper.getIds(userMapper.getById(id)!!.organizationId).forEach {
                getTime().invoke(it).forEach { send(it, update.userId()) }
            }
        }

    }

    override fun getRequiredRole(): Role = Role.READ

    protected abstract fun getTime(): (Long) -> List<File>


    private fun send(file: File, chatId: Long) {
        val sendDocument = SendDocument.builder()
            .chatId(chatId)
            .document(InputFile(file))
            .build()
        sender.send{ execute(sendDocument) }
        file.delete()
    }

}