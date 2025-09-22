package me.centralhardware.znatoki.telegram.statistic.report

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import korlibs.time.days
import korlibs.time.hours
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDate
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDateTime
import me.centralhardware.znatoki.telegram.statistic.extensions.print
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import org.apache.poi.ss.usermodel.HorizontalAlignment
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class MonthReport(
    private val fio: String,
    private val subjectId: SubjectId,
    private val tutorId: TutorId,
) {

    val clients = mutableMapOf<StudentId, Student>()
    fun getStudent(id: StudentId): Student {
        if (!clients.contains(id)) clients[id] = StudentMapper.findById(id)

        return clients[id]!!
    }

    fun generate(times: List<Lesson>, payments: List<Payment>): File? {
        val serviceName = SubjectMapper.getNameById(subjectId)!!

        if (times.isEmpty() && payments.isEmpty()) {
            return null
        }

        val dateTime = times.first().dateTime

        val id2times: Multimap<LessonId, Lesson> =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create())
        times.forEach { lesson -> id2times.put(lesson.id, lesson) }

        val fioToTimes: Multimap<Student, Lesson> =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create())
        times.forEach { service ->
            getStudent(service.studentId).let { client ->
                fioToTimes.put(client, service)
            }
        }

        val totalIndividual = AtomicInteger()
        val totalGroup = AtomicInteger()

        val i = AtomicInteger(1)
        val comparator: Comparator<Student> =
            getComparator()

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
                    cell("ФИО ученика")
                    cell("Класс")
                    cell("посетил индивидуально")
                    cell("посетил групповые")
                    cell("оплата")
                    cell("Итого")
                    cell("Даты посещений")
                }

                fioToTimes
                    .asMap()
                    .toSortedMap(comparator)
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
                            cell(client.schoolClass ?: "")
                            cell(individual)
                            cell(group)
                            cell(
                                PaymentMapper.getPaymentsSumForStudent(
                                    tutorId,
                                    fioTimes.first().subjectId,
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
                            tutorId,
                            times.first().subjectId,
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
                    cell("Полтора часа")
                    cell("Фото")
                }
                var total = 0.0
                times
                    .sortedByDescending { it.dateTime }
                    .groupBy { it.id }
                    .forEach { id, services ->
                        val fios =
                            if (services.size == 1) {
                                "      " +
                                        getStudent(services.first().studentId).fio()
                            } else {
                                val i = AtomicInteger(1)
                                services.joinToString("\n") {
                                    "${i.getAndAdd(1)} - ${getStudent(it.studentId).fio()}"
                                }
                            }
                        total = total + services.first().amount
                        row {
                            cell(services.first().dateTime.formatDateTime())
                            cell(fios, HorizontalAlignment.LEFT)
                            cell(services.first().amount)
                            if (services.size == 1) {
                                cell(services.first().forceGroup.print())
                            } else {
                                emptyCell()
                            }
                            cell(services.first().extraHalfHour.print())
                            MinioService.getLink(services.first().photoReport!!, 7.days)
                                .onSuccess {
                                    cellHyperlink(it, "отчет")
                                }
                        }
                    }
                row {
                    cell("итого")
                    emptyCell()
                    cell(total)
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
                    cell("фото")
                }
                var total = 0
                payments.forEach { payment ->
                    total = total + payment.amount.amount
                    row {
                        cell(payment.dateTime.formatDateTime())
                        cell(getStudent(payment.studentId).fio(), HorizontalAlignment.LEFT)
                        cell(payment.amount)
                        MinioService.getLink(payment.photoReport!!, 3.hours).onSuccess {
                            cellHyperlink(it,"отчет")
                        }
                    }
                }
                row {
                    cell("итого")
                    emptyCell()
                    cell(total)
                }

                title("Примечание: ссылки на отчеты действительны в течение трех часов", 4)

            }
        }
            .build("$fio - $serviceName ${dateTime.format(DateTimeFormatter.ofPattern("MMMM"))} ${dateTime.year}.xlsx")
    }

    private fun getComparator(): Comparator<Student> {
        val comparator: Comparator<Student> = compareBy { it.schoolClass?.value }
        return comparator.thenComparing { it.fio() }
    }


}
