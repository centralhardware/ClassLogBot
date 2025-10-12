package me.centralhardware.znatoki.telegram.statistic.service

import de.danielbechler.diff.ObjectDifferBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.StudentId
import me.centralhardware.znatoki.telegram.statistic.entity.SubjectId
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Generates HTML diffs between two objects for audit trail visualization.
 * Automatically handles LocalDateTime, LocalDate, and other Java Time types.
 * Used for tracking entity changes in the audit log.
 */
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

                // Skip deleted and id fields
                if (fieldName.isEmpty() || fieldName == "deleted" || fieldName == "id") {
                    return@visit
                }

                val oldValue = formatValue(fieldName, node.canonicalGet(oldObj))
                val newValue = formatValue(fieldName, node.canonicalGet(newObj))
                changes[fieldName] = oldValue to newValue
            }
        }

        return formatChangesToHtml(changes)
    }

    private fun formatValue(fieldName: String, value: Any?): String? {
        return when {
            value == null -> null
            value is LocalDateTime -> value.format(dateTimeFormatter)
            value is Boolean -> if (value) "Да" else "Нет"
            value is StudentId -> {
                try {
                    val fio = StudentMapper.getFioById(value)
                    formatFioWithInitials(fio)
                } catch (e: Exception) {
                    "ID: ${value.id}"
                }
            }
            value is TutorId -> {
                try {
                    TutorMapper.findByIdOrNull(value)?.name ?: "ID: ${value.id}"
                } catch (e: Exception) {
                    "ID: ${value.id}"
                }
            }
            value is SubjectId -> {
                try {
                    SubjectMapper.getNameById(value)
                } catch (e: Exception) {
                    "ID: ${value.id}"
                }
            }
            fieldName == "photoReport" && value is String -> {
                "<a href='/api/image/$value' target='_blank'>Фото</a>"
            }
            else -> value.toString()
        }
    }

    private fun formatFioWithInitials(fio: String): String {
        val parts = fio.trim().split("\\s+".toRegex())
        return when (parts.size) {
            3 -> "${parts[0]} ${parts[1].firstOrNull()?.uppercase() ?: ""}. ${parts[2].firstOrNull()?.uppercase() ?: ""}."
            2 -> "${parts[0]} ${parts[1].firstOrNull()?.uppercase() ?: ""}."
            else -> fio
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
            "photoReport" -> "Фото отчётности"
            "dateTime" -> "Дата и время"
            else -> field
        }
    }
}
