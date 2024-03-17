package me.centralhardware.znatoki.telegram.statistic.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class ResourceConfiguration {

    companion object {
        const val RESOURCE_BUNDLE_NAME = "Strings"
    }

    @Bean
    fun getResource(): ResourceBundle {
        return ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, Locale.US)
    }

}