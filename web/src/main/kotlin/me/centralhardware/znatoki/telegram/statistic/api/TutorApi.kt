package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.exception.*

@Serializable
data class TutorDetailDto(
    val id: Long,
    val name: String,
    val permissions: List<String>,
    val subjects: List<TutorSubjectDto>,
    val isAdmin: Boolean = false
)

@Serializable
data class TutorSubjectDto(
    val id: Long,
    val name: String
)

@Serializable
data class CurrentUserDto(
    val tutorId: Long,
    val name: String,
    val isAdmin: Boolean,
    val subjects: List<TutorSubjectDto>,
    val permissions: List<String>
)

@Serializable
data class UpdateTutorRequest(
    val name: String? = null,
    val permissions: List<String>,
    val subjectIds: List<Long>
)

fun Tutor.toDetailDto(): TutorDetailDto {
    val subjectsList = subjects.map { subjectId ->
        TutorSubjectDto(
            id = subjectId.id,
            name = SubjectMapper.getNameById(subjectId) ?: ""
        )
    }
    return TutorDetailDto(
        id = id.id,
        name = name,
        permissions = permissions.map { it.name },
        subjects = subjectsList,
        isAdmin = permissions.contains(Permissions.ADMIN)
    )
}

fun Route.tutorApi() {
    get("/api/me") {
        val tutorId = call.authenticatedTutorId
        val tutor = TutorMapper.findByIdOrNull(tutorId) ?: throw NotFoundException("User not found")
        
        val subjectsList = tutor.subjects.map { subjectId ->
            TutorSubjectDto(
                id = subjectId.id,
                name = SubjectMapper.getNameById(subjectId) ?: ""
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
            val tutors = TutorMapper.getAll().map { it.toDetailDto() }
            KSLog.info("TutorApi.GET: Admin ${requestingTutorId.id} loaded all tutors")
            call.respond(tutors)
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull() ?: throw BadRequestException("Invalid tutor ID")
            val tutorId = call.authenticatedTutorId
            val request = call.receive<UpdateTutorRequest>()

            // Validate permissions
            val validPermissions = Permissions.entries.map { it.name }
            val invalidPermissions = request.permissions.filter { it !in validPermissions }
            if (invalidPermissions.isNotEmpty()) {
                throw ValidationException("Invalid permissions: $invalidPermissions")
            }

            // Validate name if provided
            if (request.name != null && request.name.isBlank()) {
                throw ValidationException("Name cannot be blank")
            }

            // Update tutor
            TutorMapper.updateTutor(id, request.permissions, request.subjectIds, request.name)

            val updatedTutor = TutorMapper.findByIdOrNull(TutorId(id)) ?: throw NotFoundException("Tutor not found")

            KSLog.info("TutorApi.PUT: User ${tutorId.id} updated tutor $id")
            call.respond(HttpStatusCode.OK, updatedTutor.toDetailDto())
        }
    }
}
