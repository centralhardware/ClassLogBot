package me.centralhardware.znatoki.telegram.statistic.web;

import me.centralhardware.znatoki.telegram.statistic.entity.Enum.Subject;
import me.centralhardware.znatoki.telegram.statistic.entity.Pupil;
import me.centralhardware.znatoki.telegram.statistic.entity.Session;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.service.SessionService;
import me.centralhardware.znatoki.telegram.statistic.utils.DateUtils;
import me.centralhardware.znatoki.telegram.statistic.utils.TelephoneUtils;
import me.centralhardware.znatoki.telegram.statistic.web.dto.EditForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import static me.centralhardware.znatoki.telegram.statistic.web.WebConstant.ERROR_MESSAGE;
import static me.centralhardware.znatoki.telegram.statistic.web.WebConstant.ERROR_TITLE;

@Controller
public class Edit {

    private final static Logger log = LoggerFactory.getLogger(Edit.class);
    private final ResourceBundle resourceBundle;
    public static final String ERROR_PAGE_NAME = "error";

    private final SessionService sessionService;
    private final PupilService pupilService;

    public Edit(ResourceBundle resourceBundle, SessionService sessionService, PupilService pupilService) {
        this.resourceBundle = resourceBundle;
        this.sessionService = sessionService;
        this.pupilService   = pupilService;
    }

    @RequestMapping(value = "/edit", method = RequestMethod.GET)
    public String edit(@RequestParam String sessionId, Model model) {
        Optional<Session> s = sessionService.findByUuid(sessionId);
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
        Optional<Session> sessionOptional = sessionService.findByUuid(sessionId);
        if (sessionOptional.isPresent()) {
            Pupil pupil = sessionOptional.get().getPupil();
            model.addAttribute(EditForm.FIELD_NAME, pupil.getName());
            model.addAttribute(EditForm.FIELD_SECOND_NAME, pupil.getSecondName());
            model.addAttribute(EditForm.FIELD_LAST_NAME, pupil.getLastName());
            model.addAttribute(EditForm.FIELD_CLASS_NUMBER, pupil.getClassNumber());
            model.addAttribute(EditForm.FIELD_DATE_OF_RECORD, DateUtils.dateFormat.format(pupil.getDateOfRecord()));
            model.addAttribute(EditForm.FIELD_DATE_OF_BIRTH, DateUtils.dateFormat.format(pupil.getDateOfBirth()));
            model.addAttribute(EditForm.FIELD_TELEPHONE, pupil.getTelephone());
            model.addAttribute(EditForm.TELEPHONE_RESPONSIBLE, pupil.getTelephoneResponsible());
            model.addAttribute(EditForm.FIELD_MOTHER_NAME, pupil.getMotherName());
            model.addAttribute(EditForm.FIELD_CHEMISTRY, pupil.getSubjects().contains(Subject.CHEMISTRY));
            model.addAttribute(EditForm.FIELD_BIOLOGY, pupil.getSubjects().contains(Subject.BIOLOGY));
            model.addAttribute(EditForm.FIELD_GERMAN, pupil.getSubjects().contains(Subject.GERMAN));
            model.addAttribute(EditForm.FIELD_ENGLISH, pupil.getSubjects().contains(Subject.ENGLISH));
            model.addAttribute(EditForm.FIELD_PRIMARY_CLASSES, pupil.getSubjects().contains(Subject.PRIMARY_CLASSES));
            model.addAttribute(EditForm.FIELD_RUSSIAN, pupil.getSubjects().contains(Subject.RUSSIAN));
            model.addAttribute(EditForm.FIELD_MATHEMATICS, pupil.getSubjects().contains(Subject.MATHEMATICS));
            model.addAttribute(EditForm.FIELD_SOCIAL_STUDIES, pupil.getSubjects().contains(Subject.SOCIAL_STUDIES));
            model.addAttribute(EditForm.FIELD_HISTORY, pupil.getSubjects().contains(Subject.HISTORY));
            model.addAttribute(EditForm.FIELD_GEOLOGY, pupil.getSubjects().contains(Subject.GEOGRAPHY));
            model.addAttribute(EditForm.FIELD_SPEACH_THEOROPY, pupil.getSubjects().contains(Subject.SPEECH_THERAPIST));
            model.addAttribute(EditForm.FIELD_PSYCHOLOGY, pupil.getSubjects().contains(Subject.PSYCHOLOGY));
            model.addAttribute(EditForm.FIELD_PHISICS, pupil.getSubjects().contains(Subject.PHYSICS));
            model.addAttribute(EditForm.FIELD_SESSOIN_ID, sessionOptional.get().getUuid());
            return "edit";
        }
        model.addAttribute(ERROR_TITLE, resourceBundle.getString("ERROR"));
        model.addAttribute(ERROR_MESSAGE, resourceBundle.getString("UNKNOWN_ERROR"));
        return ERROR_PAGE_NAME;
    }

    private static final Predicate<String> IS_NONE_EMPTY = str -> !str.isEmpty();
    private static final Predicate<Optional<String>> IS_CHECKBOX_ON = str -> str.isPresent() && str.get().equals("on");

