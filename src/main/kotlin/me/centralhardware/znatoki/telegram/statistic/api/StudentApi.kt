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

            if (changes.isEmpty()) {
                return@runBlocking
            }

            val text = buildString {
                appendLine("#редактирование_ученика")
                appendLine("Ученик: ${newStudent.fio().hashtag()}")
                appendLine("ID: ${newStudent.id.id}")
                appendLine()
                appendLine("Изменения:")
                changes.forEach { appendLine("• $it") }
                appendLine()
                append("Изменил: $tutorName")
            }.trimEnd()

            sendTextMessage(
                bot,
                me.centralhardware.znatoki.telegram.statistic.Config.logChat(),
                text
            )
        } catch (e: Exception) {
            KSLog.error("sendStudentEditLog: Error sending log", e)
        }
    }
}

fun Route.studentApi() {
    route("/api/student") {
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid student ID"))
                return@get
            }

            val tutorId = extractAndValidateTelegramUser(
                call.request.headers["Authorization"],
                id,
                "GET"
            )

            if (tutorId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authorization required"))
                return@get
            }

            val tutor = TutorMapper.findByIdOrNull(tutorId)
            if (tutor == null) {
                KSLog.warning("StudentApi.GET: Unknown user ${tutorId.id} attempted to access student $id")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@get
            }

            if (!tutor.hasReadRight()) {
                KSLog.warning("StudentApi.GET: User ${tutorId.id} without read rights attempted to access student $id")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@get
            }

            try {
                val student = StudentMapper.findById(id.toStudentId())
                KSLog.info("StudentApi.GET: User ${tutorId.id} loaded student $id")
                call.respond(student.toDto())
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Student not found"))
            } catch (e: Exception) {
                KSLog.error("StudentApi.GET: Error retrieving student $id", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid student ID"))
                return@put
            }

            val tutorId = extractAndValidateTelegramUser(
                call.request.headers["Authorization"],
                id,
                "PUT"
            )

            if (tutorId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authorization required"))
                return@put
            }

            val tutor = TutorMapper.findByIdOrNull(tutorId)
            if (tutor == null) {
                KSLog.warning("StudentApi.PUT: Unknown user ${tutorId.id} attempted to update student $id")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@put
            }

            if (!tutor.hasClientPermission()) {
                KSLog.warning("StudentApi.PUT: User ${tutorId.id} without client permission attempted to update student $id")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@put
            }

            try {
                val request = call.receive<UpdateStudentRequest>()

                // Валидация
                if (request.name.isBlank() || request.secondName.isBlank() || request.lastName.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Name, secondName and lastName are required"))
                    return@put
                }

                if (request.schoolClass != null && !SchoolClass.validate(request.schoolClass)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid school class"))
                    return@put
                }

                if (request.phone != null && !PhoneNumber.validate(request.phone)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid phone number"))
                    return@put
                }

                if (request.responsiblePhone != null && !PhoneNumber.validate(request.responsiblePhone)) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid responsible phone number"))
                    return@put
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

                StudentMapper.update(updatedStudent)
                KSLog.info("StudentApi.PUT: User ${tutorId.id} updated student $id")
                sendStudentEditLog(existingStudent, updatedStudent, tutorId)
                call.respond(HttpStatusCode.OK, updatedStudent.toDto())
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Student not found"))
            } catch (e: Exception) {
                KSLog.error("StudentApi.PUT: Error updating student $id", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
