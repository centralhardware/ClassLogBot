package me.centralhardware.znatoki.telegram.statistic

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.entity.Amount
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper

fun validateFio(value: String): Either<String, Int> =
    if (StudentMapper.existsByFio(value)) {
        Either.Right(value.split(" ")[0].toInt())
    } else {
        Either.Left("ФИО не найдено")
    }

fun validateAmount(value: Int?): Either<String, Unit> {
    return if (Amount.validate(value)) {
        return Either.Right(Unit)
    } else {
        Either.Left("Введенное значение должно быть больше нуля")
    }
}

