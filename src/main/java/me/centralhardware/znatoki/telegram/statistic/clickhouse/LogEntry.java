package me.centralhardware.znatoki.telegram.statistic.clickhouse;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LogEntry (
        LocalDateTime dateTime,
        Long chatId,
        String username,
        String firstName,
        String lastName,
        Boolean isPremium,
        String lang,
        String action,
        String text
){ }
