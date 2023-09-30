package me.centralhardware.znatoki.telegram.statistic.typeHandler;

import me.centralhardware.znatoki.telegram.statistic.eav.PropertyDefs;

public class CustomPropertiesTypeHandler extends JsonTypeHandler<PropertyDefs>{
    @Override
    protected Class<PropertyDefs> getCLazz() {
        return PropertyDefs.class;
    }

}
