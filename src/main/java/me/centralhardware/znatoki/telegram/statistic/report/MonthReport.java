package me.centralhardware.znatoki.telegram.statistic.report;

import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;

import java.io.File;
import java.util.List;

public class MonthReport extends ExcelReport{

    public MonthReport(String fio) {
        super(fio);
    }

    public File generate(List<Time> times){
        newSheet("лог");

        writeRow("дата", "ученики", "стоимость за одного", "суммарная стоимость", "предмет");
        times.forEach(time -> {
            writeRow(
                    time.getDateTime().toString(),
                    String.join(", ", time.getFios()),
                    time.getAmount().toString(),
                    String.valueOf(time.getAmount() * time.getFios().size()),
                    Subject.valueOf(time.getSubject()).getRusName()
            );
        });
        return create();
    }

}
