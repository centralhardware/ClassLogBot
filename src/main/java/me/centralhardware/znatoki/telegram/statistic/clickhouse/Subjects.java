package me.centralhardware.znatoki.telegram.statistic.clickhouse;

import java.util.Arrays;

public enum Subjects {

    MATHEMATICS("математика"),
    RUSSIAN("русский язык"),
    LITERATURE("литература"),
    PHYSICS("физика"),
    ENGLISH("английский язык"),
    GERMAN("немецкий"),
    SOCIAL_SCIENCE("обществознание"),
    BIOLOGY("биология"),
    CHEMШSTRY("химия"),
    PRIMARY_SCHOOL("начальная школа"),
    PSYCHOLOGY("психология");

    final String rusName;

    Subjects(String name) {
        this.rusName = name;
    }

    public String getRusName() {
        return rusName;
    }

    public static Subjects of(String name){
        return Arrays.stream(Subjects.values())
                .filter(it -> it.rusName.equals(name))
                .findFirst()
                .orElse(null);
    }

}
