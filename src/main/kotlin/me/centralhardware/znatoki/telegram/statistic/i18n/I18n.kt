package me.centralhardware.znatoki.telegram.statistic.i18n

import java.util.*

object I18n {

    enum class Message(private val key: String) {
        INPUT_FIO_REQUIRED_FORMAT("INPUT_FIO_REQUIRED_FORMAT"),
        FIO_ALREADY_IN_DATABASE("FIO_ALREADY_IN_DATABASE"),
        CREATE_PUPIL_FINISHED("CREATE_PUPIL_FINISHED"),
        USER_NOT_FOUND("USER_NOT_FOUND"),
        PUPIL_DELETED("PUPIL_DELETED"),
        SEARCH_RESULT("SEARCH_RESULT"),
        NOTHING_FOUND("NOTHING_FOUND"),
        PUPIL_NOT_FOUND("PUPIL_NOT_FOUND"),
        INPUT_FIO_IN_FORMAT("INPUT_FIO_IN_FORMAT"),;

        private val resourceBundle: ResourceBundle = ResourceBundle.getBundle("Strings", Locale.US)
        fun load(): String = resourceBundle.getString(key)
    }

}