package me.centralhardware.znatoki.telegram.statistic.report

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.eav.types.NumberType
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.extensions.find
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDate
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDateTime
import me.centralhardware.znatoki.telegram.statistic.extensions.print
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ConfigMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import org.apache.poi.ss.usermodel.HorizontalAlignment
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class MonthReport(
    private val fio: String,
    private val service: Long,
    private val userId: Long,
) {

    fun generate(times: List<Service>, payments: List<Payment>): File? {
        val serviceName = ServicesMapper.getNameById(service)!!

        if (times.isEmpty() && payments.isEmpty()) {
            return null
        }

        val clients = times.associate {
            it.clientId to ClientMapper.findById(it.clientId)
        }

        val dateTime = times.first().dateTime

        val id2times: Multimap<UUID, Service> =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create())
        times.forEach { service -> id2times.put(service.id, service) }

        val fioToTimes: Multimap<Client, Service> =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create())
        times.forEach { service ->
            clients[service.clientId]?.let { client ->
                fioToTimes.put(client, service)
            }
        }

        val totalIndividual = AtomicInteger()
        val totalGroup = AtomicInteger()

        val i = AtomicInteger(1)
        val comparator: Comparator<Client> =
            getComparator(clients.values.first()!!)

        return excel {
            sheet("отчет") {
                title("Отчет по оплате и посещаемости занятий по $serviceName", 7)
                title("Преподаватель: $fio", 7)
                title(
                    "${dateTime.format(DateTimeFormatter.ofPattern("MMMM", Locale.of("ru")))} ${dateTime.year}",
                    7,
                )
                row {
                    cell("№")
                    cell("ФИО ${ConfigMapper.clientName()}")
                    clients[times.first().clientId]?.let {
                        it.properties
                            .map { it.name }
                            .filter { ConfigMapper.includeInReport().contains(it) }
                            .forEach { cell(it) }
                    }
                    cell("посетил индивидуально")
                    cell("посетил групповые")
                    cell("оплата")
                    cell("Итого")
                    cell("Даты посещений")
                }

                fioToTimes
                    .asMap()
                    .toSortedMap<Client, Collection<Service>>(comparator)
                    .forEach { (client, fioTimes) ->
                        val individual = fioTimes.count { id2times[it.id].isIndividual() }
                        val group = fioTimes.count { id2times[it.id].isGroup() }

                        totalIndividual.addAndGet(individual)
                        totalGroup.addAndGet(group)

                        val dates = linkedMapOf<String, Int>()
                        fioTimes
                            .sortedBy { it.dateTime }
                            .forEach { service ->
                                val key = service.dateTime.formatDate()
                                dates[key] = dates.getOrDefault(key, 0) + 1
                            }

                        val datesStr =
                            dates.entries.joinToString(",") { "${it.key}(${it.value})" }

                        row {
                            cell(i.getAndIncrement())
                            cell(client.fio(), HorizontalAlignment.LEFT)
                            client.properties
                                .filter { ConfigMapper.includeInReport().contains(it.name) }
                                .map { cell(it.value ?: "") }
                            cell(individual)
                            cell(group)
                            cell(
                                PaymentMapper.getPaymentsSumByClient(
                                    userId,
                                    fioTimes.first().serviceId,
                                    client.id!!,
                                    dateTime,
                                )
                            )
                            emptyCell()
                            cell(datesStr, HorizontalAlignment.LEFT)
                        }
                    }
                row {
                    emptyCell()
                    cell("Итого")
                    emptyCell()
                    cell(totalIndividual)
                    cell(totalGroup)
                    cell(
                        PaymentMapper.getPaymentsSum(
                            userId,
                            times.first().serviceId,
                            dateTime,
                        )
                    )
                }
            }
            sheet("Журнал занятий") {
                title("Журнал занятий по $serviceName", 3)
                title("Преподаватель: $fio", 3)
                title(
                    "${dateTime.format(DateTimeFormatter.ofPattern("MMMM", Locale.of("ru")))} ${dateTime.year}",
                    3,
                )
                row {
                    cell("Дата")
                    cell("ученик")
                    cell("Сумма")
                    cell("Принудительно групповое")
                }
                times
                    .sortedByDescending { it.dateTime }
                    .groupBy { it.id }
                    .forEach { id, services ->
                        val fios =
                            if (services.size == 1) {
                                "      " +
                                        clients[services.first().clientId]!!.fio()
                            } else {
                                val i = AtomicInteger(1)
                                services.joinToString("\n") {
                                    "${i.getAndAdd(1)} - ${clients[it.clientId]!!.fio()}"
                                }
                            }
                        row {
                            cell(services.first().dateTime.formatDateTime())
                            cell(fios, HorizontalAlignment.LEFT)
                            cell(services.first().amount)
                            if (services.size == 1) {
                                cell(services.first().forceGroup.print())
                            }
                        }
                    }
            }
            sheet("Журнал оплаты") {
                title("Журнал оплаты по $serviceName", 4)
                title("Преподаватель: $fio", 4)
                title(
                    "${dateTime.format(DateTimeFormatter.ofPattern("MMMM", Locale.of("ru")))} ${dateTime.year}",
                    4,
                )
                row {
                    cell("Дата")
                    cell("ученик")
                    cell("Сумма")
                    cell("Предмет")
                    cell("фото")
                }
                payments.forEach { payment ->
                    row {
                        cell(payment.dateTime.formatDateTime())
                        cell(clients[payment.clientId]!!.fio(), HorizontalAlignment.LEFT)
                        cell(payment.amount)
                        cell(ServicesMapper.getNameById(payment.serviceId))
                        MinioService.getLink(payment.properties.find("фото отчетности").value!!).onSuccess {
                            cellHyperlink(it.replace("http://10.168.0.34:9000", Config.Minio.proxyUrl), "отчет")
                        }
                    }
                }

                title("Примечание: ссылки на отчеты действительны в течение одного часа", 4)

            }
        }
            .build("$fio - $serviceName ${dateTime.format(DateTimeFormatter.ofPattern("MMMM"))} ${dateTime.year}.xlsx")
    }

    private fun getComparator(client: Client): Comparator<Client> {
        val props = client.properties.filter { ConfigMapper.includeInReport().contains(it.name) }

        var comparator: Comparator<Client> = createComparatorFunction(props.first())

        props.drop(1).forEach { property ->
            comparator = comparator.thenComparing(createComparatorFunction(property))
        }

        return comparator.thenComparing { it.fio() }
    }

    private fun createComparatorFunction(property: Property): Comparator<Client> {
        return if (property.type is NumberType) {
            Comparator.comparing<Client, Int?>(
                { client -> getProperty(client, property.name)?.value?.toIntOrNull() },
                nullsLast()
            )
        } else {
            Comparator.comparing<Client, String?>(
                { client -> getProperty(client, property.name)?.value },
                nullsLast()
            )
        }
    }

    private fun getProperty(client: Client, name: String): Property? =
        client.properties.firstOrNull { it.name == name }
}
