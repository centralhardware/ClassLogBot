package me.centralhardware.znatoki.telegram.statistic;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class Configuration {

    private final Boolean isDemo = Boolean.valueOf(System.getenv("IS_DEMO"));

}
