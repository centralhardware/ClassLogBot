package me.centralhardware.znatoki.telegram.statistic.report;

import me.centralhardware.znatoki.telegram.statistic.eav.Property;
import me.centralhardware.znatoki.telegram.statistic.eav.types.Number;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Service;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Client;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.PaymentMapper;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.utils.BeanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.MultivaluedHashMap;
import java.io.File;
import java.text.Collator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MonthReport extends ExcelReport{

    private final List<String> reportFields;
    private final String clientName;
    private final Long userId;

    public MonthReport(String fio,
                       Long userId,
                       Long service,
                       String serviceName,
                       LocalDateTime date,
                       List<String> reportFields,
                       String clientName) {
        super(fio, service, serviceName, date);

        this.reportFields = reportFields;
        this.clientName = clientName;
        this.userId = userId;
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");

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
        headers.add(STR."ФИО \{clientName}");
        BeanUtils.getBean(ClientService.class).findById(services.getFirst().getPupilId())
                .map(Client::getProperties)
                .orElse(Collections.emptyList())
                .stream()
                .map(Property::name)
                .filter(reportFields::contains)
                .forEach(headers::add);
        headers.add("посетил индивидуально");
        headers.add("посетил групповые");
        headers.add("оплата");
        headers.add("Итого");
        headers.add("Даты посещений");

        writeRow(headers.toArray(new String[0]));

        var fioToTimes = new MultivaluedHashMap<Client, Service>();
        services.forEach(it -> BeanUtils.getBean(ClientService.class).findById(it.getPupilId()).ifPresent(client -> fioToTimes.add(client, it)));

        AtomicInteger totalIndividual = new AtomicInteger();
        AtomicInteger totalGroup = new AtomicInteger();

        AtomicInteger i = new AtomicInteger(1);
        var comparator = getComparator(BeanUtils.getBean(ClientService.class).findById(services.getFirst().getPupilId()).get());
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
                    data.add(BeanUtils.getBean(PaymentMapper.class)
                            .getPaymentsSumByPupil(userId, client.getId(), date).toString());
                    data.add("");
                    data.add(datesStr);

                    writeRow(data.toArray(new String[0]));
                });

        writeRow(
                "",
                "Итого",
                "",
                totalIndividual.toString(),
                totalGroup.toString(),
                BeanUtils.getBean(PaymentMapper.class).getPaymentsSum(userId, date).toString()
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
            if (comparator == null){
                if (p.type() instanceof Number){
                    comparator = Comparator.comparing(it ->
                            StringUtils.isNotBlank(getProperty(it.getKey(), p.name()).value()) ?
                                    Integer.parseInt(getProperty(it.getKey(), p.name()).value()) :
                                    null,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                } else {
                    comparator = Comparator.comparing(it -> getProperty(it.getKey(), p.name()).value(), Comparator.nullsLast(Comparator.naturalOrder()));
                }
            }
            if (p.type() instanceof Number && StringUtils.isNotBlank(p.value())){
                comparator = comparator.thenComparing(it -> Integer.parseInt(getProperty(it.getKey(), p.name()).value()), Comparator.nullsLast(Comparator.naturalOrder()));
            } else {
                comparator = comparator.thenComparing(it -> getProperty(it.getKey(), p.name()).value(), Comparator.nullsLast(Comparator.naturalOrder()));
            }
        }
        if (comparator == null){
            comparator = Comparator.comparing(it -> it.getKey().getFio(), Comparator.nullsLast(Collator.getInstance(Locale.of("ru", "RU"))));
        } else {
            comparator = comparator.thenComparing(it -> it.getKey().getFio(), Comparator.nullsLast(Collator.getInstance(Locale.of("ru", "RU"))));
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
