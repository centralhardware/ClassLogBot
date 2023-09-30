package me.centralhardware.znatoki.telegram.statistic.telegram.fsm;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder;
import me.centralhardware.znatoki.telegram.statistic.entity.Payment;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.EmployNameMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.PaymentMapper;
import me.centralhardware.znatoki.telegram.statistic.minio.Minio;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender;
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramUtil;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.steps.AddPayment;
import me.centralhardware.znatoki.telegram.statistic.validate.AmountValidator;
import me.centralhardware.znatoki.telegram.statistic.validate.FioValidator;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Objects;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class PaymentFsm extends Fsm {

    private final TelegramUtil telegramUtil;
    private final TelegramSender sender;
    private final Minio minio;
    private final Redis redis;

    private final EmployNameMapper employNameMapper;
    private final PaymentMapper paymentMapper;
    private final ClientService clientService;
    private final OrganizationMapper organizationMapper;

    private final FioValidator fioValidator;
    private final AmountValidator amountValidator;

    @Override
    public void process(Update update) {
        Long userId = telegramUtil.getUserId(update);
        String text = Optional.of(update)
                .map(Update::getMessage)
                .map(Message::getText)
                .orElse(null);
        var znatokiUser = redis.getUser(userId).get();

        User user = telegramUtil.getFrom(update);
        switch (storage.getPaymentStage(userId)){
            case INPUT_PUPIL -> fioValidator.validate(text).peekLeft(
                    error -> sender.sendText(error, user)
            ).peek(
                    fio -> {
                        Integer id = Integer.valueOf(text.split(" ")[0]);
                        storage.getPayment(userId).setPupilId(id);
                        storage.setPaymentStage(userId, AddPayment.INPUT_AMOUNT);
                        sender.sendText("Введите сумму оплаты", user);
                    }
            );
            case INPUT_AMOUNT -> amountValidator.validate(text).peekLeft(
                    error -> sender.sendText(error, user)
            ).peek(
                    amount -> {
                        storage.getPayment(userId).setAmount(amount);

                        var org = organizationMapper.getById(znatokiUser.organizationId());
                        var payment = storage.getPayment(userId);

                        if (org.getPaymentCustomProperties() == null ||
                                org.getPaymentCustomProperties().isEmpty()){
                            ReplyKeyboardBuilder builder = ReplyKeyboardBuilder.create()
                                    .setText(String.format("""
                                        ФИО: %s
                                        Оплата: %s
                                        """,
                                            clientService.findById(payment.getPupilId()).get().getFio(),
                                            payment.getAmount()))
                                    .row().button("да").endRow()
                                    .row().button("нет").endRow();
                            sender.send(builder.build(userId), user);
                        } else {
                            storage.getPayment(userId).setPropertiesBuilder(new PropertiesBuilder(org.getServiceCustomProperties().propertyDefs()));
//                            sender.sendText(storage.getPayment(userId).getPropertiesBuilder().getNext().get(), user);
                            storage.setPaymentStage(userId, AddPayment.INPUT_PROPERTIES);
                        }

                    }
            );
//            case INPUT_PROPERTIES -> storage.getTime(userId).getPropertiesBuilder()
//                    .validate(update)
//                    .toEither()
//                    .peekLeft(error -> sender.sendText(error, user))
//                    .peek(it -> storage.getTime(userId).getPropertiesBuilder().getNext()
//                            .ifPresentOrElse(
//                                    next -> sender.sendText(next, user),
//                                    () -> storage.setStage(userId, CONFIRM)));
            case CONFIRM -> {
                if (Objects.equals(text, "да")) {
                    var payment = storage.getPayment(userId);
                    payment.setOrganizationId(redis.getUser(userId).get().organizationId());

                    sendLog(payment, userId);

                    paymentMapper.insert(payment);

                    sender.sendText("Сохранено", user);
                } else if (Objects.equals(text, "нет")) {

                }
                storage.remove(userId);
            }
        }
    }

    private void sendLog(Payment payment, Long userId) {
//        getLogUser(userId)
//                .ifPresent(user -> {
//                    SendPhoto sendPhoto = SendPhoto
//                            .builder()
//                            .photo(new InputFile(minio.get(payment.getPhotoId(), "znatoki-payment")
//                                    .onFailure(error -> sender.sendText("Ошибка во время отправки лога", user))
//                                    .get(), "отчет"))
//                            .chatId(user.getId())
//                            .caption(STR."""
//                                #оплата
//                                Время: \{payment.getDateTime().format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm"))},
//                                Ученик: #\{ clientService.findById(payment.getPupilId()).get().getFio().replaceAll(" ", "_")}
//                                оплачено: \{payment.getAmount()},
//                                Принял оплату: #\{ employNameMapper.getFio(userId).replaceAll(" ", "_")}
//                                """)
//                            .build();
//                    sender.send(sendPhoto, user);
//                });
    }

    @Override
    public boolean isActive(Long chatId) {
        return storage.containsPayment(chatId);
    }
}
