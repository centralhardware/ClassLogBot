package me.centralhardware.znatoki.telegram.statistic.web

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.browser.window
import kotlinx.serialization.json.Json
import me.centralhardware.znatoki.telegram.statistic.dto.*

class AccessDeniedException(message: String) : Exception(message)

object ApiClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private fun getAuthHeaders(): Map<String, String> {
        val tg = window.asDynamic().Telegram.WebApp
        val initData = tg.initData as? String ?: ""
        return mapOf("Authorization" to "tma $initData")
    }

    suspend fun getUserInfo(): CurrentUserDto {
        val response = client.get("/api/me") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }
        
        if (response.status.value == 403) {
            throw AccessDeniedException("У вас нет доступа к веб-интерфейсу")
        }
        
        return response.body()
    }

    suspend fun getTutors(): List<TutorDto> {
        return client.get("/api/tutors") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()
    }

    suspend fun getTodayLessons(): List<LessonDto> {
        return client.get("/api/lessons") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()
    }

    suspend fun getTodayPayments(): List<PaymentDto> {
        return client.get("/api/payments") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()
    }

    suspend fun getReport(subjectId: Long, period: String, tutorId: Long? = null): ReportDto {
        return client.get("/api/report/$subjectId/$period") {
            tutorId?.let { parameter("tutorId", it) }
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()
    }

    suspend fun getStatistics(period: String, subject: String?, tutorId: Long? = null): StatisticsDto {
        val url = if (subject != null) {
            "/api/report/$subject/aggregated/$period"
        } else {
            "/api/report/aggregated/$period"
        }
        return client.get(url) {
            tutorId?.let { parameter("tutorId", it) }
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()
    }

    suspend fun searchStudents(query: String): List<StudentDto> {
        return client.get("/api/students/search") {
            parameter("q", query)
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()
    }

    suspend fun getStudent(id: Long): StudentDto {
        return client.get("/api/students/$id") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()
    }

    suspend fun createLesson(lesson: CreateLessonRequest): LessonDto {
        return client.post("/api/lessons") {
            contentType(ContentType.Application.Json)
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
            setBody(lesson)
        }.body()
    }

    suspend fun updateLesson(id: String, lesson: UpdateLessonRequest): LessonDto {
        return client.put("/api/lessons/$id") {
            contentType(ContentType.Application.Json)
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
            setBody(lesson)
        }.body()
    }

    suspend fun deleteLesson(id: String) {
        client.delete("/api/lessons/$id") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }
    }

    suspend fun createPayment(payment: CreatePaymentRequest): PaymentDto {
        return client.post("/api/payments") {
            contentType(ContentType.Application.Json)
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
            setBody(payment)
        }.body()
    }

    suspend fun updatePayment(id: Int, payment: UpdatePaymentRequest): PaymentDto {
        return client.put("/api/payments/$id") {
            contentType(ContentType.Application.Json)
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
            setBody(payment)
        }.body()
    }

    suspend fun deletePayment(id: Int) {
        client.delete("/api/payments/$id") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }
    }

    suspend fun createStudent(student: UpdateStudentRequest): StudentDto {
        return client.post("/api/students") {
            contentType(ContentType.Application.Json)
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
            setBody(student)
        }.body()
    }

    suspend fun updateStudent(id: Long, student: UpdateStudentRequest): StudentDto {
        return client.put("/api/students/$id") {
            contentType(ContentType.Application.Json)
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
            setBody(student)
        }.body()
    }

    suspend fun getTeachers(): List<TutorDto> {
        return client.get("/api/tutors") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()
    }

    suspend fun updateTeacher(id: Long, tutor: UpdateTutorRequest): TutorDto {
        return client.put("/api/tutors/$id") {
            contentType(ContentType.Application.Json)
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
            setBody(tutor)
        }.body()
    }

    suspend fun getAuditLog(offset: Int, limit: Int): List<AuditLogEntryDto> {
        return client.get("/api/audit-log") {
            parameter("offset", offset)
            parameter("limit", limit)
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()
    }

    suspend fun checkVersion(): String {
        val response: Map<String, String> = client.get("/api/version") {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()
        return response["hash"] ?: ""
    }

    suspend fun uploadImage(file: org.w3c.files.File): String {
        val fileBytes = file.readAsArrayBuffer()
        
        val response: ImageUploadResponse = client.submitFormWithBinaryData(
            url = "/api/image/upload",
            formData = formData {
                append("file", fileBytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"${file.name}\"")
                    append(HttpHeaders.ContentType, file.type.ifEmpty { "image/jpeg" })
                })
            }
        ) {
            getAuthHeaders().forEach { (key, value) ->
                header(key, value)
            }
        }.body()
        
        return response.imageUrl
    }
    
    private suspend fun org.w3c.files.File.readAsArrayBuffer(): ByteArray = 
        kotlin.coroutines.suspendCoroutine { continuation ->
            val reader = org.w3c.files.FileReader()
            reader.onload = { event ->
                val arrayBuffer = (event.target as org.w3c.files.FileReader).result
                val uint8Array = js("new Uint8Array(arrayBuffer)").unsafeCast<ByteArray>()
                continuation.resumeWith(Result.success(uint8Array))
            }
            reader.onerror = { _ ->
                continuation.resumeWith(Result.failure(Exception("Failed to read file")))
            }
            reader.readAsArrayBuffer(this)
        }
}

data class AppState(
    val isAdmin: Boolean,
    val currentTutorId: Long?,
    val userPermissions: List<String>,
    val tutors: List<TutorDto>,
    val subjects: List<SubjectDto>,
    val todayLessons: List<LessonDto> = emptyList(),
    val todayPayments: List<PaymentDto> = emptyList()
)

suspend fun loadInitialData(log: (String) -> Unit): AppState {
    log("Загрузка информации о пользователе...")
    val userInfo = ApiClient.getUserInfo()
    log("Информация о пользователе загружена")

    val tutors = if (userInfo.isAdmin) {
        log("Загрузка списка учителей...")
        val tutorsList = ApiClient.getTutors()
        log("Режим администратора активен")
        tutorsList
    } else {
        log("Стандартный режим")
        emptyList()
    }

    log("Загрузка данных за сегодня...")
    val todayLessons = try {
        ApiClient.getTodayLessons()
    } catch (e: Exception) {
        emptyList()
    }

    val todayPayments = try {
        ApiClient.getTodayPayments()
    } catch (e: Exception) {
        emptyList()
    }
    log("Данные загружены")

    log("Настройка интерфейса...")

    return AppState(
        isAdmin = userInfo.isAdmin,
        currentTutorId = userInfo.tutorId,
        userPermissions = userInfo.permissions,
        tutors = tutors,
        subjects = userInfo.subjects,
        todayLessons = todayLessons,
        todayPayments = todayPayments
    )
}
