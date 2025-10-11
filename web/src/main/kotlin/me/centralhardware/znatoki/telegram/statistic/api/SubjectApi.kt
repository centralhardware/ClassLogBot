package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.exception.*

@Serializable
data class SubjectDto(
    val subjectId: Long,
    val subjectName: String
)

fun Route.subjectApi() {
    route("/api/subjects") {
        get("/all") {
            val tutorId = call.authenticatedTutorId
            val subjects = SubjectMapper.getAllSubjects().map { 
                SubjectDto(it.subjectId, it.subjectName) 
            }
            KSLog.info("SubjectApi.GET /all: User ${tutorId.id} loaded all subjects")
            call.respond(subjects)
        }
    }
}
