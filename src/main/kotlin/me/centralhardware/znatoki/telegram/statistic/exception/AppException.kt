package me.centralhardware.znatoki.telegram.statistic.exception

import io.ktor.http.*

sealed class AppException(
    message: String,
    val statusCode: HttpStatusCode
) : Exception(message)

class NotFoundException(message: String = "Resource not found") : AppException(message, HttpStatusCode.NotFound)

class BadRequestException(message: String = "Bad request") : AppException(message, HttpStatusCode.BadRequest)

class ValidationException(message: String = "Validation error") : AppException(message, HttpStatusCode.BadRequest)

class ConflictException(message: String = "Conflict") : AppException(message, HttpStatusCode.Conflict)

class ForbiddenException(message: String = "Forbidden") : AppException(message, HttpStatusCode.Forbidden)
