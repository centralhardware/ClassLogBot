package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.statisticCommand;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Role;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Service;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServicesMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.UserMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.TelegramUser;
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

    private final Storage storage;

    private final UserMapper userMapper;
    private final ServicesMapper servicesMapper;

    @Override
    public void handle(Message message) {
        TelegramUser user = userMapper.getById(message.getFrom().getId());

        if (storage.contain(message.getChatId())){
            sender.sendText("Сначала сохраните текущую запись", message.getFrom(), false);
            return;
        }

        Service service = new Service();
        service.setDateTime(LocalDateTime.now());
        service.setChatId(message.getChatId());

        if (user.getServices().size() == 1){
            service.setServiceId(user.getServices().getFirst());
        }

        storage.setTime(message.getChatId(), service);

        if (user.getServices().size() != 1){
            ReplyKeyboardBuilder builder = ReplyKeyboardBuilder.create();
            builder.setText("Выберите предмет");

            user.getServices().forEach(it -> builder.row().button(servicesMapper.getNameById(it)).endRow());
            sender.send(builder.build(message.getChatId()), message.getFrom());
            storage.setStage(message.getChatId(), AddTime.ADD_SUBJECT);
        } else {
            sender.sendText("Введите фио. /complete - для окончания ввода", message.getFrom());
            InlineKeyboardBuilder builder = InlineKeyboardBuilder.create()
                    .row().switchToInline().endRow();
            builder.setText("нажмите для поиска фио");
            sender.send(builder.build(message.getChatId()), message.getFrom());
            storage.setStage(message.getChatId(), AddTime.ADD_FIO);
        }
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.equalsIgnoreCase("/addTime");
    }

    @Override
    public Role getRequiredRole() {
        return Role.READ;
    }

}
