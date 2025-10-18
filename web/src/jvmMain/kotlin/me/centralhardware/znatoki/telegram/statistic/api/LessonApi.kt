package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.dto.*
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.extensions.hashtag
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.AuditLogMapper
import me.centralhardware.znatoki.telegram.statistic.exception.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable

@Serializable
data class AddStudentToLessonRequest(
    val studentId: Int
)

fun List<Lesson>.toLessonDto(): LessonDto {
    val firstLesson = this.first()
    val subjectName = SubjectMapper.getNameById(firstLesson.subjectId)

    val students = this.map { lesson ->
        val student = StudentMapper.findById(lesson.studentId)
        StudentDto(
            id = student.id.id,
            name = student.name,
            secondName = student.secondName,
            lastName = student.lastName,
            schoolClass = student.schoolClass?.value,
            recordDate = student.recordDate?.toString(),
            birthDate = student.birthDate?.toString(),
            source = student.source?.title,
            phone = student.phone?.value,
            responsiblePhone = student.responsiblePhone?.value,
            motherFio = student.motherFio
        )
    }

    return LessonDto(
        id = firstLesson.id.id.toString(),
        dateTime = firstLesson.dateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
        students = students,
        subject = SubjectDto(
            id = firstLesson.subjectId.id,
            name = subjectName
        ),
        amount = firstLesson.amount,
        isGroup = this.size > 1 || firstLesson.forceGroup,
        isExtra = firstLesson.extraHalfHour,
        tutorId = firstLesson.tutorId.id,
        photoReport = firstLesson.photoReport?.let { "/api/image/$it" },
        dataSource = firstLesson.dataSource?.value
    )
}


