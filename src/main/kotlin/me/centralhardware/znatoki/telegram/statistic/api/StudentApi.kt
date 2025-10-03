package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.warning
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
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import java.time.LocalDate

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

fun Route.studentApi() {
    route("/api/student") {
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid student ID"))
                return@get
            }

            // Проверяем авторизацию через Telegram
            val authHeader = call.request.headers["Authorization"]
            if (authHeader == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authorization required"))
                return@get
            }

            val initData = authHeader.removePrefix("tma ")
            val botToken = me.centralhardware.znatoki.telegram.statistic.Config.getString("BOT_TOKEN")
            val validatedData = validateTelegramWebAppData(initData, botToken)

            if (validatedData == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid Telegram authorization"))
                return@get
            }

            // Получаем user из данных Telegram
            val userJson = validatedData["user"]
            if (userJson == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User data not found"))
                return@get
            }

            val userId = try {
                // userJson - это URL-encoded JSON строка, нужно сначала декодировать
                val decodedUserJson = java.net.URLDecoder.decode(userJson, "UTF-8")
                Json.parseToJsonElement(decodedUserJson)
                    .jsonObject["id"]
                    ?.jsonPrimitive?.content?.toLong()
            } catch (e: Exception) {
                KSLog.error("StudentApi.GET: Error parsing user ID", e)
                null
            }

            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid user data"))
                return@get
            }

            val tutor = TutorMapper.findByIdOrNull(TutorId(userId))
            if (tutor == null || !tutor.hasReadRight()) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@get
            }

            try {
                val student = StudentMapper.findById(id.toStudentId())
                KSLog.info("StudentApi.GET: User $userId loaded student $id")
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

            // Проверяем авторизацию через Telegram
            val authHeader = call.request.headers["Authorization"]
            if (authHeader == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authorization required"))
                return@put
            }

            val initData = authHeader.removePrefix("tma ")
            val botToken = me.centralhardware.znatoki.telegram.statistic.Config.getString("BOT_TOKEN")
            val validatedData = validateTelegramWebAppData(initData, botToken)

            if (validatedData == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid Telegram authorization"))
                return@put
            }

            // Получаем user из данных Telegram
            val userJson = validatedData["user"]
            if (userJson == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User data not found"))
                return@put
            }

            val userId = try {
                // userJson - это URL-encoded JSON строка, нужно сначала декодировать
                val decodedUserJson = java.net.URLDecoder.decode(userJson, "UTF-8")
                Json.parseToJsonElement(decodedUserJson)
                    .jsonObject["id"]
                    ?.jsonPrimitive?.content?.toLong()
            } catch (e: Exception) {
                KSLog.error("StudentApi.PUT: Error parsing user ID", e)
                null
            }

            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid user data"))
                return@put
            }

            val tutor = TutorMapper.findByIdOrNull(TutorId(userId))
            if (tutor == null || !tutor.hasClientPermission()) {
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
                    updateBy = TutorId(userId)
                )

                StudentMapper.update(updatedStudent)
                KSLog.info("StudentApi.PUT: User $userId updated student $id")
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
