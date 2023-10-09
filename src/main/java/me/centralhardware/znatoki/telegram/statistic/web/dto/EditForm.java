package me.centralhardware.znatoki.telegram.statistic.web.dto;

public record EditForm(
        String name,
        String secondName,
        String lastName,
        String sessionId
) {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_SECOND_NAME = "secondName";
    public static final String FIELD_LAST_NAME = "lastName";
    public static final String FIELD_SESSOIN_ID = "sessionId";




}
