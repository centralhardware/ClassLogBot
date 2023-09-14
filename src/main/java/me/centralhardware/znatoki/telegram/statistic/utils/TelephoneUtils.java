package me.centralhardware.znatoki.telegram.statistic.utils;

import org.apache.commons.lang3.StringUtils;

import javax.swing.text.MaskFormatter;
import java.text.ParseException;

public class TelephoneUtils {

    public static boolean validate(String telephone) {
        return StringUtils.isNumeric(telephone) &&
                telephone.length() == 11 &&
                (telephone.charAt(0) == '7' || telephone.charAt(0) == '8');
    }

    /**
     * @param telephone input 89029990408
     * @return output 8-902-999-04-08
     */
    public static String format(String telephone) {
        if (telephone == null || telephone.isEmpty()) return "";
        try {
            String phoneMask = "#-###-###-##-##";
            MaskFormatter maskFormatter = new MaskFormatter(phoneMask);
            maskFormatter.setValueContainsLiteralCharacters(false);
            return TelegramUtils.makeBold(maskFormatter.valueToString(telephone));
        } catch (ParseException ignored) {
        }
        return "";
    }

}
