package me.centralhardware.znatoki.telegram.statistic.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.centralhardware.znatoki.telegram.statistic.utils.TimeStampUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;

@Entity
@Table
@NoArgsConstructor
public class Session {

    private static final int EXPIRATION_TIME = 600000;
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    @Getter
    private String uuid;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pupil")
    @Getter
    private Pupil pupil;
    @Column
    @Getter
    private Long updateBy;
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_date")
    private Date createDate;

    public Session(@NonNull Pupil pupil, @NonNull Long updateBy) {
        this.pupil = pupil;
        this.updateBy = updateBy;
    }

    public boolean isExpire() {
        return TimeStampUtils.getTimestamp() - createDate.getTime() > EXPIRATION_TIME;
    }
}
