package me.centralhardware.znatoki.telegram.statistic.report

import dev.inmo.krontab.buildSchedule
import dev.inmo.krontab.utils.asTzFlowWithDelays
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendActionUploadDocument
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.toChatId
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.ReportService

suspend fun BehaviourContext.monthReport() {
    buildSchedule("0 0 10 1 * *").asTzFlowWithDelays().collect {
        LessonMapper.getTutorIds().forEach {
            ReportService.getReportPrevious(it).forEach { file ->
                sendActionUploadDocument(it.toChatId())
                sendDocument(it.toChatId(), InputFile(file))
                TutorMapper.getAdminsId().forEach { adminId ->
                    sendActionUploadDocument(adminId.toChatId())
                    sendDocument(adminId.toChatId(), InputFile(file))
                }
                file.delete()
            }
        }
    }
}
