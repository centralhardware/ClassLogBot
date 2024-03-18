package me.centralhardware.znatoki.telegram.statistic.telegram

import me.centralhardware.znatoki.telegram.statistic.entity.Client
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle
import java.util.concurrent.atomic.AtomicInteger

@Component
class InlineHandler(
    private val clientService: ClientService,
    private val sender: TelegramSender,
    private val userMapper: UserMapper,
    private val organizationMapper: OrganizationMapper
) {

    fun processInline(update: Update): Boolean {
        update.takeIf { it.hasInlineQuery() } ?: return false

        val inlineQuery = update.inlineQuery
        val text = inlineQuery.query
        if (StringUtils.isBlank(text)) return true

        val i = AtomicInteger()
        val articles = clientService.search(text)
            .filter { it.organizationId == userMapper.getById(inlineQuery.from.id)!!.organizationId }
            .filterNot(Client::deleted)
            .map {
                InlineQueryResultArticle.builder()
                    .title(getFio(it))
                    .description(getBio(it))
                    .id(i.getAndIncrement().toString())
                    .inputMessageContent(
                        InputTextMessageContent.builder()
                            .messageText(getFio(it))
                            .disableWebPagePreview(false)
                            .build()
                    ).build()
            }.toList()

        val answerInlineQuery = AnswerInlineQuery.builder()
            .results(articles)
            .inlineQueryId(inlineQuery.id)
            .isPersonal(true)
            .cacheTime(0)
            .build()

        sender.send{execute(answerInlineQuery)}

        return true
    }

    private fun getFio(client: Client): String = "${client.id} ${client.fio()}"

    private fun getBio(client: Client): String {
        val inline = organizationMapper.getInlineFields(client.organizationId)
        return client.properties
            .filter { inline.contains(it.name) && StringUtils.isNotBlank(it.value) }
            .joinToString(" ") { "${it.value} ${it.name}" }
    }
}