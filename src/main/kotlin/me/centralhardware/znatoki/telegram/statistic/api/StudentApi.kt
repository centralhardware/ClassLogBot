package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.utils.TelegramAPIUrlsKeeper
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.extensions.hasClientPermission
import me.centralhardware.znatoki.telegram.statistic.extensions.hasReadRight
import me.centralhardware.znatoki.telegram.statistic.extensions.hashtag
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.StudentService
import me.centralhardware.znatoki.telegram.statistic.mapper.AuditLogMapper
import me.centralhardware.znatoki.telegram.statistic.service.DiffService
import me.centralhardware.znatoki.telegram.statistic.exception.*
import java.time.LocalDate
import kotlinx.coroutines.runBlocking

@Serializable
data class StudentDto(
    val id: Int,
    val name: String,
    val secondName: String,
    val lastName: String,
    val schoolClass: Int? = null,
    val recordDate: String? = null,
    val birthDate: String? = null,
    val source: String? = null,
    val phone: String? = null,
    val responsiblePhone: String? = null,
    val motherFio: String? = null
)

@Serializable
data class UpdateStudentRequest(
    val name: String,
    val secondName: String,
    val lastName: String,
    val schoolClass: Int? = null,
    val recordDate: String? = null,
    val birthDate: String? = null,
    val source: String? = null,
    val phone: String? = null,
    val responsiblePhone: String? = null,
    val motherFio: String? = null
)

fun Student.toDto() = StudentDto(
    id = id.id,
    name = name,
    secondName = secondName,
    lastName = lastName,
    schoolClass = schoolClass?.value,
    recordDate = recordDate?.toString(),
    birthDate = birthDate?.toString(),
    source = source?.title,
    phone = phone?.value,
    responsiblePhone = responsiblePhone?.value,
    motherFio = motherFio
)

// Валидация данных Telegram Web App используя встроенную функцию ktgbotapi
fun validateTelegramWebAppData(initData: String, botToken: String): Map<String, String>? {
    try {
        val params = initData.split("&")
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0] to (parts.getOrNull(1) ?: "")
            }

        val hash = params["hash"] ?: return null

        // Используем встроенную валидацию из ktgbotapi
        val telegramUrlsKeeper = TelegramAPIUrlsKeeper(botToken)

        if (!telegramUrlsKeeper.checkWebAppData(initData, hash)) {
            return null
        }

        return params
    } catch (e: Exception) {
        KSLog.error("validateTelegramWebAppData: Exception during validation", e)
        return null
    }
}

// Извлечение и валидация пользователя из Telegram Web App данных
fun extractAndValidateTelegramUser(
    authHeader: String?,
    studentId: Int,
    operation: String
): TutorId? {
    if (authHeader == null) {
        return null
    }

    val initData = authHeader.removePrefix("tma ")
    val botToken = me.centralhardware.znatoki.telegram.statistic.Config.getString("BOT_TOKEN")
    val validatedData = validateTelegramWebAppData(initData, botToken)

    if (validatedData == null) {
        KSLog.warning("StudentApi.$operation: Invalid Telegram authorization attempt for student $studentId")
        return null
    }

    val userJson = validatedData["user"]
    if (userJson == null) {
        KSLog.warning("StudentApi.$operation: User data not found in Telegram initData for student $studentId")
        return null
    }

    val userId = try {
        val decodedUserJson = java.net.URLDecoder.decode(userJson, "UTF-8")
        Json.parseToJsonElement(decodedUserJson)
            .jsonObject["id"]
            ?.jsonPrimitive?.content?.toLong()
    } catch (e: Exception) {
        KSLog.error("StudentApi.$operation: Error parsing user ID", e)
        null
    }

    if (userId == null) {
        KSLog.warning("StudentApi.$operation: Invalid user data in Telegram initData for student $studentId")
        return null
    }

    return TutorId(userId)
}

