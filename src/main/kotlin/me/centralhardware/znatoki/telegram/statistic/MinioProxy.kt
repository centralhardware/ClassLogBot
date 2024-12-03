package me.centralhardware.znatoki.telegram.statistic

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.server.application.Application
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.uri
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.toByteArray


fun Application.minioProxy() {
    val client = HttpClient(CIO)

    routing {
        route("/{...}") {
            handle {
                val originalRequestUri = call.request.uri
                val targetUrl = "http://10.168.0.34:9000$originalRequestUri"

                val requestBody = call.receiveChannel().toByteArray()

                val proxiedResponse: HttpResponse = client.request(targetUrl) {
                    method = call.request.httpMethod
                    headers {
                        call.request.headers.forEach { key, values ->
                            values.forEach { value ->
                                append(key, value)
                            }
                        }
                    }
                    setBody(requestBody)
                }

                call.respondBytes(
                    proxiedResponse.bodyAsChannel().toByteArray(),
                    contentType = proxiedResponse.contentType(),
                    status = proxiedResponse.status
                )
            }
        }
    }
}