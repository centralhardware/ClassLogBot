package me.centralhardware.znatoki.telegram.statistic.telegram.handler;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.Config;
import me.centralhardware.znatoki.telegram.statistic.clickhouse.model.Subject;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.Role;
import me.centralhardware.znatoki.telegram.statistic.redis.dto.ZnatokiUser;
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
        if (!message.getFrom().getId().equals(Config.getAdminId())){
            return;
        }

        String messasge = message.getText();

        String chatId = messasge.split(" ")[1];
        String role = messasge.split(" ")[2];
        List<Subject> subjects = Arrays.stream(messasge.replace("/register " + chatId + " " + role + " ", "")
                        .split(" "))
                .map(Subject::valueOf)
                .toList();

        ZnatokiUser user = ZnatokiUser.builder()
                .subjects(subjects)
                .role(Role.of(role))
                .build();

        redis.put(chatId, user);

        sender.sendText("сохранено", message.getFrom());
    }

    @Override
    public boolean isAcceptable(String data) {
        return data.startsWith("/register");
    }
}
