package me.centralhardware.znatoki.telegram.statistic.entity.postgres;

import lombok.Getter;
import lombok.Setter;
import me.centralhardware.znatoki.telegram.statistic.eav.PropertyDefs;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class Organization {

    private UUID id;
    private String name;
    private String ownerFio;
    private Long owner;
    private Long logChatId;
    private PropertyDefs serviceCustomProperties;
    private PropertyDefs clientCustomProperties;
    private PropertyDefs paymentCustomProperties;
    private String grafanaUsername;
    private String grafanaPassword;
    private String grafanaUrl;
    private Set<String> services = new HashSet<>();
    private Set<String> ownerServices = new HashSet<>();

}
