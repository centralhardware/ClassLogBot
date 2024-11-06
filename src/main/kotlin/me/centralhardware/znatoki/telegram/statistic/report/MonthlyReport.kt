package me.centralhardware.znatoki.telegram.statistic.report

import dev.inmo.krontab.doOnceTz
import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadDocument
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.toChatId
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ReportService

suspend fun monthReport() {
    doOnceTz("0 0 10 1 * *") {
        ServiceMapper.getIds().forEach {
            ReportService.getReportPrevious(it).forEach { file ->
                runBlocking {
                    Trace.save("monthReport", mapOf("chatId" to it.toString()))
                    bot.sendActionUploadDocument(it.toChatId())
                    bot.sendDocument(it.toChatId(), InputFile(file))
                    UserMapper.getAdminsId().forEach { adminId ->
                        bot.sendActionUploadDocument(adminId.toChatId())
                        bot.sendDocument(adminId.toChatId(), InputFile(file))
                    }
                }
                file.delete()
            }
        }
    }
}
