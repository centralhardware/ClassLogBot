package me.centralhardware.znatoki.telegram.statistic

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.entity.Amount
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.toStudentId
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper

fun validateFio(value: String): Either<String, StudentId> =
    if (StudentMapper.existsByFio(value)) {
        Either.Right(value.split(" ")[0].toInt().toStudentId())
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

