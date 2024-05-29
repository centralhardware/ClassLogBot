package me.centralhardware.znatoki.telegram.statistic

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import java.util.*

fun validateFio(value: String): Either<String, String> = if (ClientMapper.existsByFio(value)) {
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

fun validateService(value: Pair<String, UUID>): Either<String, String> {
    val services = ServicesMapper.getServicesByOrganization(value.second)
    val servicesNames = services.map { it.name }
    return if (servicesNames.contains(value.first)) {
        Either.Right(value.first)
    } else {
        Either.Left("Выберите услугу из списка")
    }
}