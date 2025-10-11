package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDate
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDateTime
import me.centralhardware.znatoki.telegram.statistic.extensions.formatTelephone
import me.centralhardware.znatoki.telegram.statistic.extensions.makeBold
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.regex.Pattern

/**
 * Represents a student ID.
 * Uses the None pattern to handle uninitialized/unsaved students safely.
 */
@JvmInline
value class StudentId private constructor(private val _id: Int?) {
    companion object {
        /** Represents a student that hasn't been persisted yet */
        val None = StudentId(null)

        fun of(id: Int): StudentId = StudentId(id)
    }

    val id: Int get() = _id ?: error("StudentId.None has no value")

}

fun Int.toStudentId() = StudentId.of(this)

@JvmInline
value class SchoolClass(val value: Int) {
    init {
        require(validate(value)) {
            "Invalid school class: $value"
        }
    }

    companion object {
        fun validate(value: Int?) = value in 1..11
    }

}

@JvmInline
value class PhoneNumber(val value: String) {
    init {
        require(validate(value)) {
            "Invalid phone number: $value"
        }
    }

    companion object {
        private val VALID_PHONE_NR: Pattern = Pattern.compile("^[78]\\d{10}$")

        fun validate(value: String) = VALID_PHONE_NR.matcher(value).matches()

    }

    fun format() = value.formatTelephone()

}

class Student(
    var id: StudentId,
    val name: String,
    val secondName: String,
    val lastName: String,
    val schoolClass: SchoolClass? = null,
    val recordDate: LocalDate? = null,
    val birthDate: LocalDate? = null,
    val source: SourceOption? = null,
    val phone: PhoneNumber? = null,
    val responsiblePhone: PhoneNumber? = null,
    val motherFio: String? = null,
    val createDate: LocalDateTime = LocalDateTime.now(),
    val modifyDate: LocalDateTime = LocalDateTime.now(),
    val createdBy: TutorId,
    val updateBy: TutorId?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Student

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

fun Student.getInfo(subjects: List<String>): String {
    val createdByName = TutorMapper.findByIdOrNull(createdBy)?.name ?: "Unknown (${createdBy.id})"
    val updatedByName = updateBy?.let { TutorMapper.findByIdOrNull(it)?.name ?: "Unknown (${it.id})" } ?: ""

    return """
    id=${id.id.makeBold()}
    ФИО: ${fio().makeBold()}
    класс=${schoolClass?.value.makeBold()}
    дата записи=${recordDate?.formatDate()?.makeBold() ?: ""}
    дата рождения=${birthDate?.formatDate()?.makeBold() ?: ""}
    как узнал=${source?.title.makeBold()}
    телефон=${phone?.format().makeBold()}
    телефон ответственного=${responsiblePhone?.format().makeBold()}
    ФИО матери=${motherFio?.makeBold() ?: ""}
    Предметы=${subjects.joinToString(",").makeBold()}
    дата создания=${createDate.formatDateTime().makeBold()}
    создано=${createdByName.makeBold()}
    дата изменения=${modifyDate.formatDateTime().makeBold()}
    редактировано=${updatedByName.makeBold()}
    """.trimIndent()
}

fun Student.fio(): String {
    val parts = listOf(secondName, name, lastName)
        .filter { it.isNotBlank() }

    return if (parts.isNotEmpty()) {
        parts.joinToString(" ")
    } else {
        "Неизвестно"
    }
}

fun Row.parseClient(): Student =
    Student(
        int("id").toStudentId(),
        string("name"),
        string("second_name"),
        string("last_name"),
        intOrNull("school_class")?.let { SchoolClass(it) },
        localDateOrNull("record_date"),
        localDateOrNull("birth_date"),
        stringOrNull("source")?.let { SourceOption.fromTitle(it) },
        stringOrNull("phone")?.let { PhoneNumber(it) },
        stringOrNull("responsible_phone")?.let { PhoneNumber(it) },
        stringOrNull("mother_fio"),
        localDateTime("create_date"),
        localDateTime("modify_date"),
        TutorId(long("created_by")),
        longOrNull("update_by")?.let { TutorId(it) },
    )

class ClientBuilder {
    var name: String? = null
    var secondName: String? = null
    var lastName: String? = null
    var createdBy: TutorId? = null

    var schoolClass: SchoolClass? = null
    var recordDate: LocalDate? = null
    var birthDate: LocalDate? = null
    var source: SourceOption? = null
    var phone: PhoneNumber? = null
    var responsiblePhone: PhoneNumber? = null
    var motherFio: String? = null

    fun build(): Student =
        Student(
            id = StudentId.None,
            name = name!!,
            secondName = secondName!!,
            lastName = lastName!!,
            schoolClass = schoolClass,
            recordDate = recordDate,
            birthDate = birthDate,
            source = source,
            phone = phone,
            responsiblePhone = responsiblePhone,
            motherFio = motherFio,
            createdBy = createdBy!!,
            updateBy = createdBy,
        )
}
