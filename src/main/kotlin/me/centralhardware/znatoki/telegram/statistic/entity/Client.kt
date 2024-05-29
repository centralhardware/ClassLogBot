package me.centralhardware.znatoki.telegram.statistic.entity

import kotliquery.Row
import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import java.time.LocalDateTime
import java.util.*
import kotlin.properties.Delegates

class Client(
    val id: Int? = null,
    val name: String,
    val secondName: String,
    val lastName: String,
    val properties: List<Property>,
    val organizationId: UUID,
    val createDate: LocalDateTime = LocalDateTime.now(),
    val modifyDate: LocalDateTime = LocalDateTime.now(),
    val createdBy: Long,
    val updateBy: Long?,
    var deleted: Boolean = false
) {

    fun getInfo(services: List<String>) = """
        id=${id.makeBold()}
        фамилия=${secondName.makeBold()}
        имя=${name.makeBold()}
        отчество=${lastName.makeBold()}
        ${properties.print()}
        Предметы=${services.joinToString(",").makeBold()}
        Баланс=${PaymentMapper.getCredit(id!!)}
        дата создания=${createDate.formatDate().makeBold()}
        дата изменения=${modifyDate.formatDate().makeBold()}
        создано=$createdBy
        редактировано=${updateBy}
        """.trimIndent()

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

fun Client.fio(): String = "$name $lastName $secondName".replace("\\s{2,}".toRegex(), " ")

fun Row.parseClient(): Client = Client(
    int("id"),
    string("name"),
    string("second_name"),
    string("last_name"),
    string("properties").toProperties(),
    uuid("organization_id"),
    localDateTime("create_date"),
    localDateTime("modify_date"),
    long("created_by"),
    longOrNull("update_by"),
    boolean("deleted")
)

class ClientBuilder: Builder{
    lateinit var  name: String
    lateinit var  secondName: String
    lateinit var  lastName: String
    lateinit var  properties: List<Property>
    lateinit var  organizationId: UUID
    var  createdBy by Delegates.notNull<Long>()
    lateinit var propertiesBuilder: PropertiesBuilder

    fun nextProperty() = propertiesBuilder.next()

    fun build(): Client = Client(
        name = name,
        secondName = secondName,
        lastName = lastName,
        properties = properties,
        organizationId = organizationId,
        createdBy = createdBy,
        updateBy = createdBy
    )

}