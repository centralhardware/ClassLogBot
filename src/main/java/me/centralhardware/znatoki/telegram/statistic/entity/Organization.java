package me.centralhardware.znatoki.telegram.statistic.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Organization {

    private UUID id;
    private String name;
    private Long owner;

}
