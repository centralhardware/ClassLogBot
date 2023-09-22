package me.centralhardware.znatoki.telegram.statistic.configuration;

import com.ibm.icu.text.Transliterator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransliteratorConfiguration {

    public static final String CYRILLIC_TO_LATIN = "Cyrillic-Latin";

    @Bean
    public Transliterator getTranslitirator(){
        return Transliterator.getInstance(CYRILLIC_TO_LATIN);
    }

}
