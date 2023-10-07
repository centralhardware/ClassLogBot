package me.centralhardware.znatoki.telegram.statistic.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Builder
@Getter
@Setter
public final class TelegramUser {
    private Long id;
    private Role role;
    private UUID organizationId;
    private List<Long> services;

}
