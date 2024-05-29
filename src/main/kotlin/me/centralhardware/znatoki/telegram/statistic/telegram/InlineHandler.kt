package me.centralhardware.znatoki.telegram.statistic.telegram

import dev.inmo.tgbotapi.extensions.api.answers.answerInlineQuery
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.types.InlineQueries.query.BaseInlineQuery
import dev.inmo.tgbotapi.types.InlineQueryId
import me.centralhardware.znatoki.telegram.statistic.bot
import me.centralhardware.znatoki.telegram.statistic.entity.Client
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.atomic.AtomicInteger

suspend fun processInline(query: BaseInlineQuery) {
    val text = query.query
    if (StringUtils.isBlank(text)) return

    val i = AtomicInteger()
    val articles = ClientService.search(text)
        .filter { it.organizationId == UserMapper.findById(query.user.id.chatId.long)!!.organizationId }
        .map {
            InlineQueryResultArticle(
                InlineQueryId(i.getAndIncrement().toString()),
                getFio(it),
                InputTextMessageContent(
                    getFio(it)
                ),
                description = getBio(it)
            )
        }.toMutableList()

    if (articles.isEmpty()) {
        val noResultsArticle = InlineQueryResultArticle(
            InlineQueryId(i.getAndIncrement().toString()),
            "/complete",
            InputTextMessageContent("Закончить ввод")
        )
        articles.add(noResultsArticle)
    }

    bot.answerInlineQuery(query, results = articles, isPersonal = true, cachedTime = 0)
}

private fun getFio(client: Client): String = "${client.id} ${client.fio()}"

private fun getBio(client: Client): String {
    val inline = OrganizationMapper.getInlineFields(client.organizationId)
    return client.properties
        .filter { inline.contains(it.name) && StringUtils.isNotBlank(it.value) }
        .joinToString(" ") { "${it.value} ${it.name}" }
}