package me.centralhardware.znatoki.telegram.statistic.telegram.fsm.steps;

public enum AddCLient {

    ADD_FIO,
    ADD_PROPERTIES;

    public AddCLient next() {
        return values()[(ordinal() + 1) % values().length];
    }

}
