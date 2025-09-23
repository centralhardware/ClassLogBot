package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDate
import me.centralhardware.znatoki.telegram.statistic.extensions.formatTelephone
import me.centralhardware.znatoki.telegram.statistic.extensions.makeBold
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.regex.Pattern

@JvmInline
value class StudentId(val id: Int)

fun Int.toStudentId() = StudentId(this)

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
value class PhoneNumber (val value: String) {
    init {
        require(validate(value)) {
            "Invalid phone number: $value"
        }
    }

    companion object {
        private val VALID_PHONE_NR: Pattern = Pattern.compile("^[78]\\d{10}$")

        fun validate(value: String) = VALID_PHONE_NR.matcher(value).matches()
    }

}

class Student(
    var id: StudentId? = null,
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
        return id?.id ?: 0
    }
}

fun Student.getInfo(services: List<String>) =
    """
        id=${id?.id.makeBold()}
        фамилия=${secondName.makeBold()}
        имя=${name.makeBold()}
        отчество=${lastName.makeBold()}
        класс=${schoolClass?.toString()?.makeBold() ?: ""}
        дата записи=${recordDate?.formatDate()?.makeBold() ?: ""}
        дата рождения=${birthDate?.formatDate()?.makeBold() ?: ""}
        как узнал=${source?.name.makeBold() ?: ""}
        телефон=${phone?.value.formatTelephone().makeBold()}
        телефон ответственного=${responsiblePhone?.value.formatTelephone().makeBold()}
        ФИО матери=${motherFio?.makeBold() ?: ""}
        Предметы=${services.joinToString(",").makeBold()}
        дата создания=${createDate.formatDate().makeBold()}
        дата изменения=${modifyDate.formatDate().makeBold()}
        создано=$createdBy
        редактировано=${updateBy}
        """
        .trimIndent()

fun Student.fio(): String = "$name $secondName $lastName".replace("\\s{2,}".toRegex(), " ")

fun Row.parseClient(): Student =
    Student(
        int("id").toStudentId(),
        string("name"),
        string("second_name"),
        string("last_name"),
        intOrNull("klass")?.let { SchoolClass(it) },
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
