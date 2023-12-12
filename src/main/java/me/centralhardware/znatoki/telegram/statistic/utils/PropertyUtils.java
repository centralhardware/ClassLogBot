package me.centralhardware.znatoki.telegram.statistic.utils;

import me.centralhardware.znatoki.telegram.statistic.eav.Property;
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo;
import me.centralhardware.znatoki.telegram.statistic.eav.types.Telephone;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PropertyUtils {
    public static String print(List<Property> properties) {
        return Optional.ofNullable(properties)
                .orElse(Collections.emptyList())
                .stream()
                .filter(it -> !(it.type() instanceof Photo))
                .map(PropertyUtils::applyTypeFormat)
                .map(property -> STR."\{property.name()}=\{TelegramUtil.makeBold(property.value())}")
                .collect(Collectors.joining("\n"));
    }

    private static Property applyTypeFormat(Property property) {
        if (property.type() instanceof Telephone) {
            var telephoneFormatted = TelephoneUtils.format(property.value());
            return property.withValue(telephoneFormatted);
        } else {
            return property;
        }
    }
}
