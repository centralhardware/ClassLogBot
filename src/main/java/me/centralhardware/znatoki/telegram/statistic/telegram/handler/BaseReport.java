package me.centralhardware.znatoki.telegram.statistic.telegram.handler;

import me.centralhardware.znatoki.telegram.statistic.mapper.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.redis.Redis;
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
public abstract class BaseReport extends CommandHandler{

    @Autowired
    private TimeMapper timeMapper;
    @Autowired
    private Redis redis;

    @Override
    public void handle(Message message) {
        var id = message.getFrom().getId();

        if (redis.exists(id.toString()) && !id.equals(Long.parseLong(System.getenv("ADMIN_ID")))){
            getTime().apply(id)
                    .forEach(it -> send(it, message.getFrom()));
            return;
        }

        if (!id.equals(Long.parseLong(System.getenv("ADMIN_ID")))){
            return;
        }

        timeMapper.getIds()
                .forEach(it -> getTime().apply(it)
                        .forEach(report -> send(report, message.getFrom())));
    }

    abstract Function<Long, List<File>> getTime();


    private void send(File file, User from){
        SendDocument sendDocument = SendDocument
                .builder()
                .chatId(from.getId())
                .document(new InputFile(file))
                .build();
        sender.send(sendDocument, from);
        file.delete();
    }

}
