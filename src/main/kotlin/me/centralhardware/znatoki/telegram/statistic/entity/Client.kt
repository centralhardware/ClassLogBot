package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row
import me.centralhardware.znatoki.telegram.statistic.extensions.formatDate
import me.centralhardware.znatoki.telegram.statistic.extensions.makeBold
import java.time.LocalDate
import java.time.LocalDateTime

class Client(
    var id: Int? = null,
    val name: String,
    val secondName: String,
    val lastName: String,
    val klass: Int? = null,
    val recordDate: LocalDate? = null,
    val birthDate: LocalDate? = null,
    val source: String? = null,
    val phone: String? = null,
    val responsiblePhone: String? = null,
    val motherFio: String? = null,
    val createDate: LocalDateTime = LocalDateTime.now(),
    val modifyDate: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long,
    val updateBy: Long?,
    var deleted: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Client

        return id == other.id
    }

    override fun hashCode(): Int {
        return id ?: 0
    }
}

fun Client.getInfo(services: List<String>) =
    """
        id=${id.makeBold()}
        фамилия=${secondName.makeBold()}
        имя=${name.makeBold()}
        отчество=${lastName.makeBold()}
        класс=${klass?.toString()?.makeBold() ?: ""}
        дата записи=${recordDate?.formatDate()?.makeBold() ?: ""}
        дата рождения=${birthDate?.formatDate()?.makeBold() ?: ""}
        как узнал=${source?.makeBold() ?: ""}
        телефон=${phone?.makeBold() ?: ""}
        телефон ответственного=${responsiblePhone?.makeBold() ?: ""}
        ФИО матери=${motherFio?.makeBold() ?: ""}
        Предметы=${services.joinToString(",").makeBold()}
        дата создания=${createDate.formatDate().makeBold()}
        дата изменения=${modifyDate.formatDate().makeBold()}
        создано=$createdBy
        редактировано=${updateBy}
        """
        .trimIndent()

fun Client.fio(): String = "$name $secondName $lastName".replace("\\s{2,}".toRegex(), " ")

fun Row.parseClient(): Client =
    Client(
        int("id"),
        string("name"),
        string("second_name"),
        string("last_name"),
        intOrNull("klass"),
        localDateOrNull("record_date"),
        localDateOrNull("birth_date"),
        stringOrNull("source"),
        stringOrNull("phone"),
        stringOrNull("responsible_phone"),
        stringOrNull("mother_fio"),
        localDateTime("create_date"),
        localDateTime("modify_date"),
        long("created_by"),
        longOrNull("update_by"),
        boolean("deleted"),
    )

class ClientBuilder : Builder {
    var name: String? = null
    var secondName: String? = null
    var lastName: String? = null
    var createdBy: Long? = null

    var klass: Int? = null
    var recordDate: LocalDate? = null
    var birthDate: LocalDate? = null
    var source: String? = null
    var phone: String? = null
    var responsiblePhone: String? = null
    var motherFio: String? = null

    fun build(): Client =
        Client(
            name = name!!,
            secondName = secondName!!,
            lastName = lastName!!,
            klass = klass,
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
