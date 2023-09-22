package me.centralhardware.znatoki.telegram.statistic.utils;

import com.ibm.icu.text.Transliterator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Transcriptor {

    private final Transliterator translitirator;

    public String convert(String str) {
        return translitirator.transliterate(str);
    }

}
