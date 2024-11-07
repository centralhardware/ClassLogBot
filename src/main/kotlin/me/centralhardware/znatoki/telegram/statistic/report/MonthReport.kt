package me.centralhardware.znatoki.telegram.statistic.report

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.eav.types.NumberType
import me.centralhardware.znatoki.telegram.statistic.entity.Client
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.entity.isGroup
import me.centralhardware.znatoki.telegram.statistic.entity.isIndividual
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDate
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDateTime
import me.centralhardware.znatoki.telegram.statistic.extensions.print
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ConfigMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import org.apache.poi.ss.usermodel.HorizontalAlignment

class MonthReport(
    private val fio: String,
    private val service: Long,
    private val date: LocalDateTime,
    private val userId: Long,
) {

    fun generate(services: List<Service>): File? {
        val serviceName = ServicesMapper.getNameById(service)!!

        val filteredServices = services.filter { it.serviceId == service }
        if (filteredServices.isEmpty()) {
            return null
        }
        val dateTime = filteredServices.first().dateTime

        val id2times: Multimap<UUID, Service> =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create())
        filteredServices.forEach { service -> id2times.put(service.id, service) }

        val fioToTimes: Multimap<Client, Service> =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create())
        filteredServices.forEach { service ->
            ClientMapper.findById(service.clientId)?.let { client ->
                fioToTimes.put(client, service)
            }
        }

        val totalIndividual = AtomicInteger()
        val totalGroup = AtomicInteger()

        val i = AtomicInteger(1)
        val comparator: Comparator<Client> =
            getComparator(ClientMapper.findById(filteredServices.first().clientId)!!)

        return excel(fio, serviceName, date) {
                sheet("отчет") {
                    title("Отчет по оплате и посещаемости занятий по $serviceName", 6)
                    title("Преподаватель: $fio", 6)
                    title(
                        "${dateTime.format(DateTimeFormatter.ofPattern("MMMM", Locale.of("ru")))} ${dateTime.year}",
                        6,
                    )
                    row {
                        cell("№")
                        cell("ФИО ${ConfigMapper.clientName()}")
                        ClientMapper.findById(filteredServices.first().clientId)?.let {
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
                                        date,
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
                                filteredServices.first().serviceId,
                                date,
                            )
                        )
                    }
                }
                sheet("Журнал") {
                    title("Журнал занятий по $serviceName", 3)
                    title("Преподаватель: $fio", 3)
                    title(
                        "${dateTime.format(DateTimeFormatter.ofPattern("MMMM", Locale.of("ru")))} ${dateTime.year}",
                        3,
                    )
                    row {
                        cell("ученик")
                        cell("Дата")
                        cell("Сумма")
                        cell("Принудительно групповое")
                    }
                    filteredServices
                        .sortedByDescending { it.dateTime }
                        .groupBy { it.id }
                        .forEach { id, services ->
                            val fios =
                                if (services.size == 1) {
                                    ClientMapper.findById(services.first().clientId)!!.fio()
                                } else {
                                    val i = AtomicInteger(1)
                                    services
                                        .map {
                                            "${i.getAndAdd(1)} - ${ClientMapper.findById(it.clientId)!!.fio()}"
                                        }
                                        .joinToString("\n")
                                }
                            row {
                                cell(fios, HorizontalAlignment.LEFT)
                                cell(services.first().dateTime.formatDateTime())
                                cell(services.first().amount)
                                if (services.size == 1) {
                                    cell(services.first().forceGroup.print())
                                }
                            }
                        }
                }
            }
            .build()
    }

    private fun getComparator(client: Client): Comparator<Client> {
        val props = client.properties.filter { ConfigMapper.includeInReport().contains(it.name) }

        val comparator: Comparator<Client> =
            props.first().let { property ->
                if (property.type is NumberType) {
                    compareBy(nullsLast()) {
                        val propValue = getProperty(it, property.name)?.value
                        if (!propValue.isNullOrBlank()) propValue.toInt() else null
                    }
                } else {
                    compareBy(nullsLast()) { getProperty(it, property.name)?.value }
                }
            }

        props.drop(1).forEach { property ->
            if (property.type is NumberType) {
                comparator.thenBy(nullsLast()) {
                    val propValue = getProperty(it, property.name)?.value
                    if (!propValue.isNullOrBlank()) propValue.toInt() else null
                }
            } else {
                comparator.thenBy(nullsLast()) { getProperty(it, property.name)?.value }
            }
        }

        return comparator.thenBy { it.fio() }
    }

    private fun getProperty(client: Client, name: String): Property? =
        client.properties.firstOrNull { it.name == name }
}
