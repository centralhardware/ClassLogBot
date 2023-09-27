package me.centralhardware.znatoki.telegram.statistic.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@NoArgsConstructor
public class Session {

    private static final int EXPIRATION_TIME = 600;
    @Getter
    private String uuid;
    @Getter
    private Integer clientId;
    @Getter
    private Long updateBy;
    private LocalDateTime createDate;

    public Session(@NonNull Client client, @NonNull Long updateBy) {
        this.uuid = UUID.randomUUID().toString();
        this.clientId = client.getId();
        this.updateBy = updateBy;
        this.createDate= LocalDateTime.now();
    }

    public boolean isExpire() {
        return ChronoUnit.SECONDS.between(createDate, LocalDateTime.now()) > EXPIRATION_TIME;
    }
}
