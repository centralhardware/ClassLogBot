package me.centralhardware.znatoki.telegram.statistic.entity.postgres;

import lombok.Getter;
import lombok.Setter;
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder;
import me.centralhardware.znatoki.telegram.statistic.eav.Property;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Payment {

    private LocalDateTime dateTime;
    private Long chatId;
    private Integer pupilId;
    private Integer amount;
    private UUID timeId;
    private UUID organizationId;
    private List<Property> properties;
    private PropertiesBuilder propertiesBuilder;

}
