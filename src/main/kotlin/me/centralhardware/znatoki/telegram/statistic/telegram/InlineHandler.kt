package me.centralhardware.znatoki.telegram.statistic.telegram

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.tgbotapi.extensions.api.answers.answerInlineQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onBaseInlineQuery
import dev.inmo.tgbotapi.types.InlineQueries.InlineQueryResult.InlineQueryResultArticle
import dev.inmo.tgbotapi.types.InlineQueries.InputMessageContent.InputTextMessageContent
import dev.inmo.tgbotapi.types.InlineQueryId
import me.centralhardware.znatoki.telegram.statistic.entity.Student
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.fio
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.StudentService
import org.apache.commons.lang3.StringUtils

val noResultsArticle =
    InlineQueryResultArticle(
        InlineQueryId("1"),
        "Закончить ввод",
        InputTextMessageContent("/complete"),
    )

enum class InlineSearchType {
    STUDENT,
    TUTOR
}

fun BehaviourContext.processInline() = onBaseInlineQuery {
    val text = it.query
    val articles: MutableList<InlineQueryResultArticle> = mutableListOf()

    // Определяем тип поиска по префиксу (используется для разделения контекста)
    val (searchType, searchQuery) = when {
        text.startsWith("t:") -> InlineSearchType.TUTOR to text.removePrefix("t:").trim()
        text.startsWith("s:") -> InlineSearchType.STUDENT to text.removePrefix("s:").trim()
        else -> InlineSearchType.STUDENT to text // По умолчанию ищем студентов
    }

    if (StringUtils.isBlank(searchQuery)) {
        articles.add(noResultsArticle)
    } else {
        when (searchType) {
            InlineSearchType.STUDENT -> {
                val searchResults = StudentService.search(searchQuery)
                KSLog.info("Inline search for students '$searchQuery': found ${searchResults.size} students")
                searchResults.forEachIndexed { i, student ->
                    articles.add(InlineQueryResultArticle(
                        InlineQueryId(i.toString()),
                        getStudentFio(student),
                        InputTextMessageContent(getStudentFio(student)),
                        description = getStudentBio(student),
                    ))
                }
            }
            InlineSearchType.TUTOR -> {
                val searchResults = TutorMapper.search(searchQuery)
                KSLog.info("Inline search for tutors '$searchQuery': found ${searchResults.size} tutors")
                searchResults.forEachIndexed { i, tutor ->
                    articles.add(InlineQueryResultArticle(
                        InlineQueryId(i.toString()),
                        getTutorFio(tutor),
                        InputTextMessageContent(getTutorFio(tutor)),
                        description = getTutorBio(tutor),
                    ))
                }
            }
        }

        if (articles.isEmpty()) {
            articles.add(noResultsArticle)
        }
    }

    answerInlineQuery(it, results = articles, isPersonal = true, cachedTime = 0)
}

private fun getStudentFio(student: Student): String = "${student.id?.id} ${student.fio()}"
private fun getStudentBio(student: Student): String = "${student.schoolClass?.value?: ""} класс"

private fun getTutorFio(tutor: Tutor): String = "${tutor.id?.id} ${tutor.name}"
private fun getTutorBio(tutor: Tutor): String = "Репетитор"
