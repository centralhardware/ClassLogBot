package me.centralhardware.znatoki.telegram.statistic.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
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
import java.util.stream.Collectors;

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

    public String getInfo(List<Subject> subjects) {
        String know             = howToKnow         == null? "" : howToKnow.toString();
        String nameMother       = motherName        == null? "" : motherName;
        String updated          = updateBy          == null? "" : updateBy.toString();

        return "id=" +                            TelegramUtil.makeBold(id)+
                "\nфамилия=" +                    TelegramUtil.makeBold(secondName) +
                "\nимя=" +                        TelegramUtil.makeBold(name) +
                "\nотчество=" +                   TelegramUtil.makeBold(lastName) +
                "\nкласс=" +                      TelegramUtil.makeBold(classNumber) +
                "\nдата записи=" +                TelegramUtil.makeBold(dateFormatter.format(dateOfRecord)) +
                "\nдата рождения=" +              TelegramUtil.makeBold(dateFormatter.format(dateOfBirth)) +
                "\nтелефон=" +                    TelephoneUtils.format(telephone) +
                "\nтелефон ответственного=" +     TelephoneUtils.format( telephoneResponsible) +
                "\nкак узнал=" +                  TelegramUtil.makeBold(know) +
                "\nПредметы=" +                   TelegramUtil.makeBold(subjects.stream().map(Subject::getRusName).collect(Collectors.joining(","))) +
                "\nимя матери=" +                 TelegramUtil.makeBold(nameMother) +
                "\nдата создания=" +              TelegramUtil.makeBold(dateFormatter.format(createDate)) +
                "\nдата изменения=" +             TelegramUtil.makeBold(dateFormatter.format(modifyDate)) +
                "\nсоздано=" + created_by +
                "\nредактировано=" +              updated + "\n";
    }

    public void incrementClassNumber(){
        classNumber++;
        log.info("class number incremented to {} for pupil {} {} {}", classNumber, name, secondName, lastName);
    }

    public long getAge(){
        return ChronoUnit.YEARS.between(dateOfBirth, LocalDateTime.now());
    }

    public String getFio(){
        return String.format("%s %s %s", name, lastName, secondName);
    }

}
