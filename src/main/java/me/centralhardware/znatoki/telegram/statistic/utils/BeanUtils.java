package me.centralhardware.znatoki.telegram.statistic.utils;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class BeanUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NotNull ApplicationContext ac) {
        BeanUtils.applicationContext = ac;
    }

    public static <T> T getBean(Class<T> clazz){
        return applicationContext.getBean(clazz);
    }

}
