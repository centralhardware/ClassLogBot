package me.centralhardware.znatoki.telegram.statistic

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper

fun validateFio(value: String): Either<String, String> =
    if (ClientMapper.existsByFio(value)) {
        Either.Right(value)
    } else {
        Either.Left("ФИО не найдено")
    }

fun validateAmount(value: String): Either<String, Int> {
    val parsedValue = value.toIntOrNull()
    return when {
        parsedValue == null -> {
            Either.Left("Введенное значение должно быть числом")
        }
        parsedValue <= 0 -> {
            Either.Left("Введенное значение должно быть больше нуля")
        }
        else -> {
            Either.Right(parsedValue)
        }
    }
}

fun validateService(serviceName: String): Either<String, String> {
    val services = ServicesMapper.findAll()
    val servicesNames = services.map { it.name }
    return if (servicesNames.contains(serviceName)) {
        Either.Right(serviceName)
    } else {
        Either.Left("Выберите услугу из списка")
    }
}
