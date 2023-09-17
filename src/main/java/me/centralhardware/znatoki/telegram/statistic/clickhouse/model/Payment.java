package me.centralhardware.znatoki.telegram.statistic.clickhouse.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class Payment {

    private LocalDateTime dateTime;
    private Long chatId;
    private Integer pupilId;
    private Integer amount;
    private String photoId;
    private UUID timeId;

}
