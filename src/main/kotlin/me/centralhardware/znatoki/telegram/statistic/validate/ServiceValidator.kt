package me.centralhardware.znatoki.telegram.statistic.validate

import arrow.core.Either
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper
import org.springframework.stereotype.Component
import java.util.*

@Component
class ServiceValidator(private val servicesMapper: ServicesMapper) : Validator<Pair<String, UUID>, String> {

    override fun validate(value: Pair<String, UUID>): Either<String, String> {
        val services = servicesMapper.getServicesByOrganization(value.second)
        val servicesNames = services.map { it.name }
        return if (servicesNames.contains(value.first)) {
            Either.Right(value.first)
        } else {
            Either.Left("Выберите услугу из списка")
        }
    }
}