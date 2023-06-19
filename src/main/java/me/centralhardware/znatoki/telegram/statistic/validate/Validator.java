package me.centralhardware.znatoki.telegram.statistic.validate;

import io.vavr.control.Either;

public interface Validator<I, V> {

    Either<V, String> validate(I value);

}
