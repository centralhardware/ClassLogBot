package me.centralhardware.znatoki.telegram.statistic.entity.postgres;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
public final class TelegramUser {
    private Long id;
    private Role role;
    private UUID organizationId;
    private List<Long> services;
    private String name;

}
