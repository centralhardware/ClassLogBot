package me.centralhardware.znatoki.telegram.statistic.report;

import me.centralhardware.znatoki.telegram.statistic.eav.Property;
import me.centralhardware.znatoki.telegram.statistic.eav.types.Number;
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
    private final List<String> reportFields;

    public MonthReport(String fio, ClientService clientService, Long service, String serviceName, LocalDateTime date, List<String> reportFields) {
        super(fio, service, serviceName, date);

        this.clientService = clientService;
        this.reportFields = reportFields;
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

        List<String> headers = new ArrayList<>();
        headers.add("№");
        headers.add("ФИО ученика");
        clientService.findById(services.getFirst().getPupilId())
                .map(Client::getProperties)
                .orElse(Collections.emptyList())
                .stream()
                .filter(it -> reportFields.contains(it.name()))
                .map(Property::name)
                .forEach(headers::add);
        headers.add("посетил индивидуально");
        headers.add("посетил групповые");
        headers.add("оплата");
        headers.add("Итого");
        headers.add("Даты посещений");

        writeRow(headers.toArray(new String[0]));

        var fioToTimes = new MultivaluedHashMap<Client, Service>();
        services.forEach(it -> clientService.findById(it.getPupilId()).ifPresent(client -> fioToTimes.add(client, it)));

        AtomicInteger totalIndividual = new AtomicInteger();
        AtomicInteger totalGroup = new AtomicInteger();

        AtomicInteger i = new AtomicInteger(1);
        var comparator = getComparator(clientService.findById(services.getFirst().getPupilId()).get());
        fioToTimes
                .entrySet()
                .stream()
                .sorted(comparator)
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

                    LinkedHashMap<String, java.lang.Integer> dates = new LinkedHashMap<>();
                    fioTimes.stream().sorted(Comparator.comparing(Service::getDateTime)).forEach(time -> {
                        int timeCount = dates.computeIfAbsent(formatter.format(time.getDateTime()), count -> 0);
                        timeCount++;
                        dates.put(formatter.format(time.getDateTime()), timeCount);
                    });

                    var datesStr = dates.entrySet().stream()
                            .map(entry -> String.format("%s(%s)", entry.getKey(), entry.getValue()))
                            .collect(Collectors.joining(","));

                    List<String> data = new ArrayList<>();
                    data.add(String.valueOf(i.getAndIncrement()));
                    data.add(client.getFio());
                    client.getProperties()
                            .stream()
                            .filter(prop -> reportFields.contains(prop.name()))
                            .map(Property::value)
                            .forEach(data::add);
                    data.add(Integer.toString(individual));
                    data.add(Integer.toString(group));
                    data.add("");
                    data.add("");
                    data.add(datesStr);

                    writeRow(data.toArray(new String[0]));
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

    private Comparator<Map.Entry<Client, List<Service>>> getComparator(Client client){
        Comparator<Map.Entry<Client, List<Service>>> comparator = null;
        var props = client.getProperties()
                .stream()
                .filter(it -> reportFields.contains(it.name()))
                .toList();
        for (var p : props){
            Object val = p.type() instanceof Number ? java.lang.Integer.parseInt(p.value()) : p.value();
            if (comparator == null){
                if (p.type() instanceof Number){
                    comparator = Comparator.comparing(it -> java.lang.Integer.parseInt(getProperty(it.getKey(), p.name()).value()), Comparator.nullsLast(Comparator.naturalOrder()));
                } else {
                    comparator = Comparator.comparing(it -> getProperty(it.getKey(), p.name()).value(), Comparator.nullsLast(Comparator.naturalOrder()));
                }
            }
            if (p.type() instanceof Number){
                comparator = comparator.thenComparing(it -> java.lang.Integer.parseInt(getProperty(it.getKey(), p.name()).value()), Comparator.nullsLast(Comparator.naturalOrder()));
            } else {
                comparator = comparator.thenComparing(it -> getProperty(it.getKey(), p.name()).value(), Comparator.nullsLast(Comparator.naturalOrder()));
            }
        }
        if (comparator == null){
            comparator = Comparator.comparing(it -> it.getKey().getFio(), Comparator.nullsLast(Collator.getInstance(new Locale("ru", "RU"))));
        } else {
            comparator = comparator.thenComparing(it -> it.getKey().getFio(), Comparator.nullsLast(Collator.getInstance(new Locale("ru", "RU"))));
        }
        return comparator;
    }

    private Property getProperty(Client client, String name){
        return client.getProperties()
                .stream()
                .filter(it -> it.name().equals(name))
                .findFirst()
                .orElse(null);
    }

}
