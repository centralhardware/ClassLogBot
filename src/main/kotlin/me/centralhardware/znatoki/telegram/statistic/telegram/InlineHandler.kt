package me.centralhardware.znatoki.telegram.statistic.telegram

import dev.inmo.tgbotapi.extensions.api.answers.answerInlineQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onBaseInlineQuery
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.types.InlineQueryId
import me.centralhardware.znatoki.telegram.statistic.entity.Student
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.service.StudentService
import org.apache.commons.lang3.StringUtils

val noResultsArticle =
    InlineQueryResultArticle(
        InlineQueryId("1"),
        "/complete",
        InputTextMessageContent("Закончить ввод"),
    )

fun BehaviourContext.processInline() = onBaseInlineQuery {
    val text = it.query
    val articles: MutableList<InlineQueryResultArticle> = mutableListOf()
    if (StringUtils.isBlank(text)) {
        articles.add(noResultsArticle)
    } else {
        StudentService.search(text)
            .forEachIndexed { i, student ->
                articles.add(InlineQueryResultArticle(
                    InlineQueryId(i.toString()),
                    getFio(student),
                    InputTextMessageContent(getFio(student)),
                    description = getBio(student),
                ))
            }
        if (articles.isEmpty()) {
            articles.add(noResultsArticle)
        }
    }

    answerInlineQuery(it, results = articles, isPersonal = true, cachedTime = 0)
}

private fun getFio(student: Student): String = "${student.id?.id} ${student.fio()}"

private fun getBio(student: Student): String = "${student.schoolClass?.value?: ""} класс"
