package me.centralhardware.znatoki.telegram.statistic.telegram.handler;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.mapper.TeacherNameMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.report.MonthReport;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.File;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReportCommand extends CommandHandler{

    private final TimeMapper timeMapper;
    private final TeacherNameMapper teacherNameMapper;

    @Override
    public void handle(Message message) {
        List<File> reports = timeMapper.getIds()
                .stream()
                .map(timeMapper::getCuurentMontTimes)
                .filter(CollectionUtils::isNotEmpty)
                .map(it -> new MonthReport().generate(it, teacherNameMapper::getFio))
                .toList();

        reports
                .forEach(report -> {
                    SendDocument sendDocument = SendDocument
                            .builder()
                            .chatId(message.getChatId())
                            .document(new InputFile(report))
                            .build();
                    sender.send(sendDocument, message.getFrom());
                });
    }

    @Override
    boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/report");
    }
}