// Отправка лога об изменении студента
fun sendStudentEditLog(oldStudent: Student, newStudent: Student, updatedBy: TutorId) {
    runBlocking {
        try {
            val bot = telegramBot(me.centralhardware.znatoki.telegram.statistic.Config.getString("BOT_TOKEN"))
            val tutorName = TutorMapper.findByIdOrNull(updatedBy)?.name?.hashtag() ?: "Unknown"

            val changes = mutableListOf<String>()

            if (oldStudent.name != newStudent.name) {
                changes.add("Имя: ${oldStudent.name} → ${newStudent.name}")
            }
            if (oldStudent.secondName != newStudent.secondName) {
                changes.add("Фамилия: ${oldStudent.secondName} → ${newStudent.secondName}")
            }
            if (oldStudent.lastName != newStudent.lastName) {
                changes.add("Отчество: ${oldStudent.lastName} → ${newStudent.lastName}")
            }
            if (oldStudent.schoolClass != newStudent.schoolClass) {
                changes.add("Класс: ${oldStudent.schoolClass?.value ?: "не указан"} → ${newStudent.schoolClass?.value ?: "не указан"}")
            }
            if (oldStudent.birthDate != newStudent.birthDate) {
                changes.add("Дата рождения: ${oldStudent.birthDate ?: "не указана"} → ${newStudent.birthDate ?: "не указана"}")
            }
            if (oldStudent.source != newStudent.source) {
                changes.add("Источник: ${oldStudent.source?.title ?: "не указан"} → ${newStudent.source?.title ?: "не указан"}")
            }
            if (oldStudent.phone != newStudent.phone) {
                changes.add("Телефон: ${oldStudent.phone?.value ?: "не указан"} → ${newStudent.phone?.value ?: "не указан"}")
            }
            if (oldStudent.responsiblePhone != newStudent.responsiblePhone) {
                changes.add("Телефон ответственного: ${oldStudent.responsiblePhone?.value ?: "не указан"} → ${newStudent.responsiblePhone?.value ?: "не указан"}")
            }
            if (oldStudent.motherFio != newStudent.motherFio) {
                changes.add("ФИО матери: ${oldStudent.motherFio ?: "не указано"} → ${newStudent.motherFio ?: "не указано"}")
            }
            if (oldStudent.recordDate != newStudent.recordDate) {
                changes.add("Дата записи: ${oldStudent.recordDate ?: "не указана"} → ${newStudent.recordDate ?: "не указана"}")
            }
            if (oldStudent.createdBy != newStudent.createdBy) {
                val oldCreator = TutorMapper.findByIdOrNull(oldStudent.createdBy)?.name?.hashtag() ?: "Unknown"
                val newCreator = TutorMapper.findByIdOrNull(newStudent.createdBy)?.name?.hashtag() ?: "Unknown"
                changes.add("Создал: $oldCreator → $newCreator")
            }
            if (oldStudent.updateBy != newStudent.updateBy) {
                val oldUpdater =
                    oldStudent.updateBy?.let { TutorMapper.findByIdOrNull(it)?.name?.hashtag() } ?: "не указан"
                val newUpdater =
                    newStudent.updateBy?.let { TutorMapper.findByIdOrNull(it)?.name?.hashtag() } ?: "не указан"
                changes.add("Последний раз изменил: $oldUpdater → $newUpdater")
            }

            if (changes.isEmpty()) {
                return@runBlocking
            }

            val createdByName = TutorMapper.findByIdOrNull(newStudent.createdBy)?.name?.hashtag() ?: "Unknown"
            val updatedByName =
                newStudent.updateBy?.let { TutorMapper.findByIdOrNull(it)?.name?.hashtag() } ?: "не указан"

            val text = buildString {
                appendLine("#редактирование_ученика")
                appendLine("Ученик: ${newStudent.fio().hashtag()}")
                appendLine("ID: ${newStudent.id.id}")
                appendLine("Создан: ${newStudent.createDate} ($createdByName)")
                newStudent.modifyDate?.let { appendLine("Изменён: $it ($updatedByName)") }
                appendLine()
                appendLine("Изменения:")
                changes.forEach { appendLine("• $it") }
                appendLine()
                append("Изменил: $tutorName")
            }.trimEnd()

            bot.sendTextMessage(
                me.centralhardware.znatoki.telegram.statistic.Config.logChat(),
                text
            )
        } catch (e: Exception) {
            KSLog.error("sendStudentEditLog: Error sending log", e)
        }
    }
}

