package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import io.vavr.concurrent.Future;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder;
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Payment;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Service;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.*;
import me.centralhardware.znatoki.telegram.statistic.service.MinioService;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.utils.PropertyUtils;
import me.centralhardware.znatoki.telegram.statistic.validate.AmountValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.FioValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.ServiceValidator;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

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
    private final MinioService minioService;

    private final ServiceMapper serviceMapper;
    private final PaymentMapper paymentMapper;
    private final EmployNameMapper employNameMapper;
    private final ServicesMapper servicesMapper;
    private final ClientService clientService;
    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;

    private final FioValidator fioValidator;
    private final AmountValidator amountValidator;
    private final ServiceValidator serviceValidator;

    @Override
    public void process(Update update) {
        Long userId = telegramUtil.getUserId(update);
        String text = Optional.of(update)
                .map(Update::getMessage)
                .map(Message::getText)
                .orElse(null);
        var znatokiUser = userMapper.getById(userId);
        var org =  organizationMapper.getById(znatokiUser.getOrganizationId());

        User user = telegramUtil.getFrom(update);
        switch (storage.getStage(userId)) {
            case INPUT_SUBJECT -> serviceValidator.validate(Pair.of(text, znatokiUser.getOrganizationId())).peekLeft(
                    error -> sender.sendText(error, user)
            ).peek(service -> {
                storage.getTime(userId).setServiceId(servicesMapper.getServiceId(znatokiUser.getOrganizationId(), service));
                sender.sendText("Введите фио. /complete - для окончания ввода", user);
                InlineKeyboardBuilder builder = InlineKeyboardBuilder.create()
                        .row().switchToInline().endRow();
                builder.setText("нажмите для поиска фио");
                sender.send(builder.build(userId), update.getMessage().getFrom());
                storage.setStage(userId, INPUT_FIO);
            });
            case INPUT_FIO -> {
                if (Objects.equals(text, "/complete")){
                    if (storage.getTime(userId).getServiceIds().isEmpty()) {
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
                            if (storage.getTime(userId).getServiceIds().contains(Pair.of(id, name))){
                                sender.sendText("Данное ФИО уже добавлено", user);
                                return;
                            }
                            storage.getTime(userId).getServiceIds().add(id);
                            sender.sendText("ФИО сохранено", user);
                        }
                );
            }
            case INPUT_AMOUNT -> amountValidator.validate(text).peekLeft(
                    error -> sender.sendText(error, user)
            ).peek(
                    amount -> {
                        storage.getTime(userId).setAmount(amount);

                        if (org.getServiceCustomProperties() == null ||
                                org.getServiceCustomProperties().isEmpty()){
                            storage.setStage(userId, CONFIRM);

                            var time = storage.getTime(userId);
                            ReplyKeyboardBuilder builder = ReplyKeyboardBuilder.create()
                                    .setText(STR."""
                                        услуга: \{ servicesMapper.getNameById(time.getServiceId())}
                                        ФИО:\{String.join(";", time.getServiceIds().stream().map(clientService::getFioById).toList())}
                                        стоимость: \{time.getAmount().toString()}
                                        Сохранить?
                                        """)
                                    .row().button("да").endRow()
                                    .row().button("нет").endRow();
                            sender.send(builder.build(userId), user);
                        } else {
                            storage.setStage(userId, INPUT_PROPERTIES);
                            storage.getTime(userId).setPropertiesBuilder(new PropertiesBuilder(org.getServiceCustomProperties().propertyDefs()));
                            var next = storage.getTime(userId).getPropertiesBuilder().getNext().get();
                            if (!next.getRight().isEmpty()){
                                var builder = ReplyKeyboardBuilder
                                        .create()
                                        .setText(next.getLeft());
                                next.getRight().forEach(it -> builder.row().button(it).endRow());
                                sender.send(builder.build(userId), user);
                            } else {
                                sender.sendText(next.getLeft(), user);
                            }
                            storage.setStage(userId, INPUT_PROPERTIES);
                        }
                    }
            );
            case INPUT_PROPERTIES -> processCustomProperties(update, storage.getTime(userId).getPropertiesBuilder(), properties -> {
                var time = storage.getTime(userId);
                time.setProperties(properties);
                ReplyKeyboardBuilder builder = ReplyKeyboardBuilder.create()
                        .setText(STR."""
                                        услуга: \{ servicesMapper.getNameById(time.getServiceId())}
                                        ФИО:\{String.join(";", time.getServiceIds().stream().map(clientService::getFioById).toList())}
                                        стоимость: \{time.getAmount().toString()}
                                        Сохранить?
                                        """)
                        .row().button("да").endRow()
                        .row().button("нет").endRow();
                sender.send(builder.build(userId), user);
                storage.setStage(userId, CONFIRM);
            });
            case CONFIRM -> {
                if (Objects.equals(text, "да")) {
                    var service = storage.getTime(userId);
                    var id = UUID.randomUUID();
                    service.getServiceIds().forEach(it -> {
                        service.setPupilId(it);
                        service.setId(id);
                        service.setOrganizationId(userMapper.getById(userId).getOrganizationId());
                        serviceMapper.insertTime(service);
                        var payment = new Payment();
                        payment.setDateTime(LocalDateTime.now());
                        payment.setPupilId(service.getPupilId());
                        payment.setAmount(service.getAmount() * -1);
                        payment.setTimeId(service.getId());
                        payment.setOrganizationId(userMapper.getById(userId).getOrganizationId());
                        paymentMapper.insert(payment);
                    });

                    sendLog(service, userId);

                    storage.remove(userId);

                    sender.sendText("Сохранено", user);
                } else if (Objects.equals(text, "нет")) {
                    Future.of(() -> {
                        storage.getTime(userId)
                                .getProperties()
                                .stream()
                                .filter(it -> it.type() instanceof Photo)
                                .forEach(photo -> minioService.delete(photo.value())
                                        .onFailure(error -> sender.send("Ошибка при удаление фотографии", user)));
                        storage.remove(userId);
                        return null;
                    }).onSuccess(error -> sender.sendText("Удалено", user));
                }
            }
        }
    }

    private void sendLog(Service service, Long userId){
        getLogUser(userId)
                .ifPresent(user -> {
                    var keybard = InlineKeyboardBuilder.create()
                            .setText("?")
                            .row()
                            .button("удалить", "timeDelete-" + service.getId())
                            .endRow().build();

                    var text =STR."""
                        #занятие
                        Время: \{ service.getDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm"))}
                        Предмет: #\{servicesMapper.getNameById(service.getServiceId()).replaceAll(" ", "_")}
                        Ученики: \{ service.getServiceIds().stream()
                            .map(it -> "#" + clientService.getFioById(it).replaceAll(" ", "_"))
                            .collect(Collectors.joining(", "))}
                        Стоимость: \{ service.getAmount()}
                        Преподаватель: #\{ employNameMapper.getFio(userId).replaceAll(" ", "_")}
                        \{ PropertyUtils.print(service.getProperties())}
                        """;

                    var hasPhoto = service.getProperties()
                            .stream()
                            .filter(it -> it.type() instanceof Photo)
                            .count();

                    if (hasPhoto == 1){
                        service.getProperties()
                                .stream()
                                .filter(it -> it.type() instanceof Photo)
                                .forEach(photo -> {
                                    SendPhoto sendPhoto = SendPhoto
                                            .builder()
                                            .photo(new InputFile(minioService.get(photo.value())
                                                    .onFailure(error -> sender.sendText("Ошибка во время отправки лога", user))
                                                    .get(), "отчет"))
                                            .chatId(user.getId())
                                            .caption(text)
                                            .replyMarkup(keybard)
                                            .build();
                                    sender.send(sendPhoto, user);
                                });
                    } else {
                        var message = SendMessage
                                .builder()
                                .chatId(user.getId())
                                .text(text)
                                .replyMarkup(keybard);
                        sender.send(message.build(), user);
                    }
                });
    }

    @Override
    public boolean isActive(Long chatId) {
        return storage.containTime(chatId);
    }
}
