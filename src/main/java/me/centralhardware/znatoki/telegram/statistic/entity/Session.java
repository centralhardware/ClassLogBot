package me.centralhardware.znatoki.telegram.statistic.entity;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class Session {

    private static final int EXPIRATION_TIME = 600;
    @Getter
    private String uuid;
    @Getter
    private Integer pupil;
    @Getter
    private Long updateBy;
    @Column(name = "create_date")
    private LocalDateTime createDate;

    public Session(@NonNull Pupil pupil, @NonNull Long updateBy) {
        this.uuid = UUID.randomUUID().toString();
        this.pupil = pupil.getId();
        this.updateBy = updateBy;
        this.createDate= LocalDateTime.now();
    }

    public boolean isExpire() {
        return ChronoUnit.SECONDS.between(createDate, LocalDateTime.now()) > EXPIRATION_TIME;
    }
}