    @RequestMapping(value = "/save", method = RequestMethod.GET)
    public String save(EditForm editForm, Model model){
        Optional<Session> optionalSession = sessionService.findByUuid(editForm.sessionId());
        if (optionalSession.isPresent()){
            if (optionalSession.get().isExpire()){
                fillError(model, "SESSION_EXPIRE_GIVE_NEW");
                return ERROR_PAGE_NAME;
            }
        } else {
            fillError(model, "SESSION_NOT_FOUND_GIVE_NEW");
            return ERROR_PAGE_NAME;
        }
        Pupil pupil = optionalSession.get().getPupil();
        if (IS_NONE_EMPTY.test(editForm.name())) {
            pupil.setName(editForm.name());
        } else {
            fillError(model, "EMPTY_FIELD_NAME");
            return ERROR_PAGE_NAME;
        }
        if (IS_NONE_EMPTY.test(editForm.secondName())) {
            pupil.setSecondName(editForm.secondName());
        } else {
            fillError(model, "EMPTY_FIELD_SECOND_NAME");
            return ERROR_PAGE_NAME;
        }
        if (IS_NONE_EMPTY.test(editForm.lastName())) {
            pupil.setLastName(editForm.lastName());
        } else {
            fillError(model, "EMPTY_FIELD_LAST_NAME");
            return ERROR_PAGE_NAME;
        }
        if (editForm.classNumber() < 12 && editForm.classNumber() > 0) {
            pupil.setClassNumber(editForm.classNumber());
        } else {
            fillError(model, "WRONG_CLASS_NUMBER");
            return ERROR_PAGE_NAME;
        }
        if (IS_NONE_EMPTY.test(editForm.date_of_record())) {
            LocalDateTime dateOfRecord = LocalDateTime.parse(editForm.date_of_record(), DateUtils.dateFormat);
            pupil.setDateOfRecord(dateOfRecord);
        } else {
            fillError(model, "EMPTY_FIELD_DATE_OF_RECORD");
            return ERROR_PAGE_NAME;
        }
        if (IS_NONE_EMPTY.test(editForm.date_of_birth())) {
            LocalDateTime dateOfBirth = LocalDateTime.parse(editForm.date_of_birth(),DateUtils.dateFormat);
            pupil.setDateOfBirth(dateOfBirth);
        } else {
            fillError(model, "EMPTY_FIELD_DATE_OF_BIRTH");
            return ERROR_PAGE_NAME;
        }
        if (TelephoneUtils.validate(editForm.telephone())) {
            pupil.setTelephone(editForm.telephone());
        }
        if (IS_NONE_EMPTY.test(editForm.mother_name())) {
            pupil.setMotherName(editForm.mother_name());
        }
        if (IS_CHECKBOX_ON.test(editForm.chemistry())) {
            pupil.getSubjects().add(Subject.CHEMISTRY);
        }
        if (IS_CHECKBOX_ON.test(editForm.biology())) {
            pupil.getSubjects().add(Subject.BIOLOGY);
        }
        if (IS_CHECKBOX_ON.test(editForm.german())) {
            pupil.getSubjects().add(Subject.GERMAN);
        }
        if (IS_CHECKBOX_ON.test(editForm.english())) {
            pupil.getSubjects().add(Subject.ENGLISH);
        }
        if (IS_CHECKBOX_ON.test(editForm.primary_classes())) {
            pupil.getSubjects().add(Subject.PRIMARY_CLASSES);
        }
        if (IS_CHECKBOX_ON.test(editForm.russian())) {
            pupil.getSubjects().add(Subject.RUSSIAN);
        }
        if (IS_CHECKBOX_ON.test(editForm.mathematics())) {
            pupil.getSubjects().add(Subject.MATHEMATICS);
        }
        if (IS_CHECKBOX_ON.test(editForm.social_studies())) {
            pupil.getSubjects().add(Subject.SOCIAL_STUDIES);
        }
        if (IS_CHECKBOX_ON.test(editForm.history())) {
            pupil.getSubjects().add(Subject.HISTORY);
        }
        if (IS_CHECKBOX_ON.test(editForm.geography())) {
            pupil.getSubjects().add(Subject.GEOGRAPHY);
        }
        if (IS_CHECKBOX_ON.test(editForm.speech_therapy())) {
            pupil.getSubjects().add(Subject.SPEECH_THERAPIST);
        }
        if (IS_CHECKBOX_ON.test(editForm.psychology())) {
            pupil.getSubjects().add(Subject.PSYCHOLOGY);
        }
        if (IS_CHECKBOX_ON.test(editForm.psychology())) {
            pupil.getSubjects().add(Subject.PHYSICS);
        }
        pupil.setUpdateBy(optionalSession.get().getUpdateBy());
        pupilService.save(pupil);
        model.addAttribute("successMessage", resourceBundle.getString("PUPIL_UPDATED"));
        return "success";
    }

    private void fillError(Model model, String errorKey){
        model.addAttribute(ERROR_TITLE, resourceBundle.getString("ERROR"));
        model.addAttribute(ERROR_MESSAGE, resourceBundle.getString(errorKey));
    }

}