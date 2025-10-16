package me.centralhardware.znatoki.telegram.statistic.web

import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.attributes.*
import me.centralhardware.znatoki.telegram.statistic.dto.*

@Composable
fun LessonModal(
    lesson: LessonDto?,
    subjects: List<SubjectDto>,
    tutorId: Long?,
    onClose: () -> Unit,
    onCreate: (CreateLessonRequest) -> Unit,
    onUpdate: (UpdateLessonRequest) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var date by remember { mutableStateOf(lesson?.dateTime ?: "") }
    var selectedSubjectId by remember { mutableStateOf(lesson?.subject?.id?.toString() ?: "") }
    var amount by remember { mutableStateOf(lesson?.amount?.toString() ?: "") }
    var isGroup by remember { mutableStateOf(lesson?.isGroup ?: false) }
    var isExtra by remember { mutableStateOf(lesson?.isExtra ?: false) }

    Modal(isOpen = true, onClose = onClose) {
        H3({ style { marginBottom(20.px) } }) {
            Text(if (lesson != null) "Редактировать занятие" else "Новое занятие")
        }

        FormGroup("Дата") {
            TextInput(
                value = date,
                onValueChange = { date = it },
                type = "datetime-local"
            )
        }

        FormGroup("Предмет") {
            SelectInput(
                value = selectedSubjectId,
                options = listOf("" to "Выберите предмет") + subjects.map { it.id.toString() to it.name },
                onValueChange = { selectedSubjectId = it }
            )
        }

        FormGroup("Сумма") {
            TextInput(
                value = amount,
                onValueChange = { amount = it },
                type = "number",
                placeholder = "0"
            )
        }

        Div({ style { marginBottom(16.px) } }) {
            Label {
                Input(InputType.Checkbox) {
                    if (isGroup) {
                        attr("checked", "checked")
                    }
                    onChange { isGroup = !isGroup }
                    style { property("margin-right", "8px") }
                }
                Text("Групповое занятие")
            }
        }

        Div({ style { marginBottom(16.px) } }) {
            Label {
                Input(InputType.Checkbox) {
                    if (isExtra) {
                        attr("checked", "checked")
                    }
                    onChange { isExtra = !isExtra }
                    style { property("margin-right", "8px") }
                }
                Text("Дополнительное")
            }
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                property("gap", "12px")
                marginTop(24.px)
            }
        }) {
            PrimaryButton("Сохранить") {
                val subjectId = selectedSubjectId.toLongOrNull()
                val amountInt = amount.toIntOrNull()
                if (subjectId != null && amountInt != null) {
                    if (lesson != null) {
                        // Update existing lesson
                        val studentId = lesson.students.firstOrNull()?.id
                        if (studentId != null) {
                            onUpdate(
                                UpdateLessonRequest(
                                    studentId = studentId,
                                    subjectId = subjectId,
                                    amount = amountInt,
                                    forceGroup = isGroup,
                                    extraHalfHour = isExtra
                                )
                            )
                        }
                    } else {
                        // Create new lesson - student selection needs to be added
                        // For now, this is incomplete and needs student selection UI
                        console.log("Creating new lesson - student selection not implemented yet")
                    }
                }
            }
            SecondaryButton("Отмена") {
                onClose()
            }
            onDelete?.let { delete ->
                SecondaryButton("Удалить") {
                    delete()
                }
            }
        }
    }
}

@Composable
fun PaymentModal(
    payment: PaymentDto?,
    subjects: List<SubjectDto>,
    tutorId: Long?,
    onClose: () -> Unit,
    onCreate: (CreatePaymentRequest) -> Unit,
    onUpdate: (UpdatePaymentRequest) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var date by remember { mutableStateOf(payment?.dateTime ?: "") }
    var amount by remember { mutableStateOf(payment?.amount?.toString() ?: "") }
    var selectedSubjectId by remember { mutableStateOf(payment?.subject?.id?.toString() ?: "") }

    Modal(isOpen = true, onClose = onClose) {
        H3({ style { marginBottom(20.px) } }) {
            Text(if (payment != null) "Редактировать оплату" else "Новая оплата")
        }

        FormGroup("Дата") {
            TextInput(
                value = date,
                onValueChange = { date = it },
                type = "datetime-local"
            )
        }

        FormGroup("Предмет") {
            SelectInput(
                value = selectedSubjectId,
                options = listOf("" to "Выберите предмет") + subjects.map { it.id.toString() to it.name },
                onValueChange = { selectedSubjectId = it }
            )
        }

        FormGroup("Сумма") {
            TextInput(
                value = amount,
                onValueChange = { amount = it },
                type = "number",
                placeholder = "0"
            )
        }

        Div({
            style {
                display(DisplayStyle.Flex)
                property("gap", "12px")
                marginTop(24.px)
            }
        }) {
            PrimaryButton("Сохранить") {
                val subjectId = selectedSubjectId.toLongOrNull()
                val amountInt = amount.toIntOrNull()
                if (subjectId != null && amountInt != null) {
                    if (payment != null) {
                        // Update existing payment
                        onUpdate(
                            UpdatePaymentRequest(
                                amount = amountInt,
                                subjectId = subjectId
                            )
                        )
                    } else {
                        // Create new payment - student selection needs to be added
                        console.log("Creating new payment - student selection not implemented yet")
                    }
                }
            }
            SecondaryButton("Отмена") {
                onClose()
            }
            onDelete?.let { delete ->
                SecondaryButton("Удалить") {
                    delete()
                }
            }
        }
    }
}
