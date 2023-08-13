package me.centralhardware.znatoki.telegram.statistic.clickhouse.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
public class Time {

    private LocalDateTime dateTime;
    private UUID id;
    private Long chatId;
    private String subject;
    private Set<String> fios = new HashSet<>();
    private String fio;
    private Integer amount;
    private String photoId;

}
