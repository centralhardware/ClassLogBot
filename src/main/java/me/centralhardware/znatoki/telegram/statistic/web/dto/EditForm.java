package me.centralhardware.znatoki.telegram.statistic.web.dto;

import java.util.Optional;

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
        String diseases,
        Optional<String> chemistry,
        Optional<String> biology,
        Optional<String> german,
        Optional<String> english,
        Optional<String> primary_classes,
        Optional<String> russian,
        Optional<String> mathematics,
        Optional<String> social_studies,
        Optional<String> history,
        Optional<String> geography,
        Optional<String> speech_therapy,
        Optional<String> psychology,
        Optional<String> phisics
) {

    public EditForm {
        chemistry = Optional.empty();
        biology = Optional.empty();
        german = Optional.empty();
        english = Optional.empty();
        primary_classes = Optional.empty();
        russian = Optional.empty();
        mathematics = Optional.empty();
        social_studies = Optional.empty();
        history = Optional.empty();
        geography = Optional.empty();
        speech_therapy = Optional.empty();
        psychology = Optional.empty();
    }

    public static final String FIELD_NAME = "name";
    public static final String FIELD_SECOND_NAME = "secondName";
    public static final String FIELD_LAST_NAME = "lastName";
    public static final String FIELD_CLASS_NUMBER = "classNumber";
    public static final String FIELD_ADDRESS = "address";
    public static final String FIELD_DATE_OF_RECORD = "date_of_record";
    public static final String FIELD_DATE_OF_BIRTH = "date_of_birth";
    public static final String FIELD_TELEPHONE = "telephone";
    public static final String FIELD_TELEPHONE_MOTHER = "telephone_mother";
    public static final String TELEPHONE_RESPONSIBLE = "telephone_responsible";
    public static final String FIELD_TELEPHONE_GRANDMOTHER = "telephone_grandmother";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_PLACE_OF_WORK_MOTHER = "place_of_work_mother";
    public static final String FIELD_PLACE_OF_WORK_FATHER = "place_of_work_father";
    public static final String FIELD_MOTHER_NAME = "mother_name";
    public static final String FIELD_FATHER_NAME = "father_name";
    public static final String FIELD_GRANDMOTHER_NAME = "grandmother_name";
    public static final String FIELD_CHEMISTRY = "chemistry";
    public static final String FIELD_BIOLOGY = "biology";
    public static final String FIELD_GERMAN = "german";
    public static final String FIELD_ENGLISH = "english";
    public static final String FIELD_PRIMARY_CLASSES = "primary_classes";
    public static final String FIELD_RUSSIAN = "russian";
    public static final String FIELD_MATHEMATICS = "mathematics";
    public static final String FIELD_SOCIAL_STUDIES = "social_studies";
    public static final String FIELD_HISTORY = "history";
    public static final String FIELD_GEOLOGY = "geography";
    public static final String FIELD_SPEACH_THEOROPY = "speech_therapy";
    public static final String FIELD_PSYCHOLOGY = "psychology";
    public static final String FIELD_PHISICS = "phisics";
    public static final String FIELD_NONE = "none";
    public static final String FIELD_BRONCHIAL_ASTHMA = "bronchialAsthma";
    public static final String FIELD_VEGETATIVE_VASCULAR_DYSTONIA = "vegetativeVascularDystonia";
    public static final String FIELD_CEREBRAL_PALSY = "cerebralPalsy";
    public static final String FIELD_DEVELOPMENT_DELAY = "developmentDelay";
    public static final String FIELD_SESSOIN_ID = "sessionId";




}
