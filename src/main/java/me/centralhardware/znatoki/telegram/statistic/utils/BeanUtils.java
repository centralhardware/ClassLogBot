package me.centralhardware.znatoki.telegram.statistic.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class BeanUtils {

    private static ApplicationContext applicationContext;

    public static <T> T getBean(Class<T> clazz){
        return applicationContext.getBean(clazz);
    }

}
