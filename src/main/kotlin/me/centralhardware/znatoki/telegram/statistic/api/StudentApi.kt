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
        KSLog.info("validateTelegramWebAppData: Starting validation")
        val params = initData.split("&")
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0] to (parts.getOrNull(1) ?: "")
            }

        val hash = params["hash"]
        if (hash == null) {
            KSLog.warning("validateTelegramWebAppData: No hash found in params")
            return null
        }

        // Используем встроенную валидацию из ktgbotapi
        val telegramUrlsKeeper = TelegramAPIUrlsKeeper(botToken)

        val isValid = telegramUrlsKeeper.checkWebAppData(initData, hash)
        KSLog.info("validateTelegramWebAppData: Validation result: $isValid")

        if (!isValid) {
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
            KSLog.info("StudentApi.GET: Request for student ID: $id")

            if (id == null) {
                KSLog.warning("StudentApi.GET: Invalid student ID in request")
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid student ID"))
                return@get
            }

            // Проверяем авторизацию через Telegram
            val authHeader = call.request.headers["Authorization"]
            KSLog.info("StudentApi.GET: Authorization header present: ${authHeader != null}")

            if (authHeader == null) {
                KSLog.warning("StudentApi.GET: No authorization header")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authorization required"))
                return@get
            }

            val initData = authHeader.removePrefix("tma ")
            val botToken = me.centralhardware.znatoki.telegram.statistic.Config.getString("BOT_TOKEN")
            val validatedData = validateTelegramWebAppData(initData, botToken)

            if (validatedData == null) {
                KSLog.warning("StudentApi.GET: Invalid Telegram authorization")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid Telegram authorization"))
                return@get
            }

            // Получаем user из данных Telegram
            val userJson = validatedData["user"]
            if (userJson == null) {
                KSLog.warning("StudentApi.GET: User data not found in validated data")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User data not found"))
                return@get
            }

            KSLog.info("StudentApi.GET: User JSON: $userJson")

            val userId = try {
                // userJson - это URL-encoded JSON строка, нужно сначала декодировать
                val decodedUserJson = java.net.URLDecoder.decode(userJson, "UTF-8")
                KSLog.info("StudentApi.GET: Decoded user JSON: $decodedUserJson")

                Json.parseToJsonElement(decodedUserJson)
                    .jsonObject["id"]
                    ?.jsonPrimitive?.content?.toLong()
            } catch (e: Exception) {
                KSLog.error("StudentApi.GET: Error parsing user ID", e)
                null
            }

            if (userId == null) {
                KSLog.warning("StudentApi.GET: Invalid user data")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid user data"))
                return@get
            }

            KSLog.info("StudentApi.GET: User ID from Telegram: $userId")

            val tutor = TutorMapper.findByIdOrNull(TutorId(userId))
            if (tutor == null) {
                KSLog.warning("StudentApi.GET: Tutor not found for user ID: $userId")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@get
            }

            if (!tutor.hasReadRight()) {
                KSLog.warning("StudentApi.GET: Tutor $userId has no read rights")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@get
            }

            try {
                val student = StudentMapper.findById(id.toStudentId())
                KSLog.info("StudentApi.GET: Successfully retrieved student $id")
                call.respond(student.toDto())
            } catch (e: IllegalArgumentException) {
                KSLog.warning("StudentApi.GET: Student not found: $id")
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Student not found"))
            } catch (e: Exception) {
                KSLog.error("StudentApi.GET: Error retrieving student $id", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            KSLog.info("StudentApi.PUT: Update request for student ID: $id")

            if (id == null) {
                KSLog.warning("StudentApi.PUT: Invalid student ID")
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid student ID"))
                return@put
            }

            // Проверяем авторизацию через Telegram
            val authHeader = call.request.headers["Authorization"]
            if (authHeader == null) {
                KSLog.warning("StudentApi.PUT: No authorization header")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Authorization required"))
                return@put
            }

            val initData = authHeader.removePrefix("tma ")
            val botToken = me.centralhardware.znatoki.telegram.statistic.Config.getString("BOT_TOKEN")
            val validatedData = validateTelegramWebAppData(initData, botToken)

            if (validatedData == null) {
                KSLog.warning("StudentApi.PUT: Invalid Telegram authorization")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid Telegram authorization"))
                return@put
            }

            // Получаем user из данных Telegram
            val userJson = validatedData["user"]
            if (userJson == null) {
                KSLog.warning("StudentApi.PUT: User data not found")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User data not found"))
                return@put
            }

            KSLog.info("StudentApi.PUT: User JSON: $userJson")

            val userId = try {
                // userJson - это URL-encoded JSON строка, нужно сначала декодировать
                val decodedUserJson = java.net.URLDecoder.decode(userJson, "UTF-8")
                KSLog.info("StudentApi.PUT: Decoded user JSON: $decodedUserJson")

                Json.parseToJsonElement(decodedUserJson)
                    .jsonObject["id"]
                    ?.jsonPrimitive?.content?.toLong()
            } catch (e: Exception) {
                KSLog.error("StudentApi.PUT: Error parsing user ID", e)
                null
            }

            if (userId == null) {
                KSLog.warning("StudentApi.PUT: Invalid user data")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid user data"))
                return@put
            }

            KSLog.info("StudentApi.PUT: User ID from Telegram: $userId")

            val tutor = TutorMapper.findByIdOrNull(TutorId(userId))
            if (tutor == null) {
                KSLog.warning("StudentApi.PUT: Tutor not found for user ID: $userId")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@put
            }

            if (!tutor.hasClientPermission()) {
                KSLog.warning("StudentApi.PUT: Tutor $userId has no client permission")
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Access denied"))
                return@put
            }

            try {
                val request = call.receive<UpdateStudentRequest>()
                KSLog.info("StudentApi.PUT: Received update request for student $id")

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
                KSLog.info("StudentApi.PUT: Successfully updated student $id by user $userId")
                call.respond(HttpStatusCode.OK, updatedStudent.toDto())
            } catch (e: IllegalArgumentException) {
                KSLog.warning("StudentApi.PUT: Student not found: $id")
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Student not found"))
            } catch (e: Exception) {
                KSLog.error("StudentApi.PUT: Error updating student $id", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}