fun Route.lessonApi() {
    route("/api/lessons") {
        get {
            val tutorId = call.authenticatedTutorId
            val lessons = LessonMapper.getTodayTimes(tutorId)
                .groupBy { it.id }
                .map { (_, groupedLessons) -> groupedLessons.toLessonDto() }
                .sortedBy { it.dateTime }

            KSLog.info("LessonsApi.GET: User ${tutorId.id} loaded today's lessons")
            call.respond(lessons)
        }

        requires(Permissions.ADD_TIME).apply {
            post {
                val tutorId = call.authenticatedTutorId
                val request = call.receive<CreateLessonRequest>()

                if (request.studentIds.isEmpty()) {
                    throw ValidationException("At least one student is required")
                }

                if (request.amount <= 0) {
                    throw ValidationException("Amount must be positive")
                }

                val lessonId = LessonId.random()
                val subjectId = SubjectId(request.subjectId)

                request.studentIds.forEach { studentI ->
                    val studentId = studentI.toStudentId()
                    val lesson = Lesson(
                        id = lessonId,
                        dateTime = LocalDateTime.now(),
                        updateTime = LocalDateTime.now(),
                        tutorId = tutorId,
                        subjectId = subjectId,
                        studentId = studentId,
                        _amount = request.amount.toAmount(),
                        forceGroup = request.forceGroup,
                        extraHalfHour = request.extraHalfHour,
                        photoReport = request.photoReport,
                        deleted = false,
                        addedByTutorId = tutorId,
                        dataSource = DataSource.WEB
                    )
                    LessonMapper.insert(lesson)
                    lesson
                }

                val lessons = LessonMapper.findById(lessonId)

                lessons.forEach { lesson ->
                    AuditLogMapper.log(
                        userId = tutorId.id,
                        action = "CREATE_LESSON",
                        entityType = "lesson",
                        entityId = lesson.id.id.toString(),
                        studentId = lesson.studentId.id.toInt(),
                        subjectId = request.subjectId.toInt(),
                        null,
                        lesson
                    )
                }

                KSLog.info("LessonsApi.POST: User ${tutorId.id} created lesson with ${request.studentIds.size} students")
                call.respond(HttpStatusCode.Created, lessons.toLessonDto())
            }

            put("/{id}") {
                val lessonIdStr = call.parameters["id"] ?: throw BadRequestException("Invalid lesson ID")
                val tutorId = call.authenticatedTutorId
                val request = call.receive<UpdateLessonRequest>()
                val lessonId = lessonIdStr.toLessonId()

                val lessons = LessonMapper.findById(lessonId)
                if (lessons.isEmpty()) {
                    throw NotFoundException("Lesson not found")
                }

                val oldLesson = lessons.first()

                val hasAmountChange = request.amount > 0 && oldLesson.amount != request.amount.toDouble()
                val hasForceGroupChange = oldLesson.forceGroup != request.forceGroup
                val hasExtraHalfHourChange = oldLesson.extraHalfHour != request.extraHalfHour
                val hasSubjectChange = oldLesson.subjectId.id != request.subjectId

                if (!hasAmountChange && !hasForceGroupChange && !hasExtraHalfHourChange && !hasSubjectChange) {
                    KSLog.info("LessonsApi.PUT: User ${tutorId.id} attempted to update lesson $lessonIdStr with no changes")
                    call.respond(HttpStatusCode.OK, lessons.toLessonDto())
                    return@put
                }

                if (hasAmountChange) {
                    LessonMapper.setAmount(lessonId, request.amount.toAmount())
                }

                if (hasForceGroupChange) {
                    LessonMapper.setForceGroup(lessonId, request.forceGroup)
                }

                if (hasExtraHalfHourChange) {
                    LessonMapper.setExtraHalfHour(lessonId, request.extraHalfHour)
                }

                if (hasSubjectChange) {
                    LessonMapper.setSubjectId(lessonId, SubjectId(request.subjectId))
                }

                val updatedLessons = LessonMapper.findById(lessonId)
                val updatedLesson = updatedLessons.first()

                AuditLogMapper.log(
                    userId = tutorId.id,
                    action = "UPDATE_LESSON",
                    entityType = "lesson",
                    entityId = lessonId.id.toString(),
                    studentId = oldLesson.studentId.id.toInt(),
                    subjectId = oldLesson.subjectId.id.toInt(),
                    oldLesson,
                    updatedLesson
                )

                KSLog.info("LessonsApi.PUT: User ${tutorId.id} updated lesson $lessonIdStr")
                call.respond(HttpStatusCode.OK, updatedLessons.toLessonDto())
            }

            delete("/{id}/student/{studentId}") {
                val lessonIdStr = call.parameters["id"] ?: throw BadRequestException("Invalid lesson ID")
                val studentIdStr =
                    call.parameters["studentId"]?.toIntOrNull() ?: throw BadRequestException("Invalid student ID")
                val tutorId = call.authenticatedTutorId

                val lessonId = lessonIdStr.toLessonId()
                val studentId = studentIdStr.toStudentId()

                val allLessons = LessonMapper.findById(lessonId)
                if (allLessons.isEmpty()) {
                    throw NotFoundException("Lesson not found")
                }

                if (allLessons.size == 1) {
                    throw ValidationException("Cannot remove last student from lesson")
                }

                val lessonToDelete = allLessons.find { it.studentId == studentId }
                    ?: throw NotFoundException("Student not in this lesson")

                LessonMapper.setDeleted(lessonId, true, studentId)

                AuditLogMapper.log(
                    userId = tutorId.id,
                    action = "REMOVE_STUDENT_FROM_LESSON",
                    entityType = "lesson",
                    entityId = lessonId.id.toString(),
                    studentId = studentId.id.toInt(),
                    subjectId = lessonToDelete.subjectId.id.toInt(),
                    lessonToDelete,
                    null
                )

                KSLog.info("LessonsApi.DELETE: User ${tutorId.id} removed student $studentIdStr from lesson $lessonIdStr")
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            }

            post("/{id}/student") {
                val lessonIdStr = call.parameters["id"] ?: throw BadRequestException("Invalid lesson ID")
                val tutorId = call.authenticatedTutorId
                val request = call.receive<AddStudentToLessonRequest>()
                val lessonId = lessonIdStr.toLessonId()

                val existingLessons = LessonMapper.findById(lessonId)
                if (existingLessons.isEmpty()) {
                    throw NotFoundException("Lesson not found")
                }

                val templateLesson = existingLessons.first()
                val newStudentId = request.studentId.toStudentId()

                if (existingLessons.any { it.studentId == newStudentId }) {
                    throw ValidationException("Student already in this lesson")
                }

                val baseAmount = if (templateLesson.extraHalfHour) {
                    (templateLesson.amount / 1.5).toInt()
                } else {
                    templateLesson.amount.toInt()
                }

                val newLesson = Lesson(
                    id = templateLesson.id,
                    dateTime = templateLesson.dateTime,
                    updateTime = LocalDateTime.now(),
                    tutorId = templateLesson.tutorId,
                    subjectId = templateLesson.subjectId,
                    studentId = newStudentId,
                    _amount = baseAmount.toAmount(),
                    forceGroup = templateLesson.forceGroup,
                    extraHalfHour = templateLesson.extraHalfHour,
                    photoReport = templateLesson.photoReport,
                    deleted = false,
                    addedByTutorId = tutorId,
                    dataSource = DataSource.WEB
                )

                LessonMapper.insert(newLesson)

                AuditLogMapper.log(
                    userId = tutorId.id,
                    action = "ADD_STUDENT_TO_LESSON",
                    entityType = "lesson",
                    entityId = lessonId.id.toString(),
                    studentId = newStudentId.id.toInt(),
                    subjectId = templateLesson.subjectId.id.toInt(),
                    null,
                    newLesson
                )

                KSLog.info("LessonsApi.POST: User ${tutorId.id} added student ${request.studentId} to lesson $lessonIdStr")
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            }

            delete("/{id}") {
                val lessonIdStr = call.parameters["id"] ?: throw BadRequestException("Invalid lesson ID")
                val tutorId = call.authenticatedTutorId
                val lessonId = lessonIdStr.toLessonId()

                val lessons = LessonMapper.findById(lessonId)
                if (lessons.isEmpty()) {
                    throw NotFoundException("Lesson not found")
                }

                LessonMapper.setDeleted(lessonId, true)

                lessons.forEach { lesson ->
                    AuditLogMapper.log(
                        userId = tutorId.id,
                        action = "DELETE_LESSON",
                        entityType = "lesson",
                        entityId = lesson.id.toString(),
                        studentId = lesson.studentId.id.toInt(),
                        subjectId = lesson.subjectId.id.toInt(),
                        lesson,
                        null
                    )
                }


                KSLog.info("LessonsApi.DELETE: User ${tutorId.id} deleted lesson $lessonIdStr")
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            }
        }
    }
}
