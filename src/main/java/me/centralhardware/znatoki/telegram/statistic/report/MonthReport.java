package me.centralhardware.znatoki.telegram.statistic.report;

import me.centralhardware.znatoki.telegram.statistic.entity.Service;
import me.centralhardware.znatoki.telegram.statistic.entity.Client;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;

import javax.ws.rs.core.MultivaluedHashMap;
import java.io.File;
import java.text.Collator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MonthReport extends ExcelReport{

    private final ClientService clientService;

    public MonthReport(String fio, ClientService clientService, Long service, String serviceName, LocalDateTime date) {
        super(fio, service, serviceName, date);

        this.clientService = clientService;
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");;

    public File generate(List<Service> services){
        services = services
                .stream()
                .filter(it -> it.getServiceId().equals(this.service))
                .toList();
        if (services.isEmpty()) return null;

        newSheet("отчет");

        writeTitle(STR."Отчет по оплате и посещаемости занятий по \{serviceName}", 6);
        writeTitle("Преподаватель: " + fio, 6);

        LocalDateTime dateTime = services.get(0).getDateTime();
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

        var fioToTimes = new MultivaluedHashMap<Client, Service>();
        services.forEach(it -> clientService.findById(it.getPupilId()).ifPresent(client -> fioToTimes.add(client, it)));

        AtomicInteger totalIndividual = new AtomicInteger();
        AtomicInteger totalGroup = new AtomicInteger();

//        Comparator<Map.Entry<Client, ?>> comparator = Comparator.comparing(it -> it.getKey().getClassNumber(), Comparator.nullsLast(Comparator.naturalOrder()));
//        comparator = comparator.thenComparing(it -> it.getKey().getFio(), Comparator.nullsLast(Collator.getInstance(new Locale("ru", "RU"))));
        AtomicInteger i = new AtomicInteger(1);
        fioToTimes
                .entrySet()
                .stream()
//                .sorted(comparator)
                .forEach(it -> {
                    var fioTimes = it.getValue();
                    var client = it.getKey();
                    int individual = (int) fioTimes
                            .stream()
                            .filter(time -> time.getServiceIds().size() == 1)
                            .count();
                    int group = (int) fioTimes
                            .stream()
                            .filter(time -> time.getServiceIds().size() != 1)
                            .count();
                    totalIndividual.addAndGet(individual);
                    totalGroup.addAndGet(group);

                    LinkedHashMap<String, Integer> dates = new LinkedHashMap<>();
                    fioTimes.stream().sorted(Comparator.comparing(Service::getDateTime)).forEach(time -> {
                        int timeCount = dates.computeIfAbsent(formatter.format(time.getDateTime()), count -> 0);
                        timeCount++;
                        dates.put(formatter.format(time.getDateTime()), timeCount);
                    });

                    var datesStr = dates.entrySet().stream()
                            .map(entry -> String.format("%s(%s)", entry.getKey(), entry.getValue()))
                            .collect(Collectors.joining(","));

                    writeRow(
                            String.valueOf(i.getAndIncrement()),
                            client.getFio(),
                            "",
//                            Objects.toString(client.getClassNumber()),
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
