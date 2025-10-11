package me.centralhardware.znatoki.telegram.statistic.service

import de.danielbechler.diff.ObjectDifferBuilder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object DiffService {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    fun <T> generateHtmlDiff(
        oldObj: T?,
        newObj: T?
    ): String {
        if (oldObj == null && newObj == null) return ""

        val differ = ObjectDifferBuilder.startBuilding()
            .comparison().ofType(LocalDateTime::class.java).toUseEqualsMethod()
            .and()
            .comparison().ofType(LocalDate::class.java).toUseEqualsMethod()
            .and()
            .comparison().ofType(LocalTime::class.java).toUseEqualsMethod()
            .and()
            .comparison().ofType(Instant::class.java).toUseEqualsMethod()
            .and()
            .build()
        val diff = differ.compare(newObj, oldObj)
        
        val changes = mutableMapOf<String, Pair<String?, String?>>()
        
        diff.visit { node, _ ->
            if (node.hasChanges() && !node.hasChildren()) {
                val fieldPath = node.path.toString()
                val fieldName = fieldPath.removePrefix("/")
                
                if (fieldName.isNotEmpty()) {
                    val oldValue = formatValue(node.canonicalGet(oldObj))
                    val newValue = formatValue(node.canonicalGet(newObj))
                    changes[fieldName] = oldValue to newValue
                }
            }
        }
        
        return formatChangesToHtml(changes)
    }

    private fun formatValue(value: Any?): String? {
        return when (value) {
            null -> null
            is LocalDateTime -> value.format(dateTimeFormatter)
            else -> value.toString()
        }
    }

    private fun formatChangesToHtml(changes: Map<String, Pair<String?, String?>>): String {
        if (changes.isEmpty()) return ""

        val html = StringBuilder()

        changes.forEach { (field, valuePair) ->
            val (oldValue, newValue) = valuePair
            val fieldLabel = formatFieldLabel(field)

            html.append("<div><b>$fieldLabel:</b> ")

            when {
                oldValue != null && newValue != null -> {
                    html.append("<del>$oldValue</del> → <ins>$newValue</ins>")
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
            "tutorId" -> "Репетитор"
            "subjectId" -> "Предмет"
            "studentId" -> "Ученик"
            "_amount" -> "Сумма"
            else -> field
        }
    }
}
