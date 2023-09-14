package me.centralhardware.znatoki.telegram.statistic;

import lombok.Getter;

public class Config {

    @Getter
    private static final String telegramUsername    = System.getenv("TELEGRAM_USERNAME");
    @Getter
    private static final String telegramToken       = System.getenv("TELEGRAM_TOKEN");
    @Getter
    private static final Long adminId               = Long.valueOf(System.getenv("TELEGRAM_ADMIN"));
    @Getter
    private static final String baseUrl             = System.getenv("BASE_URL");

}
