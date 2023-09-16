package me.centralhardware.znatoki.telegram.statistic.clickhouse.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
public class Time {

    private LocalDateTime dateTime;
    private UUID id;
    private Long chatId;
    private String subject;
    private Set<Pair<String, Integer>> fios = new HashSet<>();
    private String fio;
    private Integer pupilId;
    private Integer amount;
    private String photoId;

}
