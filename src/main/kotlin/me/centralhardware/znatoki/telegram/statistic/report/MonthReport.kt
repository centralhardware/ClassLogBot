package me.centralhardware.znatoki.telegram.statistic.report

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import me.centralhardware.znatoki.telegram.statistic.BeanUtils
import me.centralhardware.znatoki.telegram.statistic.clientService
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.eav.types.NumberType
import me.centralhardware.znatoki.telegram.statistic.entity.Client
import me.centralhardware.znatoki.telegram.statistic.entity.Service
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.formatDate
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator

class MonthReport(
    private val fio: String,
    private val service: Long,
    private val serviceName: String,
    private val date: LocalDateTime,
    private val reportFields: List<String>,
    private val clientName: String,
    private val userId: Long
) {

    fun generate(services: List<Service>): File? {
        val filteredServices = services.filter { it.serviceId == service }
        if (filteredServices.isEmpty()) {
            return null
        }
        val dateTime = filteredServices.first().dateTime

        val id2times: Multimap<UUID, Service> = ArrayListMultimap.create()
        filteredServices.forEach { service ->
            id2times.put(service.id, service)
        }

        val fioToTimes: Multimap<Client, Service> = ArrayListMultimap.create()
        filteredServices.forEach { service ->
            clientService().findById(service.clientId)?.let { client ->
                fioToTimes.put(client, service)
            }
        }

        val totalIndividual = AtomicInteger()
        val totalGroup = AtomicInteger()

        val i = AtomicInteger(1)
        val comparator: Comparator<Client> = getComparator(
            BeanUtils.getBean(ClientService::class.java).findById(filteredServices.first().clientId)!!
        )
        fioToTimes

        return excel(fio, serviceName, date) {
            sheet("отчет") {
                title("Отчет по оплате и посещаемости занятий по $serviceName", 6)
                title("Преподаватель: $fio", 6)
                title("${dateTime.format(DateTimeFormatter.ofPattern("MMMM"))} ${dateTime.year}", 6)
                row {
                    cell("№")
                    cell("ФИО $clientName")
                    BeanUtils.getBean(ClientService::class.java).findById(filteredServices.first().clientId)?.let {
                        it.properties
                            .map { it.name }
                            .filter { reportFields.contains(it) }
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
                    val individual = fioTimes.count { id2times[it.id].size == 1 }
                    val group = fioTimes.count { id2times[it.id].size != 1 }
                    totalIndividual.addAndGet(individual)
                    totalGroup.addAndGet(group)

                    val dates = linkedMapOf<String, Int>()
                    fioTimes.sortedBy { it.dateTime }.forEach { service ->
                        val key = service.dateTime.formatDate()
                        dates[key] = dates.getOrDefault(key, 0) + 1
                    }

                    val datesStr = dates.entries.joinToString(",") { "${it.key}(${it.value})" }

                    row {
                        cell(i.getAndIncrement())
                        cell(client.fio())
                        client.properties.filter { reportFields.contains(it.name) }.map { cell(it.value) }
                        cell(individual)
                        cell(group)
                        cell(BeanUtils.getBean(PaymentMapper::class.java)
                            .getPaymentsSumByClient(userId, fioTimes.first().serviceId, client.id!!, date))
                        emptyCell()
                        cell(datesStr)
                    }
                }
                row {
                    emptyCell()
                    cell("Итого")
                    emptyCell()
                    cell(totalIndividual)
                    cell(totalGroup)
                    cell(BeanUtils.getBean(PaymentMapper::class.java)
                        .getPaymentsSum(userId, filteredServices.first().serviceId, date))
                }

            }
        }.build()
    }

    private fun getComparator(client: Client): Comparator<Client> {
        val props = client.properties.filter { reportFields.contains(it.name) }

        var comparator: Comparator<Client> = props.first().let { property ->
            if (property.type is NumberType) {
                compareBy(nullsLast()) {
                    val propValue = getProperty(it, property.name)?.value
                    if (!propValue.isNullOrBlank()) propValue.toInt() else null
                }
            } else {
                compareBy(nullsLast()) {
                    getProperty(it, property.name)?.value
                }
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
        client.properties.firstOrNull { it.name == name}
}