@Serializable
data class StudentLessonDto(
    val id: String,
    val dateTime: String,
    val subjectName: String,
    val amount: Double,
    val forceGroup: Boolean,
    val extraHalfHour: Boolean
)

@Serializable
data class StudentPaymentDto(
    val id: Int,
    val dateTime: String,
    val amount: Int
)

@Serializable
data class StudentDetailsDto(
    val lessons: List<StudentLessonDto>,
    val payments: List<StudentPaymentDto>
)

fun Route.studentApi() {
    route("/api/student") {

        get("/{id}/details") {
            val id = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("Invalid student ID")
            val period = call.request.queryParameters["period"] ?: throw BadRequestException("Period is required")
            val subjectId = call.request.queryParameters["subjectId"]?.toIntOrNull()
            
            KSLog.info("StudentApi.GET /details: Received request for student $id with period: $period, subjectId: $subjectId")
            
            val tutorId = call.authenticatedTutorId
            val studentId = id.toStudentId()

            // Parse period (format: YYYY-MM)
            val (year, month) = period.split("-").map { it.toInt() }
            val periodStart = java.time.LocalDateTime.of(year, month, 1, 0, 0, 0)
            val periodEnd = periodStart.plusMonths(1)

            // Get lessons for student in period (and optionally filtered by subject)
            val lessons =
                me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper.findAllByStudent(studentId)
                    .filter { it.dateTime >= periodStart && it.dateTime < periodEnd }
                    .filter { subjectId == null || it.subjectId.id == subjectId.toLong() }
                    .groupBy { it.id }
                    .map { (lessonId, lessons) ->
                        val firstLesson = lessons.first()
                        StudentLessonDto(
                            id = lessonId.id.toString(),
                            dateTime = firstLesson.dateTime.format(
                                java.time.format.DateTimeFormatter.ofPattern(
                                    "dd.MM.yyyy HH:mm"
                                )
                            ),
                            subjectName = me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper.getNameById(
                                firstLesson.subjectId
                            ),
                            amount = firstLesson.amount,
                            forceGroup = firstLesson.forceGroup,
                            extraHalfHour = firstLesson.extraHalfHour
                        )
                    }
                    .sortedByDescending { it.dateTime }

            // Get payments for student in period (and optionally filtered by subject)
            val payments =
                me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper.findAllByStudent(studentId)
                    .filter { it.dateTime >= periodStart && it.dateTime < periodEnd }
                    .filter { subjectId == null || it.subjectId.id == subjectId.toLong() }
                    .map { payment ->
                        StudentPaymentDto(
                            id = payment.id?.id ?: 0,
                            dateTime = payment.dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                            amount = payment.amount.amount
                        )
                    }
                    .sortedByDescending { it.dateTime }

            KSLog.info("StudentApi.GET /details: User ${tutorId.id} loaded details for student $id for period $period")
            call.respond(StudentDetailsDto(lessons = lessons, payments = payments))
        }

        get("/search") {
            val query = call.request.queryParameters["q"]
            val tutorId = call.authenticatedTutorId
            
            val students = if (query.isNullOrEmpty() || query.length < 2) {
                // Return all students sorted by class and name
                StudentService.getAllActive()
            } else {
                StudentService.search(query.lowercase())
            }

            val sortedStudents = students
                .map { it.toDto() }
                .sortedWith(
                    compareBy(
                    { it.schoolClass ?: Int.MAX_VALUE },
                    { it.secondName },
                    { it.name },
                    { it.lastName }
                ))

            KSLog.info("StudentApi.GET /search: User ${tutorId.id} searched for '${query ?: "all"}', found ${sortedStudents.size} students")
            call.respond(sortedStudents)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("Invalid student ID")
            val tutorId = call.authenticatedTutorId
            
            val student = StudentMapper.findById(id.toStudentId())
            KSLog.info("StudentApi.GET: User ${tutorId.id} loaded student $id")
            call.respond(student.toDto())
        }

        put("/{id}") {
            requires(Permissions.ADD_CLIENT)
            val id = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("Invalid student ID")
            val tutorId = call.authenticatedTutorId
            val request = call.receive<UpdateStudentRequest>()

            // Валидация
            if (request.name.isBlank() || request.secondName.isBlank() || request.lastName.isBlank()) {
                throw ValidationException("Name, secondName and lastName are required")
            }

            if (request.schoolClass != null && !SchoolClass.validate(request.schoolClass)) {
                throw ValidationException("Invalid school class")
            }

            if (request.phone != null && !PhoneNumber.validate(request.phone)) {
                throw ValidationException("Invalid phone number")
            }

            if (request.responsiblePhone != null && !PhoneNumber.validate(request.responsiblePhone)) {
                throw ValidationException("Invalid responsible phone number")
            }

            val existingStudent = StudentMapper.findById(id.toStudentId())

            val updatedStudent = Student(
                id = existingStudent.id,
                name = request.name,
                secondName = request.secondName,
                lastName = request.lastName,
                schoolClass = request.schoolClass?.let { SchoolClass(it) },
                recordDate = request.recordDate?.let { LocalDate.parse(it) },
                birthDate = request.birthDate?.let { LocalDate.parse(it) },
                source = request.source?.let { SourceOption.fromTitle(it) },
                phone = request.phone?.let { PhoneNumber(it) },
                responsiblePhone = request.responsiblePhone?.let { PhoneNumber(it) },
                motherFio = request.motherFio,
                createDate = existingStudent.createDate,
                modifyDate = java.time.LocalDateTime.now(),
                createdBy = existingStudent.createdBy,
                updateBy = tutorId
            )

            val changesMap = buildMap<String, Pair<String?, String?>> {
                if (existingStudent.name != updatedStudent.name)
                    put("name", existingStudent.name to updatedStudent.name)
                if (existingStudent.secondName != updatedStudent.secondName)
                    put("secondName", existingStudent.secondName to updatedStudent.secondName)
                if (existingStudent.lastName != updatedStudent.lastName)
                    put("lastName", existingStudent.lastName to updatedStudent.lastName)
                if (existingStudent.schoolClass != updatedStudent.schoolClass)
                    put("schoolClass", existingStudent.schoolClass?.value?.toString() to updatedStudent.schoolClass?.value?.toString())
                if (existingStudent.phone != updatedStudent.phone)
                    put("phone", existingStudent.phone?.value to updatedStudent.phone?.value)
                if (existingStudent.responsiblePhone != updatedStudent.responsiblePhone)
                    put("responsiblePhone", existingStudent.responsiblePhone?.value to updatedStudent.responsiblePhone?.value)
                if (existingStudent.motherFio != updatedStudent.motherFio)
                    put("motherFio", existingStudent.motherFio to updatedStudent.motherFio)
                if (existingStudent.birthDate != updatedStudent.birthDate)
                    put("birthDate", existingStudent.birthDate?.toString() to updatedStudent.birthDate?.toString())
                if (existingStudent.source != updatedStudent.source)
                    put("source", existingStudent.source?.title to updatedStudent.source?.title)
                if (existingStudent.recordDate != updatedStudent.recordDate)
                    put("recordDate", existingStudent.recordDate?.toString() to updatedStudent.recordDate?.toString())
            }

            // Only save and log if there are actual changes
            if (changesMap.isNotEmpty()) {
                StudentMapper.update(updatedStudent)

                val htmlDiff = DiffService.generateHtmlDiff(changesMap)

                AuditLogMapper.log(
                    userId = tutorId.id,
                    action = "UPDATE_STUDENT",
                    entityType = "student",
                    entityId = id,
                    details = htmlDiff,
                    studentId = id,
                    subjectId = null
                )

                KSLog.info("StudentApi.PUT: User ${tutorId.id} updated student $id")
                sendStudentEditLog(existingStudent, updatedStudent, tutorId)
            } else {
                KSLog.info("StudentApi.PUT: User ${tutorId.id} attempted to update student $id with no changes")
            }

            call.respond(HttpStatusCode.OK, updatedStudent.toDto())
        }
    }
}
