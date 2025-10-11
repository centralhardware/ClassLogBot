package me.centralhardware.znatoki.telegram.statistic.entity

import java.time.Instant

data class AuditLog(
    val id: Int,
    val userId: Long,
    val action: String,
    val entityType: String?,
    val entityId: Int?,
    val details: String?,
    val timestamp: Instant,
    val studentId: Int?,
    val subjectId: Int?
)
