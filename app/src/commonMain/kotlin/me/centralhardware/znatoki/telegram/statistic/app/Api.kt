package me.centralhardware.znatoki.telegram.statistic.app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiError(message: String, val status: Int, val code: String? = null) : Exception(message)

val tg: Telegram? get() = platform.telegram

/**
 * The sign-in outside Telegram (browser, Android app) as a full credential, `tgb …` from the bot sign-in
 * (older clients kept bare Login Widget data, read as `tlw …`). Kept per device; the server accepts it for 30 days.
 */
object Login {
    private const val KEY = "tg-login"
    var data: String
        get() = platform.load(KEY).orEmpty()
            .let { if (it.isEmpty() || it.startsWith("tgb ") || it.startsWith("tlw ")) it else "tlw $it" }
        set(v) = platform.save(KEY, v.ifEmpty { null })
}

/** The app's REST client: JSON in and out, a Telegram credential on every request. */
object Api {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
    val client = HttpClient { install(ContentNegotiation) { json(json) }; expectSuccess = false }
    val initData: String get() = tg?.initData.orEmpty()

    /** `tma <initData>` inside Telegram, the stored sign-in outside it, empty when not signed in. */
    val credential: String get() = if (initData.isNotEmpty()) "tma $initData" else Login.data

    suspend fun send(method: HttpMethod, path: String, configure: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        val res = try {
            client.request(platform.apiBase + path) {
                this.method = method
                header(HttpHeaders.Authorization, credential)
                configure(this)
            }
        } catch (e: Throwable) {
            throw ApiError("Нет связи с сервером. Проверьте интернет и попробуйте ещё раз.", 0)
        }
        if (!res.status.isSuccess()) {
            val err = runCatching { res.body<ErrorDto>() }.getOrNull()
            throw ApiError(err?.error ?: "Ошибка ${res.status.value}", res.status.value, err?.code)
        }
        return res
    }

    suspend inline fun <reified T> get(path: String): T = send(HttpMethod.Get, path).body()

    suspend inline fun <reified T, reified B> post(path: String, body: B, method: HttpMethod = HttpMethod.Post): T =
        send(method, path) { setBody(io.ktor.http.content.TextContent(json.encodeToString(kotlinx.serialization.serializer<B>(), body), ContentType.Application.Json)) }.body()

    suspend inline fun <reified T> postEmpty(path: String, method: HttpMethod = HttpMethod.Post): T = send(method, path).body()

    /** Multipart form: a JSON `data` field plus the `photo` file, as the lesson and payment endpoints expect. */
    suspend inline fun <reified T> postForm(path: String, data: String, photo: ByteArray): T {
        val res = try {
            client.submitFormWithBinaryData(
                url = platform.apiBase + path,
                formData = formData {
                    append("data", data)
                    append("photo", photo, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"photo.jpg\"")
                    })
                },
            ) { header(HttpHeaders.Authorization, credential) }
        } catch (e: Throwable) {
            throw ApiError("Нет связи с сервером. Проверьте интернет и попробуйте ещё раз.", 0)
        }
        if (!res.status.isSuccess()) throw ApiError(runCatching { res.body<ErrorDto>() }.getOrNull()?.error ?: "Ошибка ${res.status.value}", res.status.value)
        return res.body()
    }

    /** A stored photo's bytes (lesson report, payment receipt). */
    suspend fun photo(kind: String, id: String): ByteArray {
        val (param, value) = credential.split(" ", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        val res = client.get("${platform.apiBase}/api/photo/$kind/$id?$param=${value.encodeURLParameter()}")
        if (!res.status.isSuccess()) throw ApiError("Фото не загрузилось", res.status.value)
        return res.readRawBytes()
    }
}

/** Opens a generated file: Telegram's downloadFile where available, the platform's own download otherwise. */
fun download(d: DownloadDto) {
    val url = platform.apiBase + d.url
    val t = tg
    when {
        t != null && t.downloadFile(url, d.name) { Toasts.show("Загрузка началась") } -> Unit
        t != null -> t.openLink(url)
        else -> platform.download(url, d.name)
    }
}
