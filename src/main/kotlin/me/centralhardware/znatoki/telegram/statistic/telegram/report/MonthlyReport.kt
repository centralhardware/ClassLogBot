package me.centralhardware.znatoki.telegram.statistic.telegram.report

import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.service.ReportService
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.objects.InputFile

@Component
class MonthlyReport(
    private val reportService: ReportService,
    private val serviceMapper: ServiceMapper,
    private val sender: TelegramSender,
    private val organizationMapper: OrganizationMapper
) {

    @Scheduled(cron = "0 0 10 1 * *")
    fun report() {
        organizationMapper.getOwners()
            .forEach { org ->
                serviceMapper.getIds(org.id).forEach {
                    reportService.getReportPrevious(it).forEach { file ->
                        sender.send(SendDocument.builder().chatId(it).document(InputFile(file)).build())
                        sender.send(SendDocument.builder().chatId(org.owner).document(InputFile(file)).build())
                        file.delete()
                    }
                }
            }
    }

}