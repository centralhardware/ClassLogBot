package me.centralhardware.znatoki.telegram.statistic.eav;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.centralhardware.znatoki.telegram.statistic.eav.types.JacksonDeserializer;
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type;

public record PropertyDef(
        @JsonDeserialize(using = JacksonDeserializer.class)
        Type type,
        String name,
        String[] enumeration,
        Boolean isOptional,
        Boolean isUnique
){}