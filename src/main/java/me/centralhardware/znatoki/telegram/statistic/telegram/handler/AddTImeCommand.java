package me.centralhardware.znatoki.telegram.statistic.telegram.handler;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.Storage;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Time;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.ZnatokiUser;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.InlineKeyboardBuilder;
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.ReplyKeyboardBuilder;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AddTImeCommand extends CommandHandler{

    private final Redis redis;
    private final Storage storage;

    @Override
    public void handle(Message message) {
        ZnatokiUser user = redis.get(message.getChatId().toString(), ZnatokiUser.class);

        Time time = new Time();
        time.setDateTime(LocalDateTime.now());
        time.setChatId(message.getChatId());

        if (user.subjects().size() == 1){
            time.setSubject(user.subjects().get(0).toString());
        }

        storage.setTime(message.getChatId(), time);

        if (user.subjects().size() != 1){
            ReplyKeyboardBuilder builder = ReplyKeyboardBuilder.create();
            builder.setText("Выберите предмет");

            user.subjects().forEach(it -> builder.row().button(it.getRusName()).endRow());
            sender.send(builder.build(message.getChatId()), message.getFrom());
            storage.setStage(message.getChatId(), 1);
        } else {
            sender.sendText("Введите фио. /complete - для окончания ввода", message.getFrom());
            InlineKeyboardBuilder builder = InlineKeyboardBuilder.create()
                    .row().switchToInline().endRow();
            builder.setText("нажмите для поиска фио");
            sender.send(builder.build(message.getChatId()), message.getFrom());
            storage.setStage(message.getChatId(), 2);
        }
    }

    @Override
    boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/addTime");
    }
}
