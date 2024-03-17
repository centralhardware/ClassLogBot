package me.centralhardware.znatoki.telegram.statistic.validate

import arrow.core.Either

interface Validator<I, V> {

    fun validate(value: I): Either<String, V>

}