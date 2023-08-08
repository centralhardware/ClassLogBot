package me.centralhardware.znatoki.telegram.statistic.telegram.handler;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.ReportService;
import me.centralhardware.znatoki.telegram.statistic.mapper.TimeMapper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class ReportPreviousCommand extends CommandHandler {

    private final ReportService reportService;
    private final TimeMapper timeMapper;

    @Override
    public void handle(Message message) {
        if (!message.getFrom().getId().equals(Long.parseLong(System.getenv("ADMIN_ID")))){
            return;
        }

        timeMapper.getIds()
                .forEach(it -> {
                    reportService.getReportPrevious(it)
                            .forEach(report -> {
                                SendDocument sendDocument = SendDocument
                                        .builder()
                                        .chatId(message.getChatId())
                                        .document(new InputFile(report))
                                        .build();
                                sender.send(sendDocument, message.getFrom());
                                report.delete();
                            });
                });
    }

    @Override
    boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/reportPrevious");
    }

}
