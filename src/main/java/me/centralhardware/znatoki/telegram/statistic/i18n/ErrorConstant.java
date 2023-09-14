package me.centralhardware.znatoki.telegram.statistic.i18n;

import lombok.Getter;

public enum ErrorConstant implements ConstantEnum{

    ACCESS_DENIED("ACCESS_DENIED"),
    INVALID_ARGUMENT_GRANT_ACCESS("INVALID_ARGUMENT_GRANT_ACCESS"),
    INVALID_RIGHT("INVALID_RIGHT");

    @Getter
    final String key;

    ErrorConstant(String key){
        this.key = key;
    }

}
