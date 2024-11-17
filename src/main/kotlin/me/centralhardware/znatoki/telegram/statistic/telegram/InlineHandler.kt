package me.centralhardware.znatoki.telegram.statistic.telegram

import dev.inmo.tgbotapi.Trace
import dev.inmo.tgbotapi.extensions.api.answers.answerInlineQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onBaseInlineQuery
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.types.InlineQueryId
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.centralhardware.znatoki.telegram.statistic.entity.Client
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.mapper.ConfigMapper
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.atomic.AtomicInteger

suspend fun BehaviourContext.processInline() = onBaseInlineQuery {
    val text = it.query
    if (StringUtils.isBlank(text)) return@onBaseInlineQuery

    Trace.save("searchUserInline", mapOf("query" to text))

    val i = AtomicInteger()
    val clients = ClientService.search(text)
    val articles = clients.map {
        InlineQueryResultArticle(
            InlineQueryId(i.getAndIncrement().toString()),
            getFio(it),
            InputTextMessageContent(getFio(it)),
            description = getBio(it),
        )
    }
        .toMutableList()

    if (articles.isEmpty()) {
        val noResultsArticle =
            InlineQueryResultArticle(
                InlineQueryId(i.getAndIncrement().toString()),
                "/complete",
                InputTextMessageContent("Закончить ввод"),
            )
        articles.add(noResultsArticle)
    }

    answerInlineQuery(it, results = articles, isPersonal = true, cachedTime = 0)
    coroutineScope {
        launch {
            clients.forEach { client ->
                Trace.save(
                    "searchUserInlineResult",
                    mapOf("query" to text, "userId" to client.id.toString()),
                )
            }
        }
    }
}

private fun getFio(client: Client): String = "${client.id} ${client.fio()}"

private fun getBio(client: Client): String {
    val inline = ConfigMapper.includeInInline()
    return client.properties
        .filter { inline.contains(it.name) && StringUtils.isNotBlank(it.value) }
        .joinToString(" ") { "${it.value} ${it.name}" }
}
