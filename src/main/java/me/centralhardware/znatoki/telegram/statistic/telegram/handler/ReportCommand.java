package me.centralhardware.znatoki.telegram.statistic.telegram.handler;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.ReportService;
import me.centralhardware.znatoki.telegram.statistic.mapper.TimeMapper;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.File;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ReportCommand extends BaseReport{

    private final ReportService reportService;

    @Override
    boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/report");
    }

    @Override
    protected Function<Long, List<File>> getTime() {
        return reportService::getReportsCurrent;
    }
}
