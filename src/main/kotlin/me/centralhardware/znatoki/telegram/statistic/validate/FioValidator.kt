package me.centralhardware.znatoki.telegram.statistic.validate

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import org.springframework.stereotype.Component

@Component
class FioValidator(private val clientMapper: ClientMapper) : Validator<String, String> {
    override fun validate(value: String): Either<String, String> = if (clientMapper.existsByFio(value)){
        Either.Right(value)
    } else{
        Either.Left("ФИО не найдено")
    }
}