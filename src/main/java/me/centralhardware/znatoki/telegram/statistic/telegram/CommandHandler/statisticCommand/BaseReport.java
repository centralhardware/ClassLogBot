package me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.statisticCommand;

import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Role;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.UserMapper;
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.ServiceMapper;
import me.centralhardware.znatoki.telegram.statistic.service.TelegramService;
import me.centralhardware.znatoki.telegram.statistic.telegram.CommandHandler.CommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.io.File;
import java.util.List;
import java.util.function.Function;

@Component
public abstract class BaseReport extends CommandHandler {

    @Autowired
    private ServiceMapper serviceMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private Storage storage;
    @Autowired
    private TelegramService telegramService;

    @Override
    public void handle(Message message) {
        var id = message.getFrom().getId();

        if (storage.contain(id)){
            return;
        }

        if (userMapper.getById(id) != null  && !telegramService.isAdmin(id)){
            getTime().apply(id)
                    .forEach(it -> send(it, message.getFrom()));
            return;
        }

        if (!telegramService.isAdmin(id)){
            return;
        }

        serviceMapper.getIds(userMapper.getById(id).getOrganizationId())
                .forEach(it -> getTime().apply(it)
                        .forEach(report -> send(report, message.getFrom())));
    }

    @Override
    public Role getRequiredRole() {
        return Role.READ;
    }

    protected abstract Function<Long, List<File>> getTime();


    private void send(File file, User from){
        SendDocument sendDocument = SendDocument
                .builder()
                .chatId(from.getId())
                .document(new InputFile(file))
                .build();
        sender.send(sendDocument, from);
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

}
