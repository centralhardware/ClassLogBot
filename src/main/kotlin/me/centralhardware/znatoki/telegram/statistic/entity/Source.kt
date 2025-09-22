package me.centralhardware.znatoki.telegram.statistic.entity

enum class SourceOption(val title: String) {
    SIGNBOARD("Вывеска"),
    FROM_PREVIOUS_YEARS("С прошлых лет"),
    FROM_FRIENDS("От знакомых"),
    TWO_GIS("2gis"),
    ENTRANCE_AD("Реклама на подъезде"),
    OLDER_STUDENTS("Ходили старшие"),
    INTERNET("Интернет"),
    LEAFLET("Листовка"),
    AUDIO_SHOP_AD("Аудио Реклама в магазине"),
    TV_AD("Реклама на ТВ"),
    INSTAGRAM("инстаграм");

    companion object {
        fun options() = entries.map { it.name }.toList()

        fun fromTitle(title: String): SourceOption? =
            entries.find { it.title.equals(title, ignoreCase = true) }
    }
}