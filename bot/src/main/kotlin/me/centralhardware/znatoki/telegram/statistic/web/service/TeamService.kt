package me.centralhardware.znatoki.telegram.statistic.web.service

import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.AuditService
import me.centralhardware.znatoki.telegram.statistic.service.Change
import me.centralhardware.znatoki.telegram.statistic.web.*
import me.centralhardware.znatoki.telegram.statistic.web.http.badRequest
import me.centralhardware.znatoki.telegram.statistic.web.http.forbidden
import me.centralhardware.znatoki.telegram.statistic.web.http.notFound

/** What "Вернуть доступ" gives back: the plain tutor preset. */
private val TUTOR_PRESET = listOf("ADD_TIME", "ADD_PAYMENT", "ADD_CLIENT", "FORCE_GROUP", "EXTRA_HALF_HOUR", "WEB_INTERFACE")
private val PRIVILEGED = setOf(Permissions.ADMIN.name, Permissions.MANAGE_TUTORS.name)

/** The team: tutors and staff, their permissions, subjects and pay rate; switching access off and on. */
object TeamService {

    fun list(): List<TutorDto> = TutorMapper.getAll().sortedBy { it.name }.map { it.toDto() }

    fun add(c: Caller, raw: TutorInput): TutorDto {
        val input = raw.copy(name = raw.name.trim())
        if (input.id !in 10_000L..999_999_999_999_999L) badRequest("Telegram ID — это число, например 428100231. Его можно узнать у @userinfobot.")
        if (TutorMapper.findByIdOrNull(TutorId(input.id)) != null) badRequest("Преподаватель с таким Telegram ID уже есть.")
        validate(c, null, input)
        WebRepo.insertTutor(input)
        AuditService.log(
            c.id, "Добавлен", "tutor", input.id, input.name,
            listOf(
                Change("Telegram ID", null, input.id),
                Change("права", null, input.perms.joinToString(", ")),
                Change("предметы", null, subjectList(input.subjects)),
            ),
            DataSource.WEB,
        )
        return find(input.id).toDto()
    }

    fun update(c: Caller, id: Long, raw: TutorInput): TutorDto {
        val target = find(id)
        val input = raw.copy(id = id, name = raw.name.trim())
        validate(c, target, input)
        WebRepo.updateTutor(input)
        val changes = listOf(
            Change("имя", target.name, input.name),
            Change("права", target.permissions.joinToString(", ") { it.name }, input.perms.joinToString(", ")),
            Change("предметы", subjectList(target.subjects.map { it.id }), subjectList(input.subjects)),
            Change("ставка", target.rate?.let { "$it%" }, input.rate?.let { "$it%" }),
        ).filter { it.old != it.new }
        if (changes.isNotEmpty()) AuditService.log(c.id, "Права изменены", "tutor", id, input.name, changes, DataSource.WEB)
        return find(id).toDto()
    }

    /** Takes every permission away: the bot and the web app stop answering; their records stay. */
    fun disable(c: Caller, id: Long): TutorDto {
        val target = find(id)
        if (id == c.id) badRequest("Нельзя отключить самого себя.")
        if (!c.isAdmin && Permissions.ADMIN in target.permissions) forbidden("Отключить админа может только админ.")
        WebRepo.updateTutor(TutorInput(id, target.name, emptyList(), target.subjects.map { it.id }, target.rate))
        AuditService.log(c.id, "Доступ отключён", "tutor", id, target.name, listOf(Change("права", target.permissions.joinToString(", ") { it.name }, "—")), DataSource.WEB)
        return find(id).toDto()
    }

    fun enable(c: Caller, id: Long): TutorDto {
        val target = find(id)
        WebRepo.updateTutor(TutorInput(id, target.name, TUTOR_PRESET, target.subjects.map { it.id }, target.rate))
        AuditService.log(c.id, "Доступ возвращён", "tutor", id, target.name, listOf(Change("права", "—", TUTOR_PRESET.joinToString(", "))), DataSource.WEB)
        return find(id).toDto()
    }

    private fun find(id: Long): Tutor = TutorMapper.findByIdOrNull(TutorId(id)) ?: notFound()

    /** Only admins hand out or take away ADMIN and MANAGE_TUTORS, and nobody demotes themselves. */
    private fun validate(c: Caller, target: Tutor?, input: TutorInput) {
        if (input.name.isBlank()) badRequest("Введите имя преподавателя.")
        val known = Permissions.entries.map { it.name }.toSet()
        input.perms.forEach { if (it !in known) badRequest("Неизвестное право $it.") }
        val before = target?.permissions?.map { it.name }.orEmpty().toSet()
        if (!c.isAdmin && (before.any { it in PRIVILEGED } || input.perms.any { it in PRIVILEGED })) {
            forbidden("Права ADMIN и MANAGE_TUTORS выдаёт только админ.")
        }
        if (target?.id?.id == c.id && Permissions.ADMIN.name in before && Permissions.ADMIN.name !in input.perms) {
            badRequest("Нельзя снять права администратора с самого себя.")
        }
        val teaches = input.perms.any { it == "ADD_TIME" || it == "ADD_TIME_FOR_OTHERS" }
        if (teaches && Permissions.ADMIN.name !in input.perms && Permissions.DIRECTOR.name !in input.perms && input.subjects.isEmpty()) {
            badRequest("Выберите хотя бы один предмет: без него нельзя внести занятие.")
        }
        input.rate?.let { if (it !in 0..100) badRequest("Ставка от 0 до 100%.") }
    }

    private fun subjectList(ids: List<Long>) = subjectNames().let { names -> ids.mapNotNull { names[it] }.joinToString(", ") }

    private fun Tutor.toDto() = TutorDto(id.id, name, permissions.map { it.name }, subjects.map { it.id }, rate)
}
