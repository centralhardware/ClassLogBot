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
import kotlinx.serialization.Serializable
import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.extensions.hashtag
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.AuditLogMapper
import me.centralhardware.znatoki.telegram.statistic.service.DiffService
import me.centralhardware.znatoki.telegram.statistic.exception.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class LessonStudentDto(
    val id: Int,
    val name: String
)

@Serializable
data class LessonDto(
    val id: String,
    val dateTime: String,
    val tutorName: String,
    val subjectName: String,
    val studentName: String,
    val students: List<LessonStudentDto>,
    val amount: Double,
    val forceGroup: Boolean,
    val extraHalfHour: Boolean,
    val photoReport: String?
)

@Serializable
data class UpdateLessonRequest(
    val forceGroup: Boolean,
    val extraHalfHour: Boolean,
    val amount: Int? = null
)

@Serializable
data class AddStudentToLessonRequest(
    val studentId: Int
)

@Serializable
data class CreateLessonRequest(
    val subjectId: Long,
    val studentIds: List<Int>,
    val amount: Int,
    val forceGroup: Boolean = false,
    val extraHalfHour: Boolean = false,
    val photoReport: String
)

fun List<Lesson>.toLessonDto(): LessonDto {
    val firstLesson = this.first()
    val tutor = TutorMapper.findByIdOrNull(firstLesson.tutorId)
    val subject = SubjectMapper.getNameById(firstLesson.subjectId)

    val students = this.map { lesson ->
        val student = StudentMapper.findById(lesson.studentId)
        LessonStudentDto(
            id = lesson.studentId.id,
            name = student.fio()
        )
    }

    return LessonDto(
        id = firstLesson.id.id.toString(),
        dateTime = firstLesson.dateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
        tutorName = tutor?.name ?: "Unknown",
        subjectName = subject ?: "Unknown",
        studentName = students.joinToString(", ") { it.name },
        students = students,
        amount = firstLesson.amount,
        forceGroup = firstLesson.forceGroup,
        extraHalfHour = firstLesson.extraHalfHour,
        photoReport = firstLesson.photoReport?.let { "/api/image/$it" }
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

                request.studentIds.map { studentIdInt ->
                    val studentId = studentIdInt.toStudentId()
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
                        addedByTutorId = tutorId
                    )
                    LessonMapper.insert(lesson)
                    lesson
                }

                val lessons = LessonMapper.findById(lessonId)
                val subject = SubjectMapper.getNameById(subjectId)

                runBlocking {
                    try {
                        val bot = telegramBot(me.centralhardware.znatoki.telegram.statistic.Config.getString("BOT_TOKEN"))
                        val tutorName = TutorMapper.findByIdOrNull(tutorId)?.name?.hashtag() ?: "Unknown"
                        val students = lessons.joinToString(", ") { StudentMapper.findById(it.studentId).fio() }

                        val text = buildString {
                            appendLine("#создание_занятия")
                            appendLine("Дата: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}")
                            appendLine("Ученики: $students")
                            appendLine("Предмет: $subject")
                            appendLine("Сумма: ${request.amount} ₽")
                            if (request.forceGroup) appendLine("Групповое: Да")
                            if (request.extraHalfHour) appendLine("1.5 часа: Да")
                            appendLine()
                            append("Создал: $tutorName")
                        }.trimEnd()

                        bot.sendTextMessage(
                            Config.logChat(),
                            text
                        )
                    } catch (e: Exception) {
                        KSLog.error("LessonsApi.POST: Error sending log", e)
                    }
                }

                lessons.forEach { lesson ->
                    AuditLogMapper.log(
                        userId = tutorId.id,
                        action = "CREATE_LESSON",
                        entityType = "lesson",
                        entityId = lesson.id.id.toString(),
                        studentId = lesson.studentId.id,
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

                val hasAmountChange = request.amount != null && request.amount > 0 && oldLesson.amount != request.amount.toDouble()
                val hasForceGroupChange = oldLesson.forceGroup != request.forceGroup
                val hasExtraHalfHourChange = oldLesson.extraHalfHour != request.extraHalfHour

                if (!hasAmountChange && !hasForceGroupChange && !hasExtraHalfHourChange) {
                    KSLog.info("LessonsApi.PUT: User ${tutorId.id} attempted to update lesson $lessonIdStr with no changes")
                    call.respond(HttpStatusCode.OK, lessons.toLessonDto())
                    return@put
                }

                if (hasAmountChange) {
                    LessonMapper.setAmount(lessonId, request.amount!!.toAmount())
                }

                if (hasForceGroupChange) {
                    LessonMapper.setForceGroup(lessonId, request.forceGroup)
                }

                if (hasExtraHalfHourChange) {
                    LessonMapper.setExtraHalfHour(lessonId, request.extraHalfHour)
                }

                val updatedLessons = LessonMapper.findById(lessonId)
                val updatedLesson = updatedLessons.first()

                AuditLogMapper.log(
                    userId = tutorId.id,
                    action = "UPDATE_LESSON",
                    entityType = "lesson",
                    entityId = lessonId.id.toString(),
                    studentId = oldLesson.studentId.id,
                    subjectId = oldLesson.subjectId.id.toInt(),
                    oldLesson,
                    updatedLesson
                )

            KSLog.info("LessonsApi.PUT: User ${tutorId.id} updated lesson $lessonIdStr")
            call.respond(HttpStatusCode.OK, updatedLessons.toLessonDto())
        }

        delete("/{id}/student/{studentId}") {
            val lessonIdStr = call.parameters["id"] ?: throw BadRequestException("Invalid lesson ID")
            val studentIdStr = call.parameters["studentId"]?.toIntOrNull() ?: throw BadRequestException("Invalid student ID")
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

                val student = StudentMapper.findById(studentId)
                val subject = SubjectMapper.getNameById(lessonToDelete.subjectId)

                runBlocking {
                    try {
                        val bot = telegramBot(Config.getString("BOT_TOKEN"))
                        val tutorName = TutorMapper.findByIdOrNull(tutorId)?.name?.hashtag() ?: "Unknown"

                        val text = buildString {
                            appendLine("#удаление_ученика_из_занятия")
                            appendLine("Дата: ${lessonToDelete.dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}")
                            appendLine("Ученик: ${student.fio().hashtag()}")
                            appendLine("Предмет: $subject")
                            appendLine()
                            append("Удалил: $tutorName")
                        }.trimEnd()

                        bot.sendTextMessage(
                            Config.logChat(),
                            text
                        )
                    } catch (e: Exception) {
                        KSLog.error("LessonsApi.DELETE student: Error sending log", e)
                    }
                }

                AuditLogMapper.log(
                    userId = tutorId.id,
                    action = "REMOVE_STUDENT_FROM_LESSON",
                    entityType = "lesson",
                    entityId = lessonId.id.toString(),
                    studentId = studentId.id,
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
                    addedByTutorId = tutorId
                )

                LessonMapper.insert(newLesson)

                val student = StudentMapper.findById(newStudentId)
                val subject = SubjectMapper.getNameById(templateLesson.subjectId)

                runBlocking {
                    try {
                        val bot = telegramBot(Config.getString("BOT_TOKEN"))
                        val tutorName = TutorMapper.findByIdOrNull(tutorId)?.name?.hashtag() ?: "Unknown"

                        val text = buildString {
                            appendLine("#добавление_ученика")
                            appendLine("Дата: ${templateLesson.dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}")
                            appendLine("Добавлен ученик: ${student.fio().hashtag()}")
                            appendLine("Предмет: $subject")
                            appendLine("Сумма: ${newLesson.amount} ₽")
                            appendLine()
                            append("Добавил: $tutorName")
                        }.trimEnd()

                        bot.sendTextMessage(
                            Config.logChat(),
                            text
                        )
                    } catch (e: Exception) {
                        KSLog.error("LessonsApi.POST: Error sending log", e)
                    }
                }

                AuditLogMapper.log(
                    userId = tutorId.id,
                    action = "ADD_STUDENT_TO_LESSON",
                    entityType = "lesson",
                    entityId = lessonId.id.toString(),
                    studentId = newStudentId.id,
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

                val lesson = lessons.first()
                val student = StudentMapper.findById(lesson.studentId)
                val subject = SubjectMapper.getNameById(lesson.subjectId)

                runBlocking {
                    try {
                        val bot = telegramBot(Config.getString("BOT_TOKEN"))
                        val tutorName = TutorMapper.findByIdOrNull(tutorId)?.name?.hashtag() ?: "Unknown"

                        val text = buildString {
                            appendLine("#удаление_занятия")
                            appendLine("Дата: ${lesson.dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))}")
                            appendLine("Ученик: ${student.fio().hashtag()}")
                            appendLine("Предмет: $subject")
                            appendLine("Сумма: ${lesson.amount} ₽")
                            appendLine()
                            append("Удалил: $tutorName")
                        }.trimEnd()

                        bot.sendTextMessage(
                            Config.logChat(),
                            text
                        )
                    } catch (e: Exception) {
                        KSLog.error("LessonsApi.DELETE: Error sending log", e)
                    }
                }

                AuditLogMapper.log(
                    userId = tutorId.id,
                    action = "DELETE_LESSON",
                    entityType = "lesson",
                    entityId = lessonId.id.toString(),
                    studentId = lesson.studentId.id,
                    subjectId = lesson.subjectId.id.toInt(),
                    lesson,
                    null
                )

            KSLog.info("LessonsApi.DELETE: User ${tutorId.id} deleted lesson $lessonIdStr")
            call.respond(HttpStatusCode.OK, mapOf("success" to true))
        }
        }
    }
}
