package me.centralhardware.znatoki.telegram.statistic.validate

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import org.springframework.stereotype.Component

@Component
class FioValidator(private val clientService: ClientService) : Validator<String, String> {
    override fun validate(value: String): Either<String, String> {
        return clientService.findByFioAndId(value)
            .let { Either.Left("ФИО не найдено") }
    }
}