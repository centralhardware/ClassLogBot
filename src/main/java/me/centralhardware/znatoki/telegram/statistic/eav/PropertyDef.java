package me.centralhardware.znatoki.telegram.statistic.eav;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.centralhardware.znatoki.telegram.statistic.eav.jackson.TypeDeserializer;
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type;

public record PropertyDef(
        @JsonDeserialize(using = TypeDeserializer.class)
        Type type,
        String name,
        String[] enumeration,
        Boolean isOptional,
        Boolean isUnique
){}