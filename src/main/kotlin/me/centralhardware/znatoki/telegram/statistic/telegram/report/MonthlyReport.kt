package me.centralhardware.znatoki.telegram.statistic.telegram.report

import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadDocument
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadPhoto
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.service.ReportService
import me.centralhardware.znatoki.telegram.statistic.toId

class MonthlyReport{

//    @Scheduled(cron = "0 0 10 1 * *")
    fun report() {
        OrganizationMapper.getOwners()
            .forEach { org ->
                ServiceMapper.getIds(org.id).forEach {
                    ReportService.getReportPrevious(it).forEach { file ->
                        runBlocking {
                            bot.sendActionUploadDocument(it.toId())
                            bot.sendDocument(it.toId(), InputFile(file))
                            bot.sendActionUploadDocument(org.owner.toId())
                            bot.sendDocument(org.owner.toId(), InputFile(file))
                        }
                        file.delete()
                    }
                }
            }
    }

}