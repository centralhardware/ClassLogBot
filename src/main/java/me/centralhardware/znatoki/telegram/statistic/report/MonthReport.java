package me.centralhardware.znatoki.telegram.statistic.report;

import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.mapper.PupilMapper;

import javax.ws.rs.core.MultivaluedHashMap;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MonthReport extends ExcelReport{

    private final PupilMapper pupilMapper;

    public MonthReport(String fio, PupilMapper pupilMapper, Subject subject, LocalDateTime date) {
        super(fio, subject, date);

        this.pupilMapper = pupilMapper;
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");

    public File generate(List<Time> times){
        times = times
                .stream()
                .filter(it -> Subject.valueOf(it.getSubject()).equals(this.subject))
                .toList();
        if (times.isEmpty()) return null;

        newSheet("отчет");

        writeTitle("Отчет по оплате и посещаемости занятий по " + subject.getRusName(), 9);
        writeTitle("Преподаватель: " + fio, 9);

        LocalDateTime dateTime = times.get(0).getDateTime();
        writeTitle(dateTime.format(DateTimeFormatter.ofPattern("MMMM")) + " " +
                dateTime.getYear(), 9);

        writeRow(
                "№",
                "ФИО ученика",
                "класс",
                "посетил индивидуально",
                "посетил групповые",
                "дата",
                "количество",
                "оплата",
                "Итого",
                "дополнительные сведения"
        );

        var fioToTimes = new MultivaluedHashMap<String, Time>();
        times.forEach(it -> fioToTimes.add(it.getFio(), it));

        AtomicInteger totalIndividual = new AtomicInteger();
        AtomicInteger totalGroup = new AtomicInteger();

        AtomicInteger i = new AtomicInteger(1);
        fioToTimes.forEach((fio, fioTimes) -> {
            int individual = (int) fioTimes
                    .stream()
                    .filter(it -> it.getFios().size() == 1)
                    .count();
            int group = (int) fioTimes
                    .stream()
                    .filter(it -> it.getFios().size() != 1)
                    .count();
            totalIndividual.addAndGet(individual);
            totalGroup.addAndGet(group);

            LinkedHashMap<String, Integer> dates = new LinkedHashMap<>();
            fioTimes.stream().sorted(Comparator.comparing(Time::getDateTime)).forEach(it -> {
                int timeCount =  dates.computeIfAbsent(formatter.format(it.getDateTime()), count -> 0);
                timeCount++;
                dates.put(formatter.format(it.getDateTime()), timeCount);
            });;


            writeRow(
                    String.valueOf(i.getAndIncrement()),
                    fio,
                    pupilMapper.getClass(fio).toString(),
                    Integer.toString(individual),
                    Integer.toString(group),
                    dates.keySet().stream().findFirst().get(),
                    dates.values().stream().findFirst().get().toString(),
                    ""
            );

            dates.remove(dates.keySet().stream().findFirst().get());

            dates.forEach((date, count) ->{
                writeRow(
                        "","", "","","",
                        date,
                        count.toString()
                );
            });
        });

        writeRow(
                "",
                "Итого",
                "",
                totalIndividual.toString(),
                totalGroup.toString()
        );

        return create();
    }

}
