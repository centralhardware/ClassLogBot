package me.centralhardware.znatoki.telegram.statistic.telegram;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.lucen.Lucene;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class InlineHandler {

    private final Lucene lucene;
    private final TelegramSender sender;

    public boolean processInline(Update update){
        if (!update.hasInlineQuery()) return false;

        InlineQuery inlineQuery = update.getInlineQuery();
        String text = inlineQuery.getQuery();

        AtomicInteger i = new AtomicInteger();
        List<InlineQueryResultArticle> articles = lucene.search(text)
                .stream()
                .map(it -> InlineQueryResultArticle.builder()
                        .title(it)
                        .id(String.valueOf(i.getAndIncrement()))
                        .inputMessageContent(InputTextMessageContent.builder()
                                .messageText(it)
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

        sender.send(answerInlineQuery, inlineQuery.getFrom());

        return true;
    }

}
