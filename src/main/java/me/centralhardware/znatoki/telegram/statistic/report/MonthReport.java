package me.centralhardware.znatoki.telegram.statistic.report;

import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;

import javax.ws.rs.core.MultivaluedHashMap;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MonthReport extends ExcelReport{

    private final PupilService pupilService;

    public MonthReport(String fio, PupilService pupilService, Subject subject, LocalDateTime date) {
        super(fio, subject, date);

        this.pupilService = pupilService;
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");

    public File generate(List<Time> times){
        times = times
                .stream()
                .filter(it -> Subject.valueOf(it.getSubject()).equals(this.subject))
                .toList();
        if (times.isEmpty()) return null;

        newSheet("отчет");

        writeTitle(STR."Отчет по оплате и посещаемости занятий по \{subject.getRusName()}", 6);
        writeTitle("Преподаватель: " + fio, 6);

        LocalDateTime dateTime = times.get(0).getDateTime();
        writeTitle(dateTime.format(DateTimeFormatter.ofPattern("MMMM")) + " " +
                dateTime.getYear(), 6);

        writeRow(
                "№",
                "ФИО ученика",
                "класс",
                "посетил индивидуально",
                "посетил групповые",
                "оплата",
                "Итого",
                "Даты посещений"
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
            });

            var datesStr = dates.entrySet().stream()
                    .map(entry -> String.format("%s(%s)", entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining(","));

            writeRow(
                    String.valueOf(i.getAndIncrement()),
                    fio,
                    Objects.toString(pupilService.findByFio(fio).getClassNumber()),
                    Integer.toString(individual),
                    Integer.toString(group),
                    "",
                    "",
                    datesStr
            );
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
