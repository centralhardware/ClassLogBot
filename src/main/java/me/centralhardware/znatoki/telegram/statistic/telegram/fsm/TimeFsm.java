package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import io.vavr.concurrent.Future;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Payment;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.EmployNameMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.PaymentMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServiceMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.minio.Minio;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.validate.AmountValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.FioValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.PhotoValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.ServiceValidator;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.centralhardware.znatoki.telegram.statistic.telegram.fsm.steps.AddTime.*;

@Component
@RequiredArgsConstructor
public class TimeFsm extends Fsm {

    private final TelegramUtil telegramUtil;
    private final Minio minio;
    private final Redis redis;

    private final ServiceMapper serviceMapper;
    private final PaymentMapper paymentMapper;
    private final EmployNameMapper employNameMapper;
    private final ServicesMapper servicesMapper;
    private final PupilService pupilService;

    private final FioValidator fioValidator;
    private final AmountValidator amountValidator;
    private final PhotoValidator photoValidator;
    private final ServiceValidator serviceValidator;

    @Override
    public void process(Update update) {
        Long userId = telegramUtil.getUserId(update);
        String text = Optional.of(update)
                .map(Update::getMessage)
                .map(Message::getText)
                .orElse(null);
        var znatokiUser = redis.getUser(userId).get();

        User user = telegramUtil.getFrom(update);
        switch (storage.getStage(userId)) {
            case INPUT_SUBJECT -> serviceValidator.validate(Pair.of(text, znatokiUser.organizationId())).peekLeft(
                    error -> sender.sendText(error, user)
            ).peek(service -> {
                storage.getTime(userId).setServiceId(servicesMapper.getServiceId(znatokiUser.organizationId(), service));
                sender.sendText("Введите фио. /complete - для окончания ввода", user);
                InlineKeyboardBuilder builder = InlineKeyboardBuilder.create()
                        .row().switchToInline().endRow();
                builder.setText("нажмите для поиска фио");
                sender.send(builder.build(userId), update.getMessage().getFrom());
                storage.setStage(userId, INPUT_FIO);
            });
            case INPUT_FIO -> {
                if (Objects.equals(text, "/complete")){
                    if (storage.getTime(userId).getFios().isEmpty()) {
                        sender.sendText("Необходимо ввести как минимум одно ФИО", user);
                        return;
                    }

                    sender.sendText("Введите стоимость занятия", user);
                    storage.setStage(userId, INPUT_AMOUNT);
                    return;
                }

                fioValidator.validate(text).peekLeft(
                        error -> sender.sendText(error, user)
                ).peek(
                        fio -> {
                            Integer id = Integer.valueOf(text.split(" ")[0]);
                            String name = text.replace(id + " ", "");
                            if (storage.getTime(userId).getFios().contains(Pair.of(id, name))){
                                sender.sendText("Данное ФИО уже добавлено", user);
                                return;
                            }
                            storage.getTime(userId).getFios().add(id);
                            sender.sendText("ФИО сохранено", user);
                        }
                );
            }
            case INPUT_AMOUNT -> amountValidator.validate(text).peekLeft(
                    error -> sender.sendText(error, user)
            ).peek(
                    amount -> {
                        storage.getTime(userId).setAmount(amount);
                        sender.sendText("Отправьте фото отчётности", user);
                        storage.setStage(userId, INPUT_PHOTO);
                    }
            );
            case INPUT_PHOTO -> photoValidator.validate(update).peekLeft(
                    error -> sender.sendText(error, user)
            ).peek(
                    report -> {
                        GetFile getFile = new GetFile();
                        getFile.setFileId(report.getFileId());

                        var time = storage.getTime(userId);

                        File file = sender.downloadFile(getFile)
                                .onFailure(error -> {
                                    sender.sendText("Ошибка при добавление занятия. Попробуйте снова", user);
                                    storage.remove(userId);
                                })
                                .get();

                        String id = minio.upload(file, time.getDateTime(), employNameMapper.getFio(time.getChatId()), servicesMapper.getKeyById(time.getServiceId()), "znatoki")
                                .onFailure(error -> {
                                    sender.sendText("Ошибка при сохранение фотографии. Попробуйте снова", user);
                                    storage.remove(userId);
                                })
                                .get();
                        storage.getTime(userId).setPhotoId(id);

                        ReplyKeyboardBuilder builder = ReplyKeyboardBuilder.create()
                                .setText(STR."""
                                        Предмет: \{ servicesMapper.getNameById(time.getServiceId())}
                                        ФИО:\{String.join((CharSequence) ";", time.getFios().stream().map(pupilService::getFioById).toList())}
                                        стоимость занятия: \{time.getAmount().toString()}
                                        """)
                                .row().button("да").endRow()
                                .row().button("нет").endRow();

                        sender.send(builder.build(userId), user);
                        storage.setStage(userId, CONFIRM);

                    }
            );
            case CONFIRM -> {
                if (Objects.equals(text, "да")) {
                    var time = storage.getTime(userId);
                    var id = UUID.randomUUID();
                    time.getFios().forEach(it -> {
                        time.setPupilId(it);
                        time.setId(id);
                        time.setOrganizationId(redis.getUser(userId).get().organizationId());
                        serviceMapper.insertTime(time);
                        var payment = new Payment();
                        payment.setDateTime(LocalDateTime.now());
                        payment.setPupilId(time.getPupilId());
                        payment.setAmount(time.getAmount() * -1);
                        payment.setTimeId(time.getId());
                        payment.setOrganizationId(redis.getUser(userId).get().organizationId());
                        paymentMapper.insert(payment);
                    });

                    sendLog(time, userId);

                    storage.remove(userId);

                    sender.sendText("Сохранено", user);
                } else if (Objects.equals(text, "нет")) {
                    Future.of(() -> {
                        minio.delete(storage.getTime(userId).getPhotoId(), "znatoki")
                                .onFailure(error -> sender.send("Ошибка при удаление фотографии", user));
                        storage.remove(userId);
                        return null;
                    }).onSuccess(error -> sender.sendText("Удалено", user));
                }
            }
        }
    }

    private void sendLog(Time time, Long userId){
        getLogUser(userId)
                .ifPresent(user -> {
                    var keybard = InlineKeyboardBuilder.create()
                            .setText("?")
                            .row()
                            .button("удалить", "timeDelete-" + time.getId())
                            .endRow().build();
                    SendPhoto sendPhoto = SendPhoto
                            .builder()
                            .photo(new InputFile(minio.get(time.getPhotoId(), "znatoki")
                                    .onFailure(error -> sender.sendText("Ошибка во время отправки лога", user))
                                    .get(), "отчет"))
                            .chatId(user.getId())
                            .caption(STR."""
                        #занятие
                        Время: \{time.getDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm"))}
                        Предмет: #\{servicesMapper.getNameById(time.getServiceId()).replaceAll(" ", "_")}
                        Ученики: \{time.getFios().stream()
                                    .map(it -> "#" + pupilService.getFioById(it).replaceAll(" ", "_"))
                                    .collect(Collectors.joining(", "))}
                        Стоимость: \{time.getAmount()}
                        Преподаватель: #\{ employNameMapper.getFio(userId).replaceAll(" ", "_")}
                        """)
                            .replyMarkup(keybard)
                            .build();
                    sender.send(sendPhoto, user);
                });
    }

    @Override
    public boolean isActive(Long chatId) {
        return storage.containTime(chatId);
    }
}
