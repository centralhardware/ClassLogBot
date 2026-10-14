package me.centralhardware.znatoki.telegram.statistic.web.service

import me.centralhardware.znatoki.telegram.statistic.entity.Amount
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change
import me.centralhardware.znatoki.telegram.statistic.web.*
import me.centralhardware.znatoki.telegram.statistic.web.http.badRequest
import me.centralhardware.znatoki.telegram.statistic.web.http.forbidden
import me.centralhardware.znatoki.telegram.statistic.web.http.notFound
import java.time.LocalDate
import java.time.YearMonth

/** The weekly schedule: slots, what happened to them each day, and the evening reminders. */
object ScheduleService {

    fun get(c: Caller): ScheduleDto {
        val slots = WebRepo.slots(c.id, WebRepo.studentNames())
        val m = YearMonth.now()
        return ScheduleDto(slots, WebRepo.marks(m.atDay(1), m.atEndOfMonth(), slots.map { it.id }), WebRepo.reminders(c.id))
    }

    fun add(c: Caller, input: SlotInput): SlotDto {
        validate(input)
        val slot = WebRepo.slot(WebRepo.addSlot(c.id, input), WebRepo.studentNames())!!
        log(c, "Создано", slot, listOf(
            Change("слот", null, (if (slot.onceDate != null) "разово, " else "каждую неделю, ") + "${slotWhen(slot)} ${slot.time}"),
            Change("ученик", null, slot.students.joinToString(", ") { it.fio }),
        ))
        return slot
    }

    fun setDeleted(c: Caller, id: Int, deleted: Boolean): OkDto {
        val slot = own(c, id)
        WebRepo.setSlotDeleted(slot.id, deleted)
        log(c, if (deleted) "Удалено" else "Восстановлено", slot, if (deleted) listOf(Change("слот", "${slotWhen(slot)} ${slot.time}", null)) else emptyList())
        return OkDto()
    }

    /** "Не было": the slot's lesson did not happen on that day. */
    fun skip(c: Caller, id: Int, date: String): OkDto {
        val slot = own(c, id)
        val day = parseDate(date)
        WebRepo.setMark(slot.id, day, "skip", null)
        log(c, "Не было", slot, listOf(Change(ruDay(day), "по расписанию", "отменено")))
        return OkDto()
    }

    fun unmark(c: Caller, id: Int, date: String): OkDto {
        WebRepo.clearMark(own(c, id).id, parseDate(date))
        return OkDto()
    }

    fun saveReminders(c: Caller, r: RemindersDto): RemindersDto {
        if (r.at !in listOf("20:00", "21:00", "22:00")) badRequest("Время: 20:00, 21:00 или 22:00.")
        WebRepo.saveReminders(c.id, r)
        return r
    }

    private fun validate(input: SlotInput) {
        if ((input.weekday == null) == (input.date == null)) badRequest("Укажите день недели или дату.")
        input.weekday?.let { if (it !in 0..6) badRequest("Неверный день недели.") }
        input.date?.let { if (parseDate(it).isBefore(LocalDate.now())) badRequest("Дата уже прошла.") }
        parseTime(input.time) ?: badRequest("Укажите время.")
        if (!Amount.validate(input.amount)) badRequest("Стоимость должна быть больше нуля.")
        val subject = WebRepo.subjects().find { it.id == input.subject } ?: badRequest("Предмет не найден.")
        if (input.students.isEmpty()) badRequest("Выберите ученика.")
        if (input.students.size > 1 && !subject.group) badRequest("${subject.name}: только индивидуальные занятия.")
        val names = WebRepo.studentNames()
        input.students.forEach { if (names[it]?.deleted != false) badRequest("Ученик не найден.") }
    }

    /** A slot the caller may change: their own, or anyone's for an admin. */
    private fun own(c: Caller, id: Int): SlotDto =
        (WebRepo.slot(id, WebRepo.studentNames()) ?: notFound()).also { if (it.tutor != c.id && !c.isAdmin) forbidden() }

    private fun log(c: Caller, action: String, slot: SlotDto, changes: List<Change>) =
        AuditService.log(c.id, action, "slot", slot.id, slotTitle(slot), changes, DataSource.WEB, slot.students.firstOrNull()?.id, slot.subject)

    fun slotWhen(s: SlotDto) = s.onceDate?.let { ruDay(LocalDate.parse(it)) } ?: weekdayShort(s.weekday ?: 0)

    fun slotTitle(s: SlotDto) = "${slotWhen(s)} ${s.time} · ${subjectNames()[s.subject] ?: ""}"
}
