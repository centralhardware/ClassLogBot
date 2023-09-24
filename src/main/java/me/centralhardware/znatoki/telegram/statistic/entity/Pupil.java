package me.centralhardware.znatoki.telegram.statistic.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.entity.Enum.HowToKnow;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.utils.TelephoneUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Entity
@Indexed
@Table
@Slf4j
@Getter
@Setter
public class Pupil {
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
    /**
     * 1-11
     * -1 preschool age
     */
    @Column()
    private Integer classNumber;
    @Column(nullable = false)
    private LocalDateTime dateOfRecord;
    @Column(nullable = false)
    private LocalDateTime dateOfBirth;
    /**
     * only mobile
     */
    @Column(nullable = false)
    private String telephone;
    @Column(length = 11)
    private String telephoneResponsible;
    @Enumerated(EnumType.STRING)
    private HowToKnow howToKnow;
    @Column
    private String motherName;
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

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public String getInfo(List<String> services) {
        String know             = howToKnow         == null? "" : howToKnow.toString();
        String nameMother       = motherName        == null? "" : motherName;
        String updated          = updateBy          == null? "" : updateBy.toString();

        return STR."""
                id=\{TelegramUtil.makeBold(id)}
                фамилия=\{TelegramUtil.makeBold(secondName)}
                имя=\{TelegramUtil.makeBold(name)}
                отчество=\{TelegramUtil.makeBold(lastName)}
                класс=\{TelegramUtil.makeBold(classNumber)}
                дата записи=\{TelegramUtil.makeBold(dateFormatter.format(dateOfRecord))}
                дата рождения=\{TelegramUtil.makeBold(dateFormatter.format(dateOfBirth))}
                телефон=\{TelephoneUtils.format(telephone)}
                телефон ответственного=\{TelephoneUtils.format( telephoneResponsible)}
                как узнал=\{TelegramUtil.makeBold(know)}
                Предметы=\{TelegramUtil.makeBold(String.join(",", services))}
                имя матери=\{TelegramUtil.makeBold(nameMother)}
                дата создания=\{TelegramUtil.makeBold(dateFormatter.format(createDate))}
                дата изменения=\{TelegramUtil.makeBold(dateFormatter.format(modifyDate))}
                создано=\{created_by}
                редактировано=\{updated}
                """;
    }

    public void incrementClassNumber(){
        classNumber++;
        log.info(STR."class number incremented to \{classNumber} for pupil \{name} \{secondName} \{lastName}");
    }

    public long getAge(){
        return ChronoUnit.YEARS.between(dateOfBirth, LocalDateTime.now());
    }

    public String getFio(){
        return STR."\{name} \{lastName} \{secondName}";
    }

}
