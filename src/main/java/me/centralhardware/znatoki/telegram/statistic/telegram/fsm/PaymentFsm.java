package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import io.vavr.concurrent.Future;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder;
import me.centralhardware.znatoki.telegram.statistic.eav.types.Photo;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Payment;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.PaymentMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.UserMapper;
import me.centralhardware.znatoki.telegram.statistic.service.MinioService;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.steps.AddPayment;
import me.centralhardware.znatoki.telegram.statistic.utils.PropertyUtils;
import me.centralhardware.znatoki.telegram.statistic.validate.AmountValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.FioValidator;
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


@Component
@RequiredArgsConstructor
public class PaymentFsm extends Fsm {

    private final TelegramSender sender;
    private final MinioService minioService;

    private final PaymentMapper paymentMapper;
    private final ClientService clientService;
    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;

    private final FioValidator fioValidator;
    private final AmountValidator amountValidator;

    @Override
    public void process(Update update) {
        Long userId = telegramUtil.getUserId(update);
        String text = Optional.of(update)
                .map(Update::getMessage)
                .map(Message::getText)
                .orElse(null);
        var znatokiUser = userMapper.getById(userId);

        User user = TelegramUtil.getFrom(update);
        switch (storage.getPaymentStage(userId)){
            case ADD_PUPIL -> fioValidator.validate(text).peekLeft(
                    error -> sender.sendText(error, user)
            ).peek(
                    fio -> {
                        Integer id = Integer.valueOf(text.split(" ")[0]);
                        storage.getPayment(userId).setClientId(id);
                        storage.setPaymentStage(userId, AddPayment.ADD_AMOUNT);
                        sender.sendText("Введите сумму оплаты", user);
                    }
            );
            case ADD_AMOUNT -> amountValidator.validate(text).peekLeft(
                    error -> sender.sendText(error, user)
            ).peek(
                    amount -> {
                        storage.getPayment(userId).setAmount(amount);

                        var org = organizationMapper.getById(znatokiUser.getOrganizationId());
                        var payment = storage.getPayment(userId);

                        if (org.getPaymentCustomProperties() == null ||
                                org.getPaymentCustomProperties().isEmpty()){
                            ReplyKeyboardBuilder builder = ReplyKeyboardBuilder.create()
                                    .setText(String.format("""
                                        ФИО: %s
                                        Оплата: %s
                                        """,
                                            clientService.findById(payment.getClientId()).get().getFio(),
                                            payment.getAmount()))
                                    .row().button("да").endRow()
                                    .row().button("нет").endRow();
                            sender.send(builder.build(userId), user);
                            storage.setPaymentStage(userId, AddPayment.CONFIRM);
                        } else {
                            storage.getPayment(userId).setPropertiesBuilder(new PropertiesBuilder(org.getPaymentCustomProperties().propertyDefs()));
                            storage.setPaymentStage(userId, AddPayment.ADD_PROPERTIES);
                            var next = storage.getPayment(userId).getPropertiesBuilder().getNext().get();
                            if (!next.getRight().isEmpty()){
                                var builder = ReplyKeyboardBuilder
                                        .create()
                                        .setText(next.getLeft());
                                next.getRight().forEach(it -> builder.row().button(it).endRow());
                                sender.send(builder.build(userId), user);
                            } else {
                                sender.sendText(next.getLeft(), user);
                            }
                        }

                    }
            );
            case ADD_PROPERTIES -> processCustomProperties(update, storage.getPayment(userId).getPropertiesBuilder(), properties -> {
                var payment = storage.getPayment(userId);
                payment.setProperties(properties);
                var builder = ReplyKeyboardBuilder.create()
                        .setText(String.format("""
                                        ФИО: %s
                                        Оплата: %s
                                        """,
                                clientService.findById(payment.getClientId()).get().getFio(),
                                payment.getAmount()))
                        .row().button("да").endRow()
                        .row().button("нет").endRow();
                sender.send(builder.build(userId), user);
                storage.setPaymentStage(userId, AddPayment.CONFIRM);
            });
            case CONFIRM -> {
                if (Objects.equals(text, "да")) {
                    var payment = storage.getPayment(userId);
                    payment.setOrganizationId(userMapper.getById(userId).getOrganizationId());
                    payment.setDateTime(LocalDateTime.now());

                    paymentMapper.insert(payment);
                    sendLog(payment, payment.getId(), userId, payment.getOrganizationId());

                    sender.sendText("Сохранено", user);
                } else if (Objects.equals(text, "нет")) {
                    Future.run(() -> {
                        storage.getPayment(userId)
                                .getProperties()
                                .stream()
                                .filter(it -> it.type() instanceof Photo)
                                .forEach(photo -> minioService.delete(photo.value())
                                        .onFailure(error -> sender.send("Ошибка при удаление фотографии", user)));
                        storage.remove(userId);
                    }).onSuccess(it -> sender.sendText("Удалено", user));
                }
                storage.remove(userId);
            }
        }
    }

    private void sendLog(Payment payment, Integer paymentId,Long userId, UUID organizationId) {
        getLogUser(userId)
                .ifPresent(user -> {
                    var keybard = InlineKeyboardBuilder.create()
                            .setText("?")
                            .row()
                            .button("удалить", STR."paymentDelete-\{paymentId}")
                            .endRow().build();

                    var text = STR."""
                                #оплата
                                Время: \{payment.getDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm"))},
                                \{organizationMapper.getById(organizationId).getClientName()}: #\{ clientService.findById(payment.getClientId()).get().getFio().replaceAll(" ", "_")}
                                оплачено: \{payment.getAmount()},
                                Принял оплату: #\{ userMapper.getById(userId).getName().replaceAll(" ", "_")}
                                \{ PropertyUtils.print(payment.getProperties())}
                                """;

                    var hasPhoto = payment.getProperties()
                            .stream()
                            .filter(it -> it.type() instanceof Photo)
                            .count();

                    if (hasPhoto == 1){
                        payment.getProperties()
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
                        var message = SendMessage.builder()
                                .text(text)
                                .chatId(user.getId())
                                .replyMarkup(keybard)
                                .build();
                        sender.send(message, user);
                    }
                });
    }

    @Override
    public boolean isActive(Long chatId) {
        return storage.containsPayment(chatId);
    }
}
