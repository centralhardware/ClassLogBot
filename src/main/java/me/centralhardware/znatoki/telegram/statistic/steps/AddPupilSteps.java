package me.centralhardware.znatoki.telegram.statistic.steps;

public enum AddPupilSteps{

    INPUT_FIO,
    INPUT_CLASS_NUMBER,
    INPUT_DATE_OF_RECORD,
    INPUT_DATE_OF_BIRTH,
    INPUT_TELEPHONE,
    INPUT_TELEPHONE_OF_RESPONSIBLE,
    INPUT_SUBJECT,
    INPUT_HOW_TO_KNOW,
    INPUT_MOTHER_NAME;

    public AddPupilSteps next() {
        return values()[(ordinal() + 1) % values().length];
    }

}
