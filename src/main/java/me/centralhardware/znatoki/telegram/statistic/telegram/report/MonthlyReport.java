package me.centralhardware.znatoki.telegram.statistic.telegram.report;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.service.ReportService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;

import java.io.File;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MonthlyReport {

    private final ReportService reportService;
    private final TimeMapper timeMapper;
    private final TelegramSender sender;
    private final OrganizationMapper organizationMapper;

    @Scheduled(cron = "0 0 10 1 * *")
    public void report() {
        organizationMapper.getOwners()
                .forEach(org -> timeMapper.getIds(org.getId())
                            .stream()
                            .map(id -> {
                                List<File> files = reportService.getReportPrevious(id);

                                files.forEach(file -> {
                                    SendDocument sendDocument = SendDocument
                                            .builder()
                                            .chatId(id)
                                            .document(new InputFile(file))
                                            .build();
                                    sender.send(sendDocument, getUser(id));
                                });

                                return files;
                            })
                            .flatMap(Collection::stream)
                            .forEach(report -> {
                                SendDocument sendDocument = SendDocument
                                        .builder()
                                        .chatId(org.getOwner())
                                        .document(new InputFile(report))
                                        .build();
                                sender.send(sendDocument, getUser(org.getOwner()));
                                //noinspection ResultOfMethodCallIgnored
                                report.delete();
                            })
                );

    }

    public User getUser(Long chatId) {
        var user = new User();
        user.setId(chatId);
        user.setLanguageCode("ru");
        return user;
    }

}
