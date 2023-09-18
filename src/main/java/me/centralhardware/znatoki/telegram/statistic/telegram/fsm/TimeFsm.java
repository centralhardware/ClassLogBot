package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import io.vavr.concurrent.Future;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.Config;
import me.centralhardware.znatoki.telegram.statistic.Storage;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Payment;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.PaymentMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.TeacherNameMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.minio.Minio;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.validate.AmountValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.EnumValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.FioValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.PhotoValidator;
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
public class TimeFsm implements Fsm {

    private final Storage storage;
    private final TelegramUtil telegramUtil;
    private final TelegramSender sender;
    private final Minio minio;

    private final TimeMapper timeMapper;
    private final PaymentMapper paymentMapper;
    private final TeacherNameMapper teacherNameMapper;

    private final EnumValidator enumValidator;
    private final FioValidator fioValidator;
    private final AmountValidator amountValidator;
    private final PhotoValidator photoValidator;

    @Override
    public void process(Update update) {
        Long userId = telegramUtil.getUserId(update);
        String text = Optional.of(update)
                .map(Update::getMessage)
                .map(Message::getText)
                .orElse(null);

        User user = telegramUtil.getFrom(update);
        switch (storage.getStage(userId)) {
            case INPUT_SUBJECT -> enumValidator.validate(text).peekLeft(
                    error -> sender.sendText(error, user)
            ).peek(subject -> {
                storage.getTime(userId).setSubject(subject.name());
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
                            if (storage.getTime(userId).getFios().contains(fio)){
                                sender.sendText("Данное ФИО уже добавлено", user);
                                return;
                            }
                            Integer id = Integer.valueOf(text.split(" ")[0]);
                            String name = text.replace(id + " ", "");
                            storage.getTime(userId).getFios().add(Pair.of(name, id));
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

                        String id = minio.upload(file, time.getDateTime(), teacherNameMapper.getFio(time.getChatId()), time.getSubject(), "znatoki")
                                .onFailure(error -> {
                                    sender.sendText("Ошибка при сохранение фотографии. Попробуйте снова", user);
                                    storage.remove(userId);
                                })
                                .get();
                        storage.getTime(userId).setPhotoId(id);

                        ReplyKeyboardBuilder builder = ReplyKeyboardBuilder.create()
                                .setText(String.format("""
                                        Предмет: %s,
                                        ФИО: %s
                                        стоимость занятия: %s
                                        """,
                                        Subject.valueOf(time.getSubject()).getRusName(),
                                        String.join(";", time.getFios().stream().map(Pair::getLeft).toList()),
                                        time.getAmount().toString()))
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
                        time.setFio(it.getLeft());
                        time.setPupilId(it.getRight());
                        time.setId(id);
                        timeMapper.insertTime(time);
                        var payment = new Payment();
                        payment.setDateTime(LocalDateTime.now());
                        payment.setPupilId(time.getPupilId());
                        payment.setAmount(time.getAmount() * -1);
                        payment.setTimeId(time.getId());
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
                    }).onSuccess(it -> sender.sendText("Удалено", user));
                }
            }
        }
    }

    private void sendLog(me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time time, Long userId){
        var logUser = new User();
        logUser.setId(Config.getLogChatId());
        logUser.setUserName("logger");
        logUser.setLanguageCode("ru");
        SendPhoto sendPhoto = SendPhoto
                .builder()
                .photo(new InputFile(minio.get(time.getPhotoId(), "znatoki")
                        .onFailure(error -> sender.sendText("Ошибка во время отправки лога", logUser))
                        .get(), "отчет"))
                .chatId(logUser.getId())
                .caption(String.format("""
                                            #занятие
                                            Время: %s,
                                            Предмет: %s
                                            Ученики: %s
                                            Стоимость: %s,
                                            Преподаватель: %s
                                            """,
                        time.getDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm")),
                        "#" + Subject.valueOf(time.getSubject()).getRusName().replaceAll(" ", "_"),
                        time.getFios().stream()
                                .map(it -> "#" + it.getLeft().replaceAll(" ", "_"))
                                .collect(Collectors.joining(", ")),
                        time.getAmount(),
                        "#" + teacherNameMapper.getFio(userId).replaceAll(" ", "_")))
                .build();
        sender.send(sendPhoto, logUser);
        var keybard = InlineKeyboardBuilder.create()
                .setText("?")
                .row()
                .button("удалить", "timeDelete-" + time.getId())
                .endRow().build(logUser.getId());
        sender.send(keybard, logUser);
    }

    @Override
    public boolean isActive(Long chatId) {
        return storage.containTime(chatId);
    }
}
