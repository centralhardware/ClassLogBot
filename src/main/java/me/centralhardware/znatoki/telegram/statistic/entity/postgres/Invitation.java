package me.centralhardware.znatoki.telegram.statistic.entity.postgres;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class Invitation {

    private UUID orgId;
    private Set<Long> services = new HashSet<>();
    private String confirmCode;

}
