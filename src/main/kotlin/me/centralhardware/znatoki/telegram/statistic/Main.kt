package me.centralhardware.znatoki.telegram.statistic

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration

@SpringBootApplication
@EnableJpaRepositories("me.centralhardware.znatoki.telegram.statistic.repository")
@EnableScheduling
@Import(TelegramBotStarterConfiguration::class)
class Main

fun main(args: Array<String>) {
    SpringApplication.run(Main::class.java, *args)
}