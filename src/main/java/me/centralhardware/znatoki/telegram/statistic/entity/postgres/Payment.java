package me.centralhardware.znatoki.telegram.statistic.entity.postgres;

import lombok.Getter;
import lombok.Setter;
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder;
import me.centralhardware.znatoki.telegram.statistic.eav.Property;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Payment {

    private Integer id;
    private LocalDateTime dateTime;
    private Long chatId;
    private Integer clientId;
    private Integer amount;
    private UUID timeId;
    private UUID organizationId;
    private Long serviceId;
    private List<Property> properties = new ArrayList<>();
    private PropertiesBuilder propertiesBuilder;

}
