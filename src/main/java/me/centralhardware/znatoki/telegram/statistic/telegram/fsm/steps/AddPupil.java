package me.centralhardware.znatoki.telegram.statistic.telegram.fsm.steps;

public enum AddPupil {

    INPUT_FIO,
    INPUT_CLASS_NUMBER,
    INPUT_DATE_OF_RECORD,
    INPUT_DATE_OF_BIRTH,
    INPUT_TELEPHONE,
    INPUT_TELEPHONE_OF_RESPONSIBLE,
    INPUT_HOW_TO_KNOW,
    INPUT_MOTHER_NAME;

    public AddPupil next() {
        return values()[(ordinal() + 1) % values().length];
    }

}
