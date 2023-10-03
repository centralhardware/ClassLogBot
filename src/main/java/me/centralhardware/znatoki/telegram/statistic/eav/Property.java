package me.centralhardware.znatoki.telegram.statistic.eav;

public record Property(
        String name,
        String value) {

    public Property(String name){
        this(name, "");
    }

}
