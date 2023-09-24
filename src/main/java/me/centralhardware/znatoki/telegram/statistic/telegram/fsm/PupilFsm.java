package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.znatoki.telegram.statistic.entity.Enum.HowToKnow;
import me.centralhardware.znatoki.telegram.statistic.entity.Pupil;
import me.centralhardware.znatoki.telegram.statistic.i18n.ErrorConstant;
import me.centralhardware.znatoki.telegram.statistic.i18n.MessageConstant;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.validate.ClassNumberValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.DateValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.TelephoneValidator;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
@Slf4j
public class PupilFsm extends Fsm {

    private final TelegramService telegramService;
    private final PupilService pupilService;
    private final ResourceBundle resourceBundle;
    private final TelegramSender sender;
    private final Redis redis;

    private final TimeMapper timeMapper;
    private final ServicesMapper servicesMapper;

    private final ClassNumberValidator classNumberValidator;
    private final DateValidator dateValidator;
    private final TelephoneValidator telephoneValidator;

    @Override
    public void process(Update update) {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        var user = update.getMessage().getFrom();

        if (telegramService.isUnauthorized(chatId) || !telegramService.hasWriteRight(chatId)){
            sender.sendMessageFromResource(ErrorConstant.ACCESS_DENIED, user);
            return;
        }

        switch (storage.getPupilStage(chatId)){
            case INPUT_FIO -> {
                String[] words = text.split(" ");
                if (!(words.length >= 2 && words.length <= 3)) {
                    sender.sendMessageFromResource(MessageConstant.INPUT_FIO_REQUIRED_FORMAT, user);
                    return;
                }
                me.centralhardware.znatoki.telegram.statistic.entity.Pupil pupil = storage.getPupil(chatId);
                if (words.length == 3){
                    pupil.setSecondName(words[0]);
                    pupil.setName(words[1]);
                    pupil.setLastName(words[2]);
                } else {
                    pupil.setSecondName(words[0]);
                    pupil.setName(words[1]);
                    pupil.setLastName("");
                }
                if (pupilService.checkExistenceByFio(pupil.getName(), pupil.getSecondName(), pupil.getLastName())){
                    sender.sendMessageFromResource(MessageConstant.FIO_ALREADY_IN_DATABASE, user);
                    return;
                }
                next(chatId);
                sender.sendMessageFromResource(MessageConstant.INPUT_CLASS, user);
            }
            case INPUT_CLASS_NUMBER -> {
                if (text.equals(SKIP_COMMAND)){
                    getPupil(chatId).setClassNumber(null);
                    sender.sendMessageFromResource(MessageConstant.INPUT_DATE_IN_FORMAT, user);
                    next(chatId);
                    return;
                }

                classNumberValidator.validate(text)
                        .peekLeft(error -> sender.sendText(error, user))
                        .peek(classNumber -> {
                            getPupil(chatId).setClassNumber(classNumber);
                            sender.sendMessageFromResource(MessageConstant.INPUT_DATE_IN_FORMAT, user);
                            next(chatId);
                        });
            }
            case INPUT_DATE_OF_RECORD -> {

                dateValidator.validate(text)
                        .peekLeft(error -> sender.sendText(error, user))
                        .peek(date -> {
                            if (date.getYear() < 119){
                                sender.sendMessageFromResource(MessageConstant.YEAR_OF_RECORD_LOW_THEN_2019, user);
                                return;
                            }
                            getPupil(chatId).setDateOfRecord(date.atStartOfDay());
                            next(chatId);
                            sender.sendMessageFromResource(MessageConstant.INPUT_DATE_OF_BIRTH_IN_FORMAT, user);
                        });
            }
            case INPUT_DATE_OF_BIRTH -> {
                dateValidator.validate(text)
                        .peekLeft(error -> sender.sendText(error, user))
                        .peek(date -> {
                            getPupil(chatId).setDateOfBirth(date.atStartOfDay());
                            next(chatId);
                            sender.sendMessageFromResource(MessageConstant.INPUT_PUPIL_TEL, user);
                        });
            }
            case INPUT_TELEPHONE -> {
                if (text.equals(SKIP_COMMAND)){
                    next(chatId);
                    getPupil(chatId).setTelephone("");
                    sender.sendMessageFromResource(MessageConstant.INPUT_RESPONSIBLE_TEL, user);
                    return;
                }

                telephoneValidator.validate(text)
                        .peekLeft(error -> sender.sendText(error, user))
                        .peek(telephone -> {
                            if (pupilService.existByTelephone(telephone)){
                                sender.sendMessageFromResource(MessageConstant.TEL_ALREADY_EXIST, user);
                                return;
                            }
                            getPupil(chatId).setTelephone(telephone);
                            next(chatId);
                            sender.sendMessageFromResource(MessageConstant.INPUT_RESPONSIBLE_TEL, user);
                        });
            }
            case INPUT_TELEPHONE_OF_RESPONSIBLE -> {
                if (text.equals(SKIP_COMMAND)){
                    next(chatId);
                    ReplyKeyboardBuilder replyKeyboardBuilder = ReplyKeyboardBuilder.
                            create().
                            setText(resourceBundle.getString("INPUT_HOW_TO_KNOW"));
                    for (HowToKnow howToKnow : HowToKnow.values()){
                        replyKeyboardBuilder.
                                row().button(howToKnow.getRusName()).endRow();
                    }
                    sender.send(replyKeyboardBuilder.build(chatId), user);
                    return;
                }

                telephoneValidator.validate(text)
                        .peekLeft(error -> sender.sendText(error, user))
                        .peek(telephone -> {
                            getPupil(chatId).setTelephoneResponsible(telephone);
                            next(chatId);
                            ReplyKeyboardBuilder replyKeyboardBuilder = ReplyKeyboardBuilder.
                                    create().
                                    setText(resourceBundle.getString("INPUT_HOW_TO_KNOW"));
                            for (HowToKnow howToKnow : HowToKnow.values()){
                                replyKeyboardBuilder.
                                        row().button(howToKnow.getRusName()).endRow();
                            }
                            sender.send(replyKeyboardBuilder.build(chatId), user);
                        });
            }
            case INPUT_HOW_TO_KNOW -> {
                if (HowToKnow.validate(text)){
                    getPupil(chatId).setHowToKnow(HowToKnow.getConstant(text));
                    next(chatId);
                    sender.sendMessageFromResource(MessageConstant.INPUT_MOTHER_NAME, user);
                } else {
                    sender.sendMessageFromResource(MessageConstant.NOT_FOUND, user, false);
                }
            }
            case INPUT_MOTHER_NAME -> {
                if (!text.equals("/skip")){
                    getPupil(chatId).setMotherName(text);
                }

                getPupil(chatId).setOrganizationId(redis.getUser(chatId).get().organizationId());
                getPupil(chatId).setCreated_by(chatId);
                sender.sendMessageWithMarkdown(pupilService.save(getPupil(chatId)).getInfo(timeMapper.getServicesForPupil(getPupil(chatId).getId()).stream().map(servicesMapper::getNameById).toList()), user);
                sendLog(getPupil(chatId));
                storage.remove(chatId);
                sender.sendMessageFromResource(MessageConstant.CREATE_PUPIL_FINISHED, user);
            }
        }
    }

    public Pupil getPupil(Long chatId){
        return storage.getPupil(chatId);
    }

    public void next(Long chatId){
        var next = storage.getPupilStage(chatId).next();
        storage.setPupilStage(chatId, next);
    }


    private static final String SKIP_COMMAND = "/skip";

    @Override
    public boolean isActive(Long chatId) {
        return storage.containsPupil(chatId);
    }

    private void sendLog(Pupil pupil) {
        var message = SendMessage.builder()
                .text("#ученик\n" + pupil.getInfo(timeMapper.getServicesForPupil(pupil.getId()).stream().map(servicesMapper::getNameById).toList()))
                .chatId(getLogUser().getId())
                .build();
        sender.send(message, getLogUser());
    }

}
