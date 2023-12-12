package me.centralhardware.znatoki.telegram.statistic.utils;

import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.text.MaskFormatter;
import java.text.ParseException;

import java.util.regex.Pattern;

public class TelephoneUtils {

    private static final Pattern VALID_PHONE_NR = Pattern.compile("^[78]\\d{10}$");
    private static final MaskFormatter PHONE_MASK_FORMATTER;

    static {
        try {
            PHONE_MASK_FORMATTER = new MaskFormatter("#-###-###-##-##");
        } catch (ParseException ex) {
            throw new RuntimeException("Failed to create a mask formatter.", ex);
        }
        PHONE_MASK_FORMATTER.setValueContainsLiteralCharacters(false);
    }

    public static boolean validate(String telephone) {
        return VALID_PHONE_NR.matcher(telephone).matches();
    }

    public static String format(String telephone) {
        if (telephone == null || telephone.isEmpty()) return "";
        try {
            return PHONE_MASK_FORMATTER.valueToString(telephone);
        } catch (ParseException ignored) {
        }
        return "";
    }
}
