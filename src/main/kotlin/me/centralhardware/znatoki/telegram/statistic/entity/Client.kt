package me.centralhardware.znatoki.telegram.statistic.entity

import jakarta.persistence.*
import me.centralhardware.znatoki.telegram.statistic.*
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.search.engine.backend.types.Projectable
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.*
import kotlin.properties.Delegates

@Entity
@Table
@Indexed
@NoArg
class Client(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    @Column(nullable = false)
    @KeywordField(name = "name", projectable = Projectable.YES)
    val name: String,
    @Column(nullable = false)
    @KeywordField(name = "secondName", projectable = Projectable.YES)
    val secondName: String,
    @Column(nullable = false)
    @KeywordField(name = "lastName", projectable = Projectable.YES)
    val lastName: String,
    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    val properties: List<Property>,
    val organizationId: UUID,
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_date")
    val createDate: LocalDateTime = LocalDateTime.now(),
    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modify_date")
    val modifyDate: LocalDateTime = LocalDateTime.now(),
    @Column(name = "created_by")
    val createdBy: Long,
    val updateBy: Long?,
    @Column(name = "deleted", columnDefinition = "boolean default false")
    var deleted: Boolean = false
){

    fun getInfo(services: List<String>) = """
        id=${id.makeBold()}
        фамилия=${secondName.makeBold()}
        имя=${name.makeBold()}
        отчество=${lastName.makeBold()}
        ${properties.print()}
        Предметы=${services.joinToString(",").makeBold()}
        Баланс=${BeanUtils.getBean(PaymentMapper::class.java).getCredit(id!!)}
        дата создания=${createDate.formatDate().makeBold()}
        дата изменения=${modifyDate.formatDate().makeBold()}
        создано=$createdBy
        редактировано=${updateBy}
    """.trimIndent()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val client = other as Client
        return id == client.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

fun Client.fio(): String = "$name $lastName $secondName".replace("\\s{2,}".toRegex(), " ")

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