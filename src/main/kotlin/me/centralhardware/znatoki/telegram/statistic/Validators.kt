package me.centralhardware.znatoki.telegram.statistic

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper

fun validateFio(value: String): Either<String, Int> =
    if (ClientMapper.existsByFio(value)) {
        Either.Right(value.split(" ")[0].toInt())
    } else {
        Either.Left("ФИО не найдено")
    }

fun validateAmount(value: Int?): Either<String, Unit> {
    return when {
        value == null -> {
            Either.Left("Введенное значение должно быть числом")
        }
        value <= 0 -> {
            Either.Left("Введенное значение должно быть больше нуля")
        }
        else -> {
            Either.Right(Unit)
        }
    }
}

