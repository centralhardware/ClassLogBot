package me.centralhardware.znatoki.telegram.statistic.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.ResourceBundle;

@Configuration
public class ResourceConfiguration {

    public static final String RESOURCE_BUNDLE_NAME = "Strings";

    @Bean
    public ResourceBundle getResource() {
        return ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME, Locale.US);
    }

}
