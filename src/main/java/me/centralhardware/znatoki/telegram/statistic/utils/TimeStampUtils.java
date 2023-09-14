package me.centralhardware.znatoki.telegram.statistic.utils;

import java.sql.Timestamp;

public class TimeStampUtils {

    public static long getTimestamp() {
        return new Timestamp(System.currentTimeMillis()).getTime();
    }

}
