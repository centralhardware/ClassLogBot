package me.centralhardware.znatoki.telegram.statistic.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove

@Configuration
class TelegramConfiguration {

    @Bean
    fun replyKeyboardRemove(): ReplyKeyboardRemove = ReplyKeyboardRemove
        .builder()
        .removeKeyboard(true)
        .build()
}