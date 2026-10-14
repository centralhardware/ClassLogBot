package me.centralhardware.znatoki.telegram.statistic.webapp

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.window
import kotlinx.serialization.json.Json
import org.khronos.webgl.Int8Array
import org.w3c.files.Blob

/** Telegram's Mini App object (telegram-web-app.js), typed only as far as the app uses it. */
external interface TgWebApp {
    val initData: String
    val platform: String
    val colorScheme: String
    val version: String
    val BackButton: TgButton
    val HapticFeedback: dynamic
    fun ready()
    fun expand()
    fun close()
    fun openLink(url: String)
    fun isVersionAtLeast(v: String): Boolean
    fun downloadFile(params: dynamic, callback: (Boolean) -> Unit)
}

external interface TgButton {
    fun show()
    fun hide()
    fun onClick(cb: () -> Unit)
}

val tg: TgWebApp? get() = window.asDynamic().Telegram?.WebApp.unsafeCast<TgWebApp?>()

class ApiError(message: String, val status: Int, val code: String? = null) : Exception(message)

/** The app's REST client: JSON in and out, Telegram's initData as the credential on every request. */
object Api {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
    private val client = HttpClient { install(ContentNegotiation) { json(json) }; expectSuccess = false }
    /** Telegram's signed launch data; the server checks it on every request. */
    val initData: String get() = tg?.initData.orEmpty()

    suspend fun send(method: HttpMethod, path: String, configure: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        val res = try {
            client.request(path) {
                this.method = method
                header(HttpHeaders.Authorization, "tma $initData")
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
            HttpClient { install(ContentNegotiation) { json(json) }; expectSuccess = false }.submitFormWithBinaryData(
                url = path,
                formData = formData {
                    append("data", data)
                    append("photo", photo, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"photo.jpg\"")
                    })
                },
            ) { header(HttpHeaders.Authorization, "tma $initData") }
        } catch (e: Throwable) {
            throw ApiError("Нет связи с сервером. Проверьте интернет и попробуйте ещё раз.", 0)
        }
        if (!res.status.isSuccess()) throw ApiError(runCatching { res.body<ErrorDto>() }.getOrNull()?.error ?: "Ошибка ${res.status.value}", res.status.value)
        return res.body()
    }
}

fun photoUrl(kind: String, id: String) = "/api/photo/$kind/$id?tma=" + js("encodeURIComponent")(Api.initData) as String

/** Opens a generated file: Telegram's downloadFile where available, the browser otherwise. */
fun download(d: DownloadDto) {
    val url = window.location.origin + d.url
    val t = tg
    if (t != null && t.isVersionAtLeast("8.0")) {
        val p: dynamic = js("({})"); p.url = url; p.file_name = d.name
        t.downloadFile(p) { ok -> if (ok) Toasts.show("Загрузка началась") }
    } else if (t != null) t.openLink(url) else window.location.href = url
}

@Suppress("UNUSED_PARAMETER")
fun blobToBytes(blob: Blob, done: (ByteArray) -> Unit) {
    blob.asDynamic().arrayBuffer().then { buf: dynamic -> val arr: Int8Array = Int8Array(buf.unsafeCast<org.khronos.webgl.ArrayBuffer>()); done(arr.unsafeCast<ByteArray>()) }
}
