package me.centralhardware.znatoki.telegram.statistic.telegram.commandHandler.statisticCommand

import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ReportService
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import org.springframework.stereotype.Component
import java.io.File

@Component
class ReportPreviousCommand(
    private val reportService: ReportService, serviceMapper: ServiceMapper, storage: Storage,
    telegramService: TelegramService, sender: TelegramSender, userMapper: UserMapper
) : BaseReport(
    serviceMapper, storage, telegramService, sender, userMapper

) {

    override fun isAcceptable(data: String): Boolean = data.equals("/reportPrevious", ignoreCase = true)

    override fun getTime(): (Long) -> List<File> = reportService::getReportPrevious
}