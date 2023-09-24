package me.centralhardware.znatoki.telegram.statistic.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class Organization {

    private UUID id;
    private String name;
    private String ownerFio;
    private Long owner;
    private Set<String> services = new HashSet<>();
    private Set<String> ownerServices = new HashSet<>();

}
