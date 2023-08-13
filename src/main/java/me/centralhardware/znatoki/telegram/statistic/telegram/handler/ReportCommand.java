package me.centralhardware.znatoki.telegram.statistic.telegram.handler;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.ReportService;
import org.springframework.stereotype.Component;

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
