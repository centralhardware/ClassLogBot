package me.centralhardware.znatoki.telegram.statistic.web.dto;

public record EditForm(
        String name,
        String secondName,
        String lastName,
        int classNumber,
        String address,
        String date_of_record,
        String date_of_birth,
        String telephone,
        String telephone_responsible,
        String telephone_mother,
        String telephone_father,
        String telephone_grandmother,
        String email,
        String place_of_work_mother,
        String place_of_work_father,
        String mother_name,
        String father_name,
        String grandmother_name,
        String sessionId,
        String diseases
) {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_SECOND_NAME = "secondName";
    public static final String FIELD_LAST_NAME = "lastName";
    public static final String FIELD_CLASS_NUMBER = "classNumber";
    public static final String FIELD_DATE_OF_RECORD = "date_of_record";
    public static final String FIELD_DATE_OF_BIRTH = "date_of_birth";
    public static final String FIELD_TELEPHONE = "telephone";
    public static final String TELEPHONE_RESPONSIBLE = "telephone_responsible";
    public static final String FIELD_MOTHER_NAME = "mother_name";
    public static final String FIELD_SESSOIN_ID = "sessionId";




}
