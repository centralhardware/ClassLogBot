package me.centralhardware.znatoki.telegram.statistic.web;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.eav.Property;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Client;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Session;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.service.SessionService;
import me.centralhardware.znatoki.telegram.statistic.web.dto.EditForm;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.function.Predicate;

import static me.centralhardware.znatoki.telegram.statistic.web.WebConstant.ERROR_MESSAGE;
import static me.centralhardware.znatoki.telegram.statistic.web.WebConstant.ERROR_TITLE;

@Controller
@RequiredArgsConstructor
public class Edit {
    private final ResourceBundle resourceBundle;
    public static final String ERROR_PAGE_NAME = "error";

    private final SessionService sessionService;
    private final ClientService clientService;
    private final OrganizationMapper organizationMapper;

    @GetMapping(value = "/edit")
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
            model.addAttribute(EditForm.FIELD_SESSOIN_ID, sessionOptional.get().getUuid());
            model.addAttribute("properties", client.getProperties());
            model.addAttribute("clientName",
                    STR."редактирование \{organizationMapper.getById(client.getOrganizationId()).getClientName()}");
            return "edit";
        }
        model.addAttribute(ERROR_TITLE, resourceBundle.getString("ERROR"));
        model.addAttribute(ERROR_MESSAGE, resourceBundle.getString("UNKNOWN_ERROR"));
        return ERROR_PAGE_NAME;
    }

    private static final Predicate<String> IS_NONE_EMPTY = str -> !str.isEmpty();

    @GetMapping(value = "/save")
    public String save(@RequestParam Map<String, String> params, Model model){
        Optional<Session> optionalSession = sessionService.findByUuid(UUID.fromString(params.get("sessionId")));
        if (optionalSession.isPresent()){
            if (optionalSession.get().isExpire()){
                fillError(model, "SESSION_EXPIRE_GIVE_NEW");
                return ERROR_PAGE_NAME;
            }
        } else {
            fillError(model, "SESSION_NOT_FOUND_GIVE_NEW");
            return ERROR_PAGE_NAME;
        }
        params.remove("sessionId");
        Client client = clientService.findById(optionalSession.get().getClientId()).get();
        if (IS_NONE_EMPTY.test(params.get("name"))) {
            client.setName(params.get("name"));
        } else {
            fillError(model, "EMPTY_FIELD_NAME");
            return ERROR_PAGE_NAME;
        }
        params.remove("name");
        if (IS_NONE_EMPTY.test(params.get("secondName"))) {
            client.setSecondName(params.get("secondName"));
        } else {
            fillError(model, "EMPTY_FIELD_SECOND_NAME");
            return ERROR_PAGE_NAME;
        }
        params.remove("secondName");
        if (IS_NONE_EMPTY.test(params.get("lastName"))) {
            client.setLastName(params.get("lastName"));
        }
        params.remove("lastName");

        var iterator = client.getProperties().iterator();
        var changed = new ArrayList<Property>();
        while (iterator.hasNext()){
            var property = iterator.next();
            if (params.containsKey(property.name())){
                var validation = property.type().validate(params.get(property.name()));
                if (StringUtils.isNotBlank(property.value()) && validation.isInvalid()){
                    model.addAttribute(ERROR_TITLE, resourceBundle.getString("ERROR"));
                    model.addAttribute(ERROR_MESSAGE, property.name() + " " + validation.getError());
                    return ERROR_PAGE_NAME;
                }
                changed.add(property.withValue(params.get(property.name())));
                iterator.remove();
            }
        }
        client.getProperties().addAll(changed);

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