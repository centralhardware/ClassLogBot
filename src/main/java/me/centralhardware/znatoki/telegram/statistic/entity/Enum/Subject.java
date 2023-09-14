package me.centralhardware.znatoki.telegram.statistic.entity.Enum;

import lombok.Getter;

import java.util.Arrays;

public enum Subject {

    CHEMISTRY("химия"),
    BIOLOGY("биология"),
    GERMAN("немецкий язык"),
    ENGLISH("английский язык"),
    PRIMARY_CLASSES("начальные классы"),
    RUSSIAN("русский язык"),
    MATHEMATICS("математика"),
    SOCIAL_STUDIES("обществознание"),
    HISTORY("история"),
    GEOGRAPHY("география"),
    SPEECH_THERAPIST("логопед"),
    PSYCHOLOGY("психология"),
    PHYSICS("физика");

    @Getter
    final String rusName;

    Subject(String rusName) {
        this.rusName = rusName;
    }

    public static Subject getConstant(String name) {
        return Arrays.stream(values())
                .filter(it -> it.rusName.equals(name))
                .findFirst()
                .orElse(null);
    }


    public static boolean validate(String subject) {
        return getConstant(subject) != null;
    }

    @Override
    public String toString() {
        return rusName;
    }
}
