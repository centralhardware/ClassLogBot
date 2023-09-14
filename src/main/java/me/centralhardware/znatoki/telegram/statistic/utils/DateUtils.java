package me.centralhardware.znatoki.telegram.statistic.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils {

    public static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM");

    public static boolean isBirthday(LocalDateTime dateOfBirth) {
        return formatter.format(dateOfBirth).equals(formatter.format(LocalDateTime.now()));
    }

}
