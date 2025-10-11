package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import me.centralhardware.znatoki.telegram.statistic.exception.AppException

object WebServer {

    fun start(port: Int = 8080) {
        embeddedServer(Netty, port = port) {
            install(CallLogging) {
                format { call ->
                    val status = call.response.status()
                    val method = call.request.httpMethod.value
                    val path = call.request.path()
                    val userAgent = call.request.headers["User-Agent"]

                    buildString {
                        append("$method $path")
                        append(" -> $status")
                        if (userAgent != null && userAgent.isNotEmpty()) {
                            append(" [${userAgent.take(100)}]")
                        }
                    }
                }
                // Log all requests including static resources
                filter { call ->
                    true
                }
            }

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
        }.apply {
            KSLog.info("WebServer: Starting web server on port $port")
        }.start(wait = false).also {
            KSLog.info("WebServer: Web server started successfully on port $port")
        }
    }
}
