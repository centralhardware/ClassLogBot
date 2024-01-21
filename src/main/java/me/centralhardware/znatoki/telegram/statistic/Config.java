package me.centralhardware.znatoki.telegram.statistic;

import lombok.Getter;

public class Config {

    public static class Telegram{
        @Getter
        private static final String telegramUsername = getSystemEnv("BOT_USERNAME");
        @Getter
        private static final String telegramToken = getSystemEnv("BOT_TOKEN");
        @Getter
        private static final Double telegramRateLimit = Double.valueOf(getSystemEnv("TELEGRAM_RATE_LIMIT"));
        @Getter
        private static final String startTelegraph = getSystemEnv("START_TELEGRAPH");
    }

    public static class Minio{
        @Getter
        private static final String minioUrl = getSystemEnv("MINIO_URL");
        @Getter
        private static final String minioBucket = getSystemEnv("MINIO_BUCKET");
        @Getter
        private static final Integer minioPort = Integer.parseInt(getSystemEnv("MINIO_PORT"));
        @Getter
        private static final String minioAccessKey = getSystemEnv("MINIO_ACCESS_KEY");
        @Getter
        private static final String minioSecretKey = getSystemEnv("MINIO_SECRET_KEY");
        @Getter
        private static final String minioBasePath = getSystemEnv("BASE_PATH");
    }

    public static class Redis{
        @Getter
        private static final String redisHost = getSystemEnv("REDIS_HOST");
        @Getter
        private static final Integer redisPort = Integer.parseInt(getSystemEnv("REDIS_PORT"));
    }

    public static class Clickhouse{
        @Getter
        private static final String clickhouseUrl = getSystemEnv("CLICKHOUSE_URL");
    }

    public static class Web{
        @Getter
        private static final String baseUrl = getSystemEnv("BASE_URL");
    }


    private static String getSystemEnv(String env) {
        return System.getenv(env);
    }
}
