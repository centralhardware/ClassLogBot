package me.centralhardware.znatoki.telegram.statistic.entity.postgres;

import lombok.Getter;
import lombok.Setter;
import me.centralhardware.znatoki.telegram.statistic.eav.Property;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
public class Services {

    private Long id;
    private String key;
    private String name;
    private UUID orgId;
    private Boolean allowMultiplyClients;
    private List<Property> properties;

}
