package me.centralhardware.znatoki.telegram.statistic.i18n

object I18n {

    enum class Error(private val key: String): ConstantEnum {
        ACCESS_DENIED("ACCESS_DENIED");

        override fun key(): String {
            return key
        }
    }

    enum class Message(private val key: String): ConstantEnum {
        INPUT_FIO_REQUIRED_FORMAT("INPUT_FIO_REQUIRED_FORMAT"),
        FIO_ALREADY_IN_DATABASE("FIO_ALREADY_IN_DATABASE"),
        CREATE_PUPIL_FINISHED("CREATE_PUPIL_FINISHED"),
        USER_NOT_FOUND("USER_NOT_FOUND"),
        PUPIL_DELETED("PUPIL_DELETED"),
        SEARCH_RESULT("SEARCH_RESULT"),
        NOTHING_FOUND("NOTHING_FOUND"),
        PUPIL_NOT_FOUND("PUPIL_NOT_FOUND");

        override fun key(): String {
            return key
        }

    }

}

interface ConstantEnum {
    fun key(): String
}