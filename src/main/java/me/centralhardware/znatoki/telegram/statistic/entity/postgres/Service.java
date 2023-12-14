package me.centralhardware.znatoki.telegram.statistic.entity.postgres;

import lombok.Getter;
import lombok.Setter;
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder;
import me.centralhardware.znatoki.telegram.statistic.eav.Property;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
public class Service {

    private LocalDateTime dateTime;
    private UUID id;
    private Long chatId;
    private Long serviceId;
    private Set<Integer> clientIds = new HashSet<>();
    private Integer clientId;
    private Integer amount;
    private UUID organizationId;
    private List<Property> properties;
    private PropertiesBuilder propertiesBuilder;

}
