package me.centralhardware.znatoki.telegram.statistic.configuration

import me.centralhardware.znatoki.telegram.statistic.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient

@Configuration
class TelegramClientConfiguration {

    @Bean
    fun getTelegramClient(): OkHttpTelegramClient {
        return OkHttpTelegramClient(Config.Telegram.token)
    }

}