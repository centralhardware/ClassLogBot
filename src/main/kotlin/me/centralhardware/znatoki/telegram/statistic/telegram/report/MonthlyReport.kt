package me.centralhardware.znatoki.telegram.statistic.telegram.report

import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadDocument
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.service.ReportService

class MonthlyReport{

//    @Scheduled(cron = "0 0 10 1 * *")
    fun report() {
        OrganizationMapper.getOwners()
            .forEach { org ->
                ServiceMapper.getIds(org.id).forEach {
                    ReportService.getReportPrevious(it).forEach { file ->
                        runBlocking {
                            bot.sendActionUploadDocument(it.toChatId())
                            bot.sendDocument(it.toChatId(), InputFile(file))
                            bot.sendActionUploadDocument(org.owner.toChatId())
                            bot.sendDocument(org.owner.toChatId(), InputFile(file))
                        }
                        file.delete()
                    }
                }
            }
    }

}