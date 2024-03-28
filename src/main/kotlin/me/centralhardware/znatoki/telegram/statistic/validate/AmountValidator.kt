package me.centralhardware.znatoki.telegram.statistic.validate

import arrow.core.Either
import org.springframework.stereotype.Component

@Component
class AmountValidator : Validator<String, Int> {

    override fun validate(value: String): Either<String, Int> {
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
}