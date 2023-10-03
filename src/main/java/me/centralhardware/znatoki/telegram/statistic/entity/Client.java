package me.centralhardware.znatoki.telegram.statistic.entity;

import com.google.common.base.Objects;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder;
import me.centralhardware.znatoki.telegram.statistic.eav.Property;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Indexed
@Table
@Slf4j
@Getter
@Setter
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    /**
     * имя
     */
    @Column(nullable = false)
    @KeywordField(name = "name", projectable = Projectable.YES)
    private String name;
    /**
     * фамилия
     */
    @Column(nullable = false)
    @KeywordField(name = "secondName", projectable = Projectable.YES)
    private String secondName;
    /**
     * отчество
     */
    @Column(nullable = false)
    @KeywordField(name = "lastName", projectable = Projectable.YES)
    private String lastName;

    @Column
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Property> properties;

    @Column
    private UUID organizationId;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_date")
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modify_date")
    private LocalDateTime modifyDate;

    @Column
    private Long created_by;
    @Column
    private Long updateBy;

    @Column(name = "deleted", columnDefinition = "boolean default false")
    private boolean deleted;

    @Transient
    private PropertiesBuilder propertiesBuilder;

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public String getInfo(List<String> services) {
        var start = STR."""
                id=\{TelegramUtil.makeBold(id)}
                фамилия=\{TelegramUtil.makeBold(secondName)}
                имя=\{TelegramUtil.makeBold(name)}
                отчество=\{TelegramUtil.makeBold(lastName)}""";
        var end = STR."""
                Предметы=\{TelegramUtil.makeBold(String.join(",", services))}
                дата создания=\{TelegramUtil.makeBold(dateFormatter.format(createDate))}
                дата изменения=\{TelegramUtil.makeBold(dateFormatter.format(modifyDate))}
                создано=\{created_by}
                редактировано=\{updateBy == null? "" : updateBy}
                """;

        var customProperties = properties
                .stream()
                .map(property -> STR."\{property.name()}=\{TelegramUtil.makeBold(property.value())}" )
                .collect(Collectors.joining("\n"));

        return start + "\n" + customProperties + "\n" + end;
    }

    public String getFio(){
        return STR."\{name} \{lastName} \{secondName}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return Objects.equal(id, client.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
