package me.centralhardware.znatoki.telegram.statistic;

import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.entity.Pupil;
import me.centralhardware.znatoki.telegram.statistic.steps.AddPupilSteps;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class Storage {

    private final Map<Long, Time> fsmTime = new HashMap<>();
    private final Map<Long, Integer> fsmTimeStage = new HashMap<>();
    private final Map<Long, Pupil> fsmPupil = new HashMap<>();
    private final Map<Long, AddPupilSteps> fsmPupilStage = new HashMap<>();


    public Integer getStage(Long chatId){
        return fsmTimeStage.get(chatId);
    }

    public void setStage(Long chatId, Integer stage){
        fsmTimeStage.put(chatId, stage);
    }

    public Time getTime(Long chatId){
        return fsmTime.get(chatId);
    }

    public void setTime(Long chatId, Time time){
        fsmTime.put(chatId, time);
    }

    public Pupil getPupil(Long chatId){
        return fsmPupil.get(chatId);
    }

    public void createPupil(Long chatId){
        fsmPupil.put(chatId, new Pupil());
        fsmPupilStage.put(chatId, AddPupilSteps.INPUT_FIO);
    }

    public AddPupilSteps getPupilStage(Long chatId){
        return fsmPupilStage.get(chatId);
    }

    public void setPupilStage(Long chatId, AddPupilSteps step){
        fsmPupilStage.put(chatId, step);
    }

    public void remove(Long chatId){
        fsmTime.remove(chatId);
        fsmTimeStage.remove(chatId);
        fsmPupil.remove(chatId);
        fsmPupilStage.remove(chatId);
    }

    public boolean contain(Long chaId){
        return fsmTime.containsKey(chaId);
    }

    public boolean containsPupil(Long chatId){
        return fsmPupil.containsKey(chatId);
    }

}
