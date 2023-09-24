package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.statisticCommand;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.ZnatokiUser;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.steps.AddTime;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AddTImeCommand extends CommandHandler {

    private final Redis redis;
    private final Storage storage;

    private final ServicesMapper servicesMapper;

    @Override
    public void handle(Message message) {
        ZnatokiUser user = redis.getUser(message.getChatId())
                .onFailure(error -> sender.sendText("Внутрення ошибка", message.getFrom()))
                .get();

        if (storage.contain(message.getChatId())){
            sender.sendText("Сначала сохраните текущую запись", message.getFrom(), false);
            return;
        }

        Time time = new Time();
        time.setDateTime(LocalDateTime.now());
        time.setChatId(message.getChatId());

        if (user.services().size() == 1){
            time.setServiceId(user.services().get(0));
        }

        storage.setTime(message.getChatId(), time);

        if (user.services().size() != 1){
            ReplyKeyboardBuilder builder = ReplyKeyboardBuilder.create();
            builder.setText("Выберите предмет");

            user.services().forEach(it -> builder.row().button(servicesMapper.getNameById(it)).endRow());
            sender.send(builder.build(message.getChatId()), message.getFrom());
            storage.setStage(message.getChatId(), AddTime.INPUT_SUBJECT);
        } else {
            sender.sendText("Введите фио. /complete - для окончания ввода", message.getFrom());
            InlineKeyboardBuilder builder = InlineKeyboardBuilder.create()
                    .row().switchToInline().endRow();
            builder.setText("нажмите для поиска фио");
            sender.send(builder.build(message.getChatId()), message.getFrom());
            storage.setStage(message.getChatId(), AddTime.INPUT_FIO);
        }
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/addTime");
    }
}
