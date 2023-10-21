package me.centralhardware.znatoki.telegram.statistic.typeHandler;

public class ListStringTypeHandler extends ListTypeHandler<String> {

    @Override
    protected String convert(String val) {
        return val;
    }

}
