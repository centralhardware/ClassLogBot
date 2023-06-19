package me.centralhardware.znatoki.telegram.statistic.clickhouse.model;

import java.util.Arrays;

public enum Subject {

    MATHEMATICS("математика"),
    RUSSIAN("русский язык"),
    LITERATURE("литература"),
    PHYSICS("физика"),
    ENGLISH("английский язык"),
    GERMAN("немецкий"),
    SOCIAL_SCIENCE("обществознание"),
    BIOLOGY("биология"),
    CHEMISTRY("химия"),
    PRIMARY_SCHOOL("начальная школа"),
    PSYCHOLOGY("психология");

    final String rusName;

    Subject(String name) {
        this.rusName = name;
    }

    public String getRusName() {
        return rusName;
    }

    public static Subject of(String name){
        return Arrays.stream(Subject.values())
                .filter(it -> it.rusName.equals(name))
                .findFirst()
                .orElse(null);
    }

}
