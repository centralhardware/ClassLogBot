package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import me.centralhardware.znatoki.telegram.statistic.entity.postgres.*;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.steps.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class Storage {

    private final Map<Long, Service> fsmTime = new HashMap<>();
    private final Map<Long, AddTime> fsmTimeStage = new HashMap<>();

    private final Map<Long, Client> fsmClient = new HashMap<>();
    private final Map<Long, AddCLient> fsmClientStage = new HashMap<>();

    private final Map<Long, Payment> fsmPayment = new HashMap<>();
    private final Map<Long, AddPayment> fsmPaymentStage = new HashMap<>();

    public AddTime getStage(Long chatId){
        return fsmTimeStage.get(chatId);
    }

    public void setStage(Long chatId, AddTime stage){
        fsmTimeStage.put(chatId, stage);
    }

    public Service getTime(Long chatId){
        return fsmTime.get(chatId);
    }

    public void setTime(Long chatId, Service service){
        fsmTime.put(chatId, service);
    }

    public Client getClient(Long chatId){
        return fsmClient.get(chatId);
    }

    public void createClient(Long chatId){
        fsmClient.put(chatId, new Client());
        fsmClientStage.put(chatId, AddCLient.ADD_FIO);
    }

    public AddCLient getCLientStage(Long chatId){
        return fsmClientStage.get(chatId);
    }

    public void setClientStage(Long chatId, AddCLient step){
        fsmClientStage.put(chatId, step);
    }

    public AddPayment getPaymentStage(Long chatId){
        return fsmPaymentStage.get(chatId);
    }

    public void setPaymentStage(Long chatId, AddPayment stage){
        fsmPaymentStage.put(chatId, stage);
    }

    public Payment getPayment(Long chatId){
        return fsmPayment.get(chatId);
    }

    public void setPayment(Long chatId, Payment payment){
        fsmPayment.put(chatId, payment);
    }

    public void remove(Long chatId){
        fsmTime.remove(chatId);
        fsmTimeStage.remove(chatId);
        fsmClient.remove(chatId);
        fsmClientStage.remove(chatId);
        fsmPayment.remove(chatId);
        fsmPaymentStage.remove(chatId);
    }

    public boolean contain(Long chaId){
        return fsmTime.containsKey(chaId) || fsmClient.containsKey(chaId) || fsmPayment.containsKey(chaId);
    }

    public boolean containsCLient(Long chatId){
        return fsmClient.containsKey(chatId);
    }

    public boolean containTime(Long chatId){
        return fsmTime.containsKey(chatId);
    }

    public boolean containsPayment(Long chatId){
        return fsmPayment.containsKey(chatId);
    }


}
