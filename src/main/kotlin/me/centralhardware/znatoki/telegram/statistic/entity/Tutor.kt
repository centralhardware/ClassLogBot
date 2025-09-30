package me.centralhardware.znatoki.telegram.statistic.entity

import dev.inmo.tgbotapi.types.toChatId
import kotliquery.Row

@JvmInline
value class SubjectId(val id: Long)

fun Long.toSubjectId() = SubjectId(this)

@JvmInline
value class TutorId(val id: Long) {
    fun toChatId() = id.toChatId()
}

class Tutor(
    val id: TutorId,
    val permissions: List<Permissions>,
    val subjects: List<SubjectId>,
    val name: String,
)

fun Row.parseTutor() =
    Tutor(
        TutorId(long("id")),
        array<String>("permissions").map { Permissions.valueOf(it) },
        string("services").split(":").map { SubjectId(it.toLong()) },
        string("name"),
    )
