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
    /** Share of the billed amount paid to the tutor, in percent; null when not set. */
    val rate: Int? = null,
)

fun Row.parseTutor() =
    Tutor(
        TutorId(long("id")),
        array<String>("permissions").mapNotNull { name -> Permissions.entries.find { it.name == name } },
        (stringOrNull("subjects") ?: "").split(":").filter { it.isNotBlank() }.map { SubjectId(it.toLong()) },
        string("name"),
        intOrNull("rate"),
    )
