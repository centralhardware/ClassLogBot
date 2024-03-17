package me.centralhardware.znatoki.telegram.statistic.entity

import java.util.UUID

class TelegramUser(
    val id: Long,
    val role: Role,
    val organizationId: UUID,
    val services: List<Long>,
    val name: String
)