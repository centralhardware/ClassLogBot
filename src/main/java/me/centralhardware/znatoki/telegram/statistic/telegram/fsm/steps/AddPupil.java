package me.centralhardware.znatoki.telegram.statistic.telegram.fsm.steps;

public enum AddPupil {

    INPUT_FIO,
    INPUT_PROPERTIES;

    public AddPupil next() {
        return values()[(ordinal() + 1) % values().length];
    }

}
