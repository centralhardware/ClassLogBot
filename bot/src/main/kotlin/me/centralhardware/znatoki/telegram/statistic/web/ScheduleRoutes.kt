package me.centralhardware.znatoki.telegram.statistic.web

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import me.centralhardware.znatoki.telegram.statistic.entity.Amount
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

private val WEEKDAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

fun slotWhen(s: SlotDto) = s.onceDate?.let { ruDay(LocalDate.parse(it)) } ?: WEEKDAYS[s.weekday ?: 0]

fun slotTitle(s: SlotDto, subjects: Map<Long, String>) = "${slotWhen(s)} ${s.time} · ${subjects[s.subject] ?: ""}"

fun Route.scheduleRoutes() {
    requires(Access.TIME) {
        apiGet("/schedule") { c ->
            val slots = WebRepo.slots(c.id, WebRepo.studentNames())
            val m = YearMonth.now()
            call.respond(ScheduleDto(slots, WebRepo.marks(m.atDay(1), m.atEndOfMonth(), slots.map { it.id }), WebRepo.reminders(c.id)))
        }
        post("/schedule/slots") {
            val c = call.caller
            val input = call.receive<SlotInput>()
            if ((input.weekday == null) == (input.date == null)) badRequest("Укажите день недели или дату.")
            input.weekday?.let { if (it !in 0..6) badRequest("Неверный день недели.") }
            input.date?.let {
                val d = runCatching { LocalDate.parse(it) }.getOrNull() ?: badRequest("Неверная дата.")
                if (d.isBefore(LocalDate.now())) badRequest("Дата уже прошла.")
            }
            runCatching { LocalTime.parse(input.time) }.getOrNull() ?: badRequest("Укажите время.")
            if (!Amount.validate(input.amount)) badRequest("Стоимость должна быть больше нуля.")
            val subjects = WebRepo.subjects().associateBy { it.id }
            val subject = subjects[input.subject] ?: badRequest("Предмет не найден.")
            if (input.students.isEmpty()) badRequest("Выберите ученика.")
            if (input.students.size > 1 && !subject.group) badRequest("${subject.name}: только индивидуальные занятия.")
            val names = WebRepo.studentNames()
            input.students.forEach { if (names[it]?.deleted != false) badRequest("Ученик не найден.") }
            val id = WebRepo.addSlot(c.id, input)
            val slot = WebRepo.slot(id, names)!!
            AuditService.log(
                c.id, "Создано", "slot", id, slotTitle(slot, subjects.mapValues { it.value.name }),
                listOf(
                    Change("слот", null, (if (slot.onceDate != null) "разово, " else "каждую неделю, ") + "${slotWhen(slot)} ${slot.time}"),
                    Change("ученик", null, slot.students.joinToString(", ") { it.fio }),
                ),
                DataSource.WEB, input.students.first(), input.subject,
            )
            call.respond(slot)
        }
        delete("/schedule/slots/{id}") {
            val c = call.caller
            val names = WebRepo.studentNames()
            val slot = WebRepo.slot(call.parameters["id"]?.toIntOrNull() ?: notFound(), names) ?: notFound()
            if (slot.tutor != c.id && !c.isAdmin) forbidden()
            WebRepo.setSlotDeleted(slot.id, true)
            AuditService.log(
                c.id, "Удалено", "slot", slot.id, slotTitle(slot, subjectNames()),
                listOf(Change("слот", "${slotWhen(slot)} ${slot.time}", null)), DataSource.WEB, slot.students.firstOrNull()?.id, slot.subject,
            )
            call.respond(OkDto())
        }
        post("/schedule/slots/{id}/restore") {
            val c = call.caller
            val slot = WebRepo.slot(call.parameters["id"]?.toIntOrNull() ?: notFound(), WebRepo.studentNames()) ?: notFound()
            if (slot.tutor != c.id && !c.isAdmin) forbidden()
            WebRepo.setSlotDeleted(slot.id, false)
            AuditService.log(c.id, "Восстановлено", "slot", slot.id, slotTitle(slot, subjectNames()), emptyList(), DataSource.WEB, slot.students.firstOrNull()?.id, slot.subject)
            call.respond(OkDto())
        }
        /** "Не было": the slot's lesson did not happen on that day. */
        post("/schedule/slots/{id}/skip") {
            val c = call.caller
            val slot = WebRepo.slot(call.parameters["id"]?.toIntOrNull() ?: notFound(), WebRepo.studentNames()) ?: notFound()
            if (slot.tutor != c.id && !c.isAdmin) forbidden()
            val date = runCatching { LocalDate.parse(call.receive<DateInput>().date) }.getOrNull() ?: badRequest("Неверная дата.")
            WebRepo.setMark(slot.id, date, "skip", null)
            AuditService.log(
                c.id, "Не было", "slot", slot.id, slotTitle(slot, subjectNames()),
                listOf(Change(ruDay(date), "по расписанию", "отменено")), DataSource.WEB, slot.students.firstOrNull()?.id, slot.subject,
            )
            call.respond(OkDto())
        }
        post("/schedule/slots/{id}/unmark") {
            val c = call.caller
            val slot = WebRepo.slot(call.parameters["id"]?.toIntOrNull() ?: notFound(), WebRepo.studentNames()) ?: notFound()
            if (slot.tutor != c.id && !c.isAdmin) forbidden()
            val date = runCatching { LocalDate.parse(call.receive<DateInput>().date) }.getOrNull() ?: badRequest("Неверная дата.")
            WebRepo.clearMark(slot.id, date)
            call.respond(OkDto())
        }
        put("/reminders") {
            val c = call.caller
            val r = call.receive<RemindersDto>()
            if (r.at !in listOf("20:00", "21:00", "22:00")) badRequest("Время: 20:00, 21:00 или 22:00.")
            WebRepo.saveReminders(c.id, r)
            call.respond(r)
        }
    }






}
