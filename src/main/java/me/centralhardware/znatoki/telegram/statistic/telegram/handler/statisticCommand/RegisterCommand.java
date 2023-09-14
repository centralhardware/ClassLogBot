package me.centralhardware.znatoki.telegram.statistic.telegram.handler.statisticCommand;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.ZnatokiUser;
import me.centralhardware.znatoki.telegram.statistic.telegram.handler.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RegisterCommand extends CommandHandler {

    private final Redis redis;

    @Override
    public void handle(Message message) {
        if (!message.getFrom().getId().equals(Long.parseLong(System.getenv("ADMIN_ID")))){
            return;
        }

        String messasge = message.getText();

        String chatId = messasge.split(" ")[1];
        List<Subject> subjects = Arrays.stream(messasge.replace("/register " + chatId + " ", "")
                        .split(" "))
                .map(Subject::valueOf)
                .toList();

        ZnatokiUser user = ZnatokiUser.builder()
                .subjects(subjects)
                .build();

        redis.put(chatId, user);

        sender.sendText("сохранено", message.getFrom());
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("/register");
    }
}
