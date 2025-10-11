package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import me.centralhardware.znatoki.telegram.statistic.exception.AppException

object WebServer {

    fun start(port: Int = 8080) {
        embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }

            install(CORS) {
                anyHost()
                allowHeader(HttpHeaders.ContentType)
                allowHeader(HttpHeaders.Authorization)
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Put)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Delete)
            }

            install(TelegramAuthPlugin)

            install(StatusPages) {
                exception<AppException> { call, cause ->
                    KSLog.error("WebServer: Application exception: ${cause.message}", cause)
                    call.respond(
                        cause.statusCode,
                        mapOf("error" to cause.message)
                    )
                }
                exception<IllegalArgumentException> { call, cause ->
                    KSLog.error("WebServer: Validation error: ${cause.message}", cause)
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid input")
                    )
                }
                exception<Throwable> { call, cause ->
                    KSLog.error("WebServer: Unexpected error", cause)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Internal server error")
                    )
                }
            }

            routing {
                tutorApi()
                subjectApi()
                studentApi()
                reportApi()
                lessonApi()
                paymentApi()
                imageApi()
                versionApi()
                auditLogApi()

                // Serve static files
                staticResources("/", "static")
            }
        }.start(wait = false)
    }
}
