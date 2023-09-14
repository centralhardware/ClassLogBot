package me.centralhardware.znatoki.telegram.statistic.telegram;

import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.entity.Pupil;
import me.centralhardware.znatoki.telegram.statistic.service.PupilService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class InlineHandler {

    private final PupilService pupilService;
    private final TelegramSender sender;

    public boolean processInline(Update update) throws InterruptedException {
        if (!update.hasInlineQuery()) return false;

        InlineQuery inlineQuery = update.getInlineQuery();
        String text = inlineQuery.getQuery();

        AtomicInteger i = new AtomicInteger();
        List<InlineQueryResultArticle> articles = pupilService.search(text)
                .stream()
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

        sender.send(answerInlineQuery, inlineQuery.getFrom());

        return true;
    }

    private String getFio(Pupil pupil){
        return pupil.getName().toLowerCase() + " " +
                pupil.getSecondName().toLowerCase() + " " +
                pupil.getLastName().toLowerCase();
    }

    private String getBio(Pupil pupil){
        return String.format("%s класс %s лет", pupil.getClassNumber(), ChronoUnit.YEARS.between(pupil.getDateOfBirth(), LocalDateTime.now()));
    }

}
