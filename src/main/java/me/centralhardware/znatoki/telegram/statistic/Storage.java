package me.centralhardware.znatoki.telegram.statistic;

import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class Storage {

    private final Map<Long, Time> fsm = new HashMap<>();
    private final Map<Long, Integer> fsmStage = new HashMap<>();


    public Integer getStage(Long chatId){
        return fsmStage.get(chatId);
    }

    public void setStage(Long chatId, Integer stage){
        fsmStage.put(chatId, stage);
    }

    public Time getTIme(Long chatId){
        return fsm.get(chatId);
    }

    public void setTime(Long chatId, Time time){
        fsm.put(chatId, time);
    }

    public void remove(Long chatId){
        fsm.remove(chatId);
        fsmStage.remove(chatId);
    }

    public boolean contain(Long chaId){
        return fsm.containsKey(chaId);
    }

}
