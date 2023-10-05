package me.centralhardware.znatoki.telegram.statistic.eav;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import me.centralhardware.znatoki.telegram.statistic.eav.jackson.TypeDeserializer;
import me.centralhardware.znatoki.telegram.statistic.eav.jackson.TypeSerializer;
import me.centralhardware.znatoki.telegram.statistic.eav.types.Type;

public record Property(
        String name,
        @JsonSerialize(using = TypeSerializer.class)
        @JsonDeserialize(using = TypeDeserializer.class)
        Type type,
        Boolean isIncludeInBio,
        String value) {

    public Property(String name, Type type, Boolean isIncludeInBio){
        this(name,type, isIncludeInBio,"");
    }

}
