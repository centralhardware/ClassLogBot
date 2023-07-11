package me.centralhardware.znatoki.telegram.statistic.clickhouse.model;

import java.time.LocalDateTime;

public record Pupil(
        Integer classNumber,
        LocalDateTime dateOfBirth,
        String lastName,
        String name,
        String secondName
) {
}
