package me.centralhardware.znatoki.telegram.statistic.entity

enum class DataSource(val value: String) {
    WEB("WEB"),
    BOT("BOT");

    companion object {
        fun fromValue(value: String): DataSource? =
            entries.find { it.value.equals(value, ignoreCase = true) }
    }
}
