package me.centralhardware.znatoki.telegram.statistic.entity.Enum;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum HowToKnow {

    SIGNBOARD("Вывеска"),
    FROM_PASTE_YEAR("С прошлых лет"),
    FROM_FIENDS("От знакомых"),
    FROM_2GIS("2gis"),
    ENTRANCE_ADVERTISE("Реклама на подъезде"),
    THE_ELDERS_WENT("Ходили старшие"),
    INTERNET("Интернет "),
    LEAFLET("Листовка"),
    AUDIO_ADVERTISE_IN_STORE("Аудио Реклама в магазине"),
    ADVERTISING_ON_TV("Реклама на ТВ"),
    INSTAGRAM("инстаграм");

    final String rusName;

    HowToKnow(String rusName) {
        this.rusName = rusName;
    }

    public static HowToKnow getConstant(String name) {
        return Arrays.stream(values())
                .filter(it -> it.rusName.equals(name))
                .findFirst()
                .orElse(null);
    }

    public static boolean validate(String howToKnow) {
        return getConstant(howToKnow) != null;
    }

    @Override
    public String toString() {
        return rusName;
    }
}
