package me.centralhardware.znatoki.telegram.statistic.clickhouse.model;

import java.time.LocalDateTime;

public record Pupil(
        Integer id,
        String address,
        Integer classNumber,
        String congenitalDiseases,
        LocalDateTime createDate,
        LocalDateTime dateOfBirth,
        LocalDateTime dateOfRecord,
        String email,
        String fatherName,
        String grandMotherName,
        String hobbies,
        String howToKnow,
        String lastName,
        LocalDateTime modifyDate,
        String motherName,
        String name,
        String placeOfWorkFather,
        String placeOfWorkMother,
        String secondName,
        String telephone,
        String telephoneFather,
        String telephoneGrandMother,
        String telephoneMother,
        Integer createdBy,
        Integer updatedBY,
        Boolean deleted
) {
}
