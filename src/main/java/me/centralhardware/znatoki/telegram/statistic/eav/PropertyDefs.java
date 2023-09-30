package me.centralhardware.znatoki.telegram.statistic.eav;

import org.apache.commons.collections.CollectionUtils;

import java.util.List;

public record PropertyDefs(
        List<PropertyDef> propertyDefs
) {
    public Boolean isEmpty(){
        return CollectionUtils.isEmpty(propertyDefs);
    }
}
