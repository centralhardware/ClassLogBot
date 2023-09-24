package me.centralhardware.znatoki.telegram.statistic.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class Service {

    private Long id;
    private String key;
    private String name;
    private UUID orgId;

}
