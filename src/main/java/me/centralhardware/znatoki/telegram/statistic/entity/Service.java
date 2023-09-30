package me.centralhardware.znatoki.telegram.statistic.entity;

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
    private Set<Integer> serviceIds = new HashSet<>();
    private Integer pupilId;
    private Integer amount;
    private UUID organizationId;
    private List<Property> properties;
    private PropertiesBuilder propertiesBuilder;

}
