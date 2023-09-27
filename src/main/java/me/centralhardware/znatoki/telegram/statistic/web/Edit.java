package me.centralhardware.znatoki.telegram.statistic.web;

import me.centralhardware.znatoki.telegram.statistic.entity.Client;
import me.centralhardware.znatoki.telegram.statistic.entity.Session;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.service.SessionService;
import me.centralhardware.znatoki.telegram.statistic.utils.DateUtils;
import me.centralhardware.znatoki.telegram.statistic.utils.TelephoneUtils;
import me.centralhardware.znatoki.telegram.statistic.web.dto.EditForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Predicate;

import static me.centralhardware.znatoki.telegram.statistic.web.WebConstant.ERROR_MESSAGE;
import static me.centralhardware.znatoki.telegram.statistic.web.WebConstant.ERROR_TITLE;

@Controller
public class Edit {
    private final ResourceBundle resourceBundle;
    public static final String ERROR_PAGE_NAME = "error";

    private final SessionService sessionService;
    private final ClientService clientService;

    public Edit(ResourceBundle resourceBundle, SessionService sessionService, ClientService clientService) {
        this.resourceBundle = resourceBundle;
        this.sessionService = sessionService;
        this.clientService = clientService;
    }

    @RequestMapping(value = "/edit", method = RequestMethod.GET)
    public String edit(@RequestParam String sessionId, Model model) {
        Optional<Session> s = sessionService.findByUuid(UUID.fromString(sessionId));
        if (s.isPresent()) {
            if (s.get().isExpire()) {
                model.addAttribute(ERROR_TITLE, resourceBundle.getString("SESSION_EXPIRE"));
                model.addAttribute(ERROR_MESSAGE, resourceBundle.getString("SESSION_EXPIRE_GIVE_NEW"));
                return ERROR_PAGE_NAME;
            }
        } else {
            model.addAttribute(ERROR_TITLE, resourceBundle.getString("SESSION_NOT_FOUND"));
            model.addAttribute(ERROR_MESSAGE, resourceBundle.getString("SESSION_NOT_FOUND"));
            return ERROR_PAGE_NAME;
        }
        Optional<Session> sessionOptional = sessionService.findByUuid(UUID.fromString(sessionId));
        if (sessionOptional.isPresent()) {
            Client client = clientService.findById(sessionOptional.get().getClientId()).get();
            model.addAttribute(EditForm.FIELD_NAME, client.getName());
            model.addAttribute(EditForm.FIELD_SECOND_NAME, client.getSecondName());
            model.addAttribute(EditForm.FIELD_LAST_NAME, client.getLastName());
            model.addAttribute(EditForm.FIELD_CLASS_NUMBER, client.getClassNumber());
            model.addAttribute(EditForm.FIELD_DATE_OF_RECORD, DateUtils.dateFormat.format(client.getDateOfRecord()));
            model.addAttribute(EditForm.FIELD_DATE_OF_BIRTH, DateUtils.dateFormat.format(client.getDateOfBirth()));
            model.addAttribute(EditForm.FIELD_TELEPHONE, client.getTelephone());
            model.addAttribute(EditForm.TELEPHONE_RESPONSIBLE, client.getTelephoneResponsible());
            model.addAttribute(EditForm.FIELD_MOTHER_NAME, client.getMotherName());
            model.addAttribute(EditForm.FIELD_SESSOIN_ID, sessionOptional.get().getUuid());
            return "edit";
        }
        model.addAttribute(ERROR_TITLE, resourceBundle.getString("ERROR"));
        model.addAttribute(ERROR_MESSAGE, resourceBundle.getString("UNKNOWN_ERROR"));
        return ERROR_PAGE_NAME;
    }

    private static final Predicate<String> IS_NONE_EMPTY = str -> !str.isEmpty();

    @RequestMapping(value = "/save", method = RequestMethod.GET)
    public String save(EditForm editForm, Model model){
        Optional<Session> optionalSession = sessionService.findByUuid(UUID.fromString(editForm.sessionId()));
        if (optionalSession.isPresent()){
            if (optionalSession.get().isExpire()){
                fillError(model, "SESSION_EXPIRE_GIVE_NEW");
                return ERROR_PAGE_NAME;
            }
        } else {
            fillError(model, "SESSION_NOT_FOUND_GIVE_NEW");
            return ERROR_PAGE_NAME;
        }
        Client client = clientService.findById(optionalSession.get().getClientId()).get();
        if (IS_NONE_EMPTY.test(editForm.name())) {
            client.setName(editForm.name());
        } else {
            fillError(model, "EMPTY_FIELD_NAME");
            return ERROR_PAGE_NAME;
        }
        if (IS_NONE_EMPTY.test(editForm.secondName())) {
            client.setSecondName(editForm.secondName());
        } else {
            fillError(model, "EMPTY_FIELD_SECOND_NAME");
            return ERROR_PAGE_NAME;
        }
        if (IS_NONE_EMPTY.test(editForm.lastName())) {
            client.setLastName(editForm.lastName());
        } else {
            fillError(model, "EMPTY_FIELD_LAST_NAME");
            return ERROR_PAGE_NAME;
        }
        if (editForm.classNumber() < 12 && editForm.classNumber() > 0) {
            client.setClassNumber(editForm.classNumber());
        } else {
            fillError(model, "WRONG_CLASS_NUMBER");
            return ERROR_PAGE_NAME;
        }
        if (IS_NONE_EMPTY.test(editForm.date_of_record())) {
            LocalDateTime dateOfRecord = LocalDate.parse(editForm.date_of_record(), DateUtils.dateFormat).atStartOfDay();
            client.setDateOfRecord(dateOfRecord);
        } else {
            fillError(model, "EMPTY_FIELD_DATE_OF_RECORD");
            return ERROR_PAGE_NAME;
        }
        if (IS_NONE_EMPTY.test(editForm.date_of_birth())) {
            LocalDateTime dateOfBirth = LocalDate.parse(editForm.date_of_birth(),DateUtils.dateFormat).atStartOfDay();
            client.setDateOfBirth(dateOfBirth);
        } else {
            fillError(model, "EMPTY_FIELD_DATE_OF_BIRTH");
            return ERROR_PAGE_NAME;
        }
        if (TelephoneUtils.validate(editForm.telephone())) {
            client.setTelephone(editForm.telephone());
        }
        if (TelephoneUtils.validate(editForm.telephone_responsible())) {
            client.setTelephoneResponsible(editForm.telephone_responsible());
        }
        if (IS_NONE_EMPTY.test(editForm.mother_name())) {
            client.setMotherName(editForm.mother_name());
        }
        client.setUpdateBy(optionalSession.get().getUpdateBy());
        clientService.save(client);
        model.addAttribute("successMessage", resourceBundle.getString("PUPIL_UPDATED"));
        return "success";
    }

    private void fillError(Model model, String errorKey){
        model.addAttribute(ERROR_TITLE, resourceBundle.getString("ERROR"));
        model.addAttribute(ERROR_MESSAGE, resourceBundle.getString(errorKey));
    }

}