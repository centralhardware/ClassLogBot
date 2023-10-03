package me.centralhardware.znatoki.telegram.statistic.typeHandler;

import me.centralhardware.znatoki.telegram.statistic.eav.Property;

public class PropertiesTypeHandler extends JsonListTypeHandler<Property>{
    @Override
    protected Class<Property> getCLazz() {
        return Property.class;
    }
}
