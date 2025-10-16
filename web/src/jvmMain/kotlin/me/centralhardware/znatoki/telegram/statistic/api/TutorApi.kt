package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.centralhardware.znatoki.telegram.statistic.dto.*
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.exception.*

fun Tutor.toDto(): TutorDto {
    val subjectsList = subjects.map { subjectId ->
        SubjectDto(
            id = subjectId.id,
            name = SubjectMapper.getNameById(subjectId)
        )
    }
    return TutorDto(
        id = id.id,
        name = name,
        permissions = permissions.map { it.name },
        subjects = subjectsList
    )
}

fun Route.tutorApi() {
    get("/api/me") {
        val tutorId = call.authenticatedTutorId
        val tutor = TutorMapper.findByIdOrNull(tutorId) ?: throw NotFoundException("User not found")
        
        val subjectsList = tutor.subjects.map { subjectId ->
            SubjectDto(
                id = subjectId.id,
                name = SubjectMapper.getNameById(subjectId)
            )
        }
        
        val response = CurrentUserDto(
            tutorId = tutor.id.id,
            name = tutor.name,
            isAdmin = tutor.permissions.contains(Permissions.ADMIN),
            subjects = subjectsList,
            permissions = tutor.permissions.map { it.name }
        )
        
        KSLog.info("TutorApi.GET /me: User ${tutorId.id} loaded their info")
        call.respond(response)
    }

    route("/api/tutors") {
        requires(Permissions.ADMIN)

        get {
            val requestingTutorId = call.authenticatedTutorId
            val tutors = TutorMapper.getAll().map { it.toDto() }
            KSLog.info("TutorApi.GET: Admin ${requestingTutorId.id} loaded all tutors")
            call.respond(tutors)
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull() ?: throw BadRequestException("Invalid tutor ID")
            val tutorId = call.authenticatedTutorId
            val request = call.receive<UpdateTutorRequest>()

            val validPermissions = Permissions.entries.map { it.name }
            val invalidPermissions = request.permissions.filter { it !in validPermissions }
            if (invalidPermissions.isNotEmpty()) {
                throw ValidationException("Invalid permissions: $invalidPermissions")
            }

            if (request.name != null && request.name.isBlank()) {
                throw ValidationException("Name cannot be blank")
            }

            TutorMapper.updateTutor(id, request.permissions, request.subjectIds, request.name)

            val updatedTutor = TutorMapper.findByIdOrNull(TutorId(id)) ?: throw NotFoundException("Tutor not found")

            KSLog.info("TutorApi.PUT: User ${tutorId.id} updated tutor $id")
            call.respond(HttpStatusCode.OK, updatedTutor.toDto())
        }
    }
}
