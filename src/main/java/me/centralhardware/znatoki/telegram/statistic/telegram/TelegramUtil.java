package me.centralhardware.znatoki.telegram.statistic.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.centralhardware.telegram.bot.common.ClickhouseRuben;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramUtil {

    private final ClickhouseRuben clickhouse = new ClickhouseRuben();

    public Long getUserId(Update update){
        if (update.hasMessage()){
            return update.getMessage().getFrom().getId();
        } else if (update.hasCallbackQuery()){
            return update.getCallbackQuery().getFrom().getId();
        } else if (update.hasInlineQuery()){
            return update.getInlineQuery().getFrom().getId();
        }

        throw new IllegalStateException();
    }

    public static User getFrom(Update update){
        if (update.hasMessage()){
            return update.getMessage().getFrom();
        } else if (update.hasCallbackQuery()){
            return update.getCallbackQuery().getFrom();
        } else if (update.hasInlineQuery()){
            return update.getInlineQuery().getFrom();
        }

        throw new IllegalStateException();
    }

    private static String getText(Update update){
        if (update.hasMessage()){
            return update.getMessage().getText();
        } else if (update.hasCallbackQuery()){
            return update.getCallbackQuery().getData();
        } else if (update.hasInlineQuery()){
            return update.getInlineQuery().getQuery();
        }

        throw new IllegalStateException();
    }

    public void logUpdate(Update update){
        if (update.hasMessage()){
            Message message = update.getMessage();
            log.info("Receive message username: {}, firstName: {}, lastName: {}, id: {}, text: {}",
                    message.getFrom().getUserName(),
                    message.getFrom().getFirstName(),
                    message.getFrom().getLastName(),
                    message.getFrom().getId(),
                    message.getText());
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            log.info("Receive callback username: {}, firstName: {}, lastName: {}, id: {}, callbackDate: {}",
                    callbackQuery.getFrom().getUserName(),
                    callbackQuery.getFrom().getFirstName(),
                    callbackQuery.getFrom().getLastName(),
                    callbackQuery.getFrom().getId(),
                    callbackQuery.getData());
        }
    }

    public void saveStatisticIncome(Update update){
        clickhouse.log(getText(update) == null? "": getText(update),
                update.hasInlineQuery(),
                getFrom(update),
                "znatokiStatistic");
        log.info(STR."Save to clickHouse income(\{getUserId(update)}, \{getText(update)})");
    }

    private static final String BOLD_MAKER = "*";


    /**
     * @param text message to make bold
     * @return text wrapped in markdown bold symbol
     */
    public static String makeBold(String text) {
        return BOLD_MAKER + text + BOLD_MAKER;
    }

    /**
     * @param text number to make bold
     * @return text wrapped in markdown bold symbol
     */
    public static String makeBold(Integer text) {
        if (text == null) return "";

        return BOLD_MAKER + text + BOLD_MAKER;
    }

}
