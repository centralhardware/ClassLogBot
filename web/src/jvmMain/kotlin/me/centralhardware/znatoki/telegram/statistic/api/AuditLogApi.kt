package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.centralhardware.znatoki.telegram.statistic.dto.*
import me.centralhardware.znatoki.telegram.statistic.entity.AuditLog
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.extensions.hasAdminPermission
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.AuditLogMapper
import me.centralhardware.znatoki.telegram.statistic.exception.*
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import java.time.format.DateTimeFormatter

fun AuditLog.toDto(): AuditLogDto {
    val user = TutorMapper.findByIdOrNull(TutorId(userId))

    val userName = user?.let { tutor ->
        val parts = tutor.name.split(" ")
        if (parts.size >= 2) {
            "${parts[0]} ${parts[1].firstOrNull()?.toString()?.uppercase() ?: ""}." +
                    (parts.getOrNull(2)?.firstOrNull()?.toString()?.uppercase()?.let { " $it." } ?: "")
        } else {
            tutor.name
        }
    }

    val studentName = studentId?.let {
        StudentMapper.findById(
            it.toStudentId()
        ).let { student ->
            val parts = listOf(student.name, student.secondName, student.lastName)
            if (parts.size >= 2) {
                "${parts[0]} ${parts[1].firstOrNull()?.toString()?.uppercase() ?: ""}." +
                        (parts.getOrNull(2)?.firstOrNull()?.toString()?.uppercase()?.let { " $it." } ?: "")
            } else {
                student.name
            }
        }
    }

    val subjectName = subjectId?.let {
        SubjectMapper.getNameById(
            SubjectId(it.toLong())
        )
    }

    return AuditLogDto(
        id = id,
        userId = userId,
        userName = userName,
        action = action,
        entityType = entityType,
        entityId = entityId,
        details = details,
        timestamp = timestamp.atZone(java.time.ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
        studentName = studentName,
        subject = subjectName
    )
}

fun Route.auditLogApi() {
    route("/api/audit-log") {
        get {
            val tutorId = call.authenticatedTutorId
            val tutor = TutorMapper.findByIdOrNull(tutorId) ?: throw NotFoundException("User not found")

            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0
            val filterTutorId = call.request.queryParameters["tutorId"]?.toLongOrNull()
            val filterSubjectId = call.request.queryParameters["subjectId"]?.toIntOrNull()
            val filterAction = call.request.queryParameters["action"]

            val logs = if (tutor.hasAdminPermission()) {
                AuditLogMapper.getAll(limit, offset, filterTutorId, filterSubjectId, filterAction)
            } else {
                AuditLogMapper.getByUserId(tutorId.id, limit, offset, filterSubjectId, filterAction)
            }

            val totalCount = if (tutor.hasAdminPermission()) {
                AuditLogMapper.count(filterTutorId, filterSubjectId, filterAction)
            } else {
                AuditLogMapper.countByUserId(tutorId.id, filterSubjectId, filterAction)
            }

            KSLog.info("AuditLogApi.GET: User ${tutorId.id} loaded ${logs.size} audit logs")
            call.respond(
                AuditLogResponse(
                    logs = logs.map { it.toDto() },
                    total = totalCount,
                    limit = limit,
                    offset = offset
                )
            )
        }
    }
}
