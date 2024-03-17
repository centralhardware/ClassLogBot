package me.centralhardware.znatoki.telegram.statistic.configuration

import kotliquery.sessionOf
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class SessionConfiguration {

    @Bean
    fun session(dataSource: DataSource) = sessionOf(dataSource)

}