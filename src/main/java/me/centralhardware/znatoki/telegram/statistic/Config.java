package me.centralhardware.znatoki.telegram.statistic;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

public class Config {

    @Getter
    private static final String telegramUsername    = System.getenv("BOT_USERNAME");
    @Getter
    private static final String telegramToken       = System.getenv("BOT_TOKEN");
    @Getter
    private static final Double telegramRateLimit = Double.valueOf(System.getenv("TELEGRAM_RATE_LIMIT"));

    @Getter
    private static final String baseUrl             = System.getenv("BASE_URL");
    @Getter
    private static final Long logChatId = Long.parseLong(System.getenv("LOG_CHAT"));
    @Getter
    private static final List<Long> reportSendTo = Arrays.stream(System.getenv("REPORT_SEND_TO").split(","))
            .map(Long::parseLong)
            .toList();

    @Getter
    private static final String minioUrl = System.getenv("MINIO_URL");
    @Getter
    private static final Integer minioPort = Integer.parseInt(System.getenv("MINIO_PORT"));
    @Getter
    private static final String minioAccessKey = System.getenv("MINIO_ACCESS_KEY");
    @Getter
    private static final String minioSecretKey = System.getenv("MINIO_SECRET_KEY");
    @Getter
    private static final String minioBasePath = System.getenv("BASE_PATH");


    @Getter
    private static final String redisHost = System.getenv("REDIS_HOST");
    @Getter
    private static final Integer redisPort = Integer.parseInt(System.getenv("REDIS_PORT"));


    @Getter
    private static final String clickhouseUrl = System.getenv("CLICKHOUSE_URL");
}
