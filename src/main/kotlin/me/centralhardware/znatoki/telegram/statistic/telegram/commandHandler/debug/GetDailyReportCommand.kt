package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.debug

import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.from
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.CommandHandler
import me.centralhardware.znatoki.telegram.statistic.telegram.report.DailyReport
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class GetDailyReportCommand(val dailyReport: DailyReport,
                            sender: TelegramSender,
                            userMapper: UserMapper): CommandHandler(sender, userMapper) {

    override fun handle(update: Update) {
        var id = update.message.text.replace("/dailyReport ", "").toLong()
        dailyReport.getReport(id, update.from().id)
    }

    override fun isAcceptable(data: String): Boolean = data.startsWith("/dailyReport")

    override fun getRequiredRole(): Role? = Role.ADMIN

}