package me.centralhardware.znatoki.telegram.statistic.service

import com.github.difflib.text.DiffRowGenerator

object DiffService {

    private val diffGenerator = DiffRowGenerator.create()
        .showInlineDiffs(true)
        .inlineDiffByWord(true)
        .oldTag { if (it) "<span class=\"diff-deleted\">" else "</span>" }
        .newTag { if (it) "<span class=\"diff-inserted\">" else "</span>" }
        .columnWidth(Integer.MAX_VALUE)
        .build()

    fun generateHtmlDiff(changes: Map<String, Pair<String?, String?>>): String {
        if (changes.isEmpty()) return ""

        val html = StringBuilder()

        changes.forEach { (field, valuePair) ->
            val (oldValue, newValue) = valuePair
            val fieldLabel = formatFieldLabel(field)

            html.append("<div><b>$fieldLabel:</b> ")

            when {
                oldValue != null && newValue != null -> {
                    val rows = diffGenerator.generateDiffRows(listOf(oldValue), listOf(newValue))
                    if (rows.isNotEmpty()) {
                        val row = rows[0]
                        html.append(row.oldLine)
                        html.append(" → ")
                        html.append(row.newLine)
                    }
                }
                oldValue != null -> html.append("<del>$oldValue</del>")
                newValue != null -> html.append("<ins>$newValue</ins>")
            }

            html.append("</div>")
        }

        return html.toString()
    }

    private fun formatFieldLabel(field: String): String {
        return when (field) {
            "name" -> "Имя"
            "secondName" -> "Фамилия"
            "lastName" -> "Отчество"
            "schoolClass" -> "Класс"
            "phone" -> "Телефон"
            "responsiblePhone" -> "Телефон ответственного"
            "motherFio" -> "ФИО матери"
            "birthDate" -> "Дата рождения"
            "source" -> "Источник"
            "recordDate" -> "Дата записи"
            "forceGroup" -> "Групповое занятие"
            "extraHalfHour" -> "1.5 часа"
            "amount" -> "Сумма"
            else -> field
        }
    }
}
