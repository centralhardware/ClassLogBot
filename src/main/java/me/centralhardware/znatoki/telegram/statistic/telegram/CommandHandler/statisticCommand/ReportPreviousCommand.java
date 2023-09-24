package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.statisticCommand;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.service.ReportService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ReportPreviousCommand extends BaseReport {

    private final ReportService reportService;

    @Override
    public boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/reportPrevious");
    }

    @Override
    protected Function<Long, List<File>> getTime() {
        return reportService::getReportPrevious;
    }
}
