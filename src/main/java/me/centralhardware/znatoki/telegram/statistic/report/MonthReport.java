package me.centralhardware.znatoki.telegram.statistic.report;

import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;

import java.io.File;
import java.util.List;
import java.util.function.Function;

public class MonthReport extends ExcelReport{

    public File generate(List<Time> times, Function<Long, String> getFio){
        newSheet("лог");

        writeRow("дата", "преподаватель", "ученики", "стоимость за одного", "суммарная стоимость", "предмет");
        times.forEach(time -> {
            writeRow(
                    time.getDateTime().toString(),
                    getFio.apply(time.getChatId()),
                    String.join(", ", time.getFios()),
                    time.getAmount().toString(),
                    String.valueOf(time.getAmount() * time.getFios().size()),
                    Subject.valueOf(time.getSubject()).getRusName()
            );
        });
        return create();
    }

}
