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

    private final Map<Long, Client> fsmPupil = new HashMap<>();
    private final Map<Long, AddPupil> fsmPupilStage = new HashMap<>();

    private final Map<Long, Payment> fsmPayment = new HashMap<>();
    private final Map<Long, AddPayment> fsmPaymentStage = new HashMap<>();

    private final Map<Long, Organization> fsmOrganization = new HashMap<>();
    private final Map<Long, AddOrganization> fsmOrganizationStage = new HashMap<>();

    private final Map<Long, Invitation> fsmInvitation = new HashMap<>();
    private final Map<Long, AddInvitation> fsmInvitationStage = new HashMap<>();

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

    public Client getPupil(Long chatId){
        return fsmPupil.get(chatId);
    }

    public void createPupil(Long chatId){
        fsmPupil.put(chatId, new Client());
        fsmPupilStage.put(chatId, AddPupil.ФВВ_FIO);
    }

    public AddPupil getPupilStage(Long chatId){
        return fsmPupilStage.get(chatId);
    }

    public void setPupilStage(Long chatId, AddPupil step){
        fsmPupilStage.put(chatId, step);
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

    public void setOrganizationStage(Long chatId, AddOrganization stage){
        fsmOrganizationStage.put(chatId, stage);
    }

    public AddOrganization getOrganizationStage(Long chatId){
        return fsmOrganizationStage.get(chatId);
    }

    public void setOrganization(Long chatId, Organization organization){
        fsmOrganization.put(chatId, organization);
    }

    public Organization getOrganization(Long chatId){
        return fsmOrganization.get(chatId);
    }

    public void setInvitationStage(Long chatId, AddInvitation stage){
        fsmInvitationStage.put(chatId, stage);
    }

    public AddInvitation getInvitationStage(Long chatId){
        return fsmInvitationStage.get(chatId);
    }

    public void setInvitation(Long chatId, Invitation invitation){
        fsmInvitation.put(chatId, invitation);
    }

    public Invitation getInvitation(Long chatId){
        return fsmInvitation.get(chatId);
    }

    public void remove(Long chatId){
        fsmTime.remove(chatId);
        fsmTimeStage.remove(chatId);
        fsmPupil.remove(chatId);
        fsmPupilStage.remove(chatId);
        fsmPayment.remove(chatId);
        fsmPaymentStage.remove(chatId);
        fsmOrganization.remove(chatId);
        fsmOrganizationStage.remove(chatId);
        fsmOrganization.remove(chatId);
        fsmInvitationStage.remove(chatId);
    }

    public boolean contain(Long chaId){
        return fsmTime.containsKey(chaId) || fsmPupil.containsKey(chaId) || fsmPayment.containsKey(chaId);
    }

    public boolean containsPupil(Long chatId){
        return fsmPupil.containsKey(chatId);
    }

    public boolean containTime(Long chatId){
        return fsmTime.containsKey(chatId);
    }

    public boolean containsPayment(Long chatId){
        return fsmPayment.containsKey(chatId);
    }

    public boolean containsOrganization(Long chatId){
        return fsmOrganization.containsKey(chatId);
    }

    public boolean containsInvitation(Long chatId){
        return fsmInvitation.containsKey(chatId);
    }

}
