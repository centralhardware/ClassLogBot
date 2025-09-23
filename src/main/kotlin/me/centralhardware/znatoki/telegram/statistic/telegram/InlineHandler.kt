package me.centralhardware.znatoki.telegram.statistic.telegram

import dev.inmo.tgbotapi.extensions.api.answers.answerInlineQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onBaseInlineQuery
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.types.InlineQueryId
import me.centralhardware.znatoki.telegram.statistic.entity.Student
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.atomic.AtomicInteger

fun BehaviourContext.processInline() = onBaseInlineQuery {
    val text = it.query
    if (StringUtils.isBlank(text)) return@onBaseInlineQuery


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
}

private fun getFio(student: Student): String = "${student.id?.id} ${student.fio()}"

private fun getBio(student: Student): String = "${student.schoolClass?.value} класс"
