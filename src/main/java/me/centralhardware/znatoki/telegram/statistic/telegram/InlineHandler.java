package me.centralhardware.znatoki.telegram.statistic.telegram;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.postgres.Client;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.OrganizationMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.UserMapper;
import me.centralhardware.znatoki.telegram.statistic.service.ClientService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Component
@RequiredArgsConstructor
public class InlineHandler {

    private final ClientService clientService;
    private final TelegramSender sender;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;

    public boolean processInline(Update update) {
        if (!update.hasInlineQuery()) return false;

        InlineQuery inlineQuery = update.getInlineQuery();
        String text = inlineQuery.getQuery();

        if (StringUtils.isBlank(text)) return true;

        AtomicInteger i = new AtomicInteger();
        List<InlineQueryResultArticle> articles = clientService.search(text)
                .stream()
                .filter(it -> it.getOrganizationId().equals(userMapper.getById(inlineQuery.getFrom().getId()).getOrganizationId()))
                .filter(not(Client::isDeleted))
                .map(it -> InlineQueryResultArticle.builder()
                        .title(getFio(it))
                        .description(getBio(it))
                        .id(String.valueOf(i.getAndIncrement()))
                        .inputMessageContent(InputTextMessageContent.builder()
                                .messageText(getFio(it))
                                .disableWebPagePreview(false)
                                .build())
                        .build())
                .toList();
        AnswerInlineQuery answerInlineQuery = AnswerInlineQuery
                .builder()
                .results(articles)
                .inlineQueryId(inlineQuery.getId())
                .isPersonal(true)
                .cacheTime(0)
                .build();

        sender.send(answerInlineQuery);

        return true;
    }

    private String getFio(Client client){
        return STR."\{client.getId()} \{client.getName()} \{client.getSecondName()} \{client.getLastName()}";
    }

    private String getBio(Client client){
        var inline = organizationMapper.getInlineFields(client.getOrganizationId());
        return client.getProperties()
                .stream()
                .filter(it -> inline.contains(it.name()))
                .filter(it -> StringUtils.isNotBlank(it.value()))
                .map(it -> STR."\{it.value()} \{it.name()}")
                .collect(Collectors.joining(" "));
    }

}
