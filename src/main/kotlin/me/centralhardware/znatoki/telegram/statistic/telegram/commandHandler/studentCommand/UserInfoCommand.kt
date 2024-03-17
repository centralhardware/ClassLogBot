package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.studentCommand

import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.i18n.I18n
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.CommandHandler
import me.centralhardware.znatoki.telegram.statistic.userId
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update

/**
 * get user info by id
 * param: id of pupil
 * output format: "pupil toString"
 * input format: "/command pupil-id"
 * access level: read
 */
@Component
class UserInfoCommand(
    private val clientService: ClientService,
    private val serviceMapper: ServiceMapper,
    private val servicesMapper: ServicesMapper,
    sender: TelegramSender,
    userMapper: UserMapper
) : CommandHandler(sender, userMapper) {

    override fun handle(update: Update) {
        val arguments = update.message.text.replace("/i ", "")
        clientService.findById(arguments.toInt())?.let { client ->
            val orgId = userMapper.getById(update.userId())!!.organizationId
            if (client.organizationId != orgId) {
                sender.sendText("Доступ запрещен", update.userId())
                return
            }

            sender.sendMessageWithMarkdown(
                client.getInfo(
                    serviceMapper.getServicesForCLient(client.id!!).mapNotNull { servicesMapper.getNameById(it) }.toList()
                ), update.userId()
            )
        } ?: sender.sendMessageFromResource(I18n.Message.PUPIL_NOT_FOUND, update.userId())
    }

    override fun isAcceptable(data: String): Boolean = data.startsWith("/i")

    override fun getRequiredRole(): Role = Role.READ
}