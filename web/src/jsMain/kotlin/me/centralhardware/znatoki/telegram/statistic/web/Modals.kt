package me.centralhardware.znatoki.telegram.statistic.web

import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.attributes.*
import me.centralhardware.znatoki.telegram.statistic.dto.*
import kotlinx.coroutines.launch

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
    var selectedSubjectId by remember { mutableStateOf(lesson?.subject?.id?.toString() ?: "") }
    var amount by remember { mutableStateOf(lesson?.amount?.toString() ?: "") }
    var isGroup by remember { mutableStateOf(lesson?.isGroup ?: false) }
    var isExtra by remember { mutableStateOf(lesson?.isExtra ?: false) }
    var selectedFile by remember { mutableStateOf<org.w3c.files.File?>(null) }
    var previewUrl by remember { mutableStateOf<String?>(lesson?.photoReport) }
    var isUploading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Modal(isOpen = true, onClose = onClose) {
        H3({ style { marginBottom(20.px) } }) {
            Text(if (lesson != null) "Редактировать занятие" else "Новое занятие")
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

        FormGroup("Фото отчета") {
            FileInput(
                selectedFile = selectedFile,
                previewUrl = previewUrl,
                onFileSelected = { file ->
                    selectedFile = file
                    // Create preview URL
                    val reader = org.w3c.files.FileReader()
                    reader.onload = { event ->
                        previewUrl = event.target.asDynamic().result as String
                    }
                    reader.readAsDataURL(file)
                },
                required = true
            )
        }

        if (isUploading) {
            Div({
                style {
                    textAlign("center")
                    padding(12.px)
                    color(Color("#4299e1"))
                    fontSize(14.px)
                }
            }) {
                Text("Загрузка фото...")
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
                
                // Validate required fields
                if (lesson == null && selectedFile == null) {
                    console.log("Фото отчета обязательно для новых занятий")
                    return@PrimaryButton
                }
                
                if (subjectId != null && amountInt != null) {
                    scope.launch {
                        try {
                            isUploading = true
                            
                            // Upload photo if a new file was selected
                            var photoUrl: String? = null
                            if (selectedFile != null) {
                                photoUrl = ApiClient.uploadImage(selectedFile!!)
                            } else if (lesson != null) {
                                photoUrl = lesson.photoReport
                            }
                            
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
                                console.log("Photo URL: $photoUrl")
                            }
                        } catch (e: Exception) {
                            console.log("Error uploading photo: ${e.message}")
                        } finally {
                            isUploading = false
                        }
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
    var amount by remember { mutableStateOf(payment?.amount?.toString() ?: "") }
    var selectedSubjectId by remember { mutableStateOf(payment?.subject?.id?.toString() ?: "") }
    var selectedFile by remember { mutableStateOf<org.w3c.files.File?>(null) }
    var previewUrl by remember { mutableStateOf<String?>(payment?.photoReport) }
    var isUploading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Modal(isOpen = true, onClose = onClose) {
        H3({ style { marginBottom(20.px) } }) {
            Text(if (payment != null) "Редактировать оплату" else "Новая оплата")
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

        FormGroup("Фото отчета") {
            FileInput(
                selectedFile = selectedFile,
                previewUrl = previewUrl,
                onFileSelected = { file ->
                    selectedFile = file
                    // Create preview URL
                    val reader = org.w3c.files.FileReader()
                    reader.onload = { event ->
                        previewUrl = event.target.asDynamic().result as String
                    }
                    reader.readAsDataURL(file)
                },
                required = true
            )
        }

        if (isUploading) {
            Div({
                style {
                    textAlign("center")
                    padding(12.px)
                    color(Color("#4299e1"))
                    fontSize(14.px)
                }
            }) {
                Text("Загрузка фото...")
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
                
                // Validate required fields
                if (payment == null && selectedFile == null) {
                    console.log("Фото отчета обязательно для новых оплат")
                    return@PrimaryButton
                }
                
                if (subjectId != null && amountInt != null) {
                    scope.launch {
                        try {
                            isUploading = true
                            
                            // Upload photo if a new file was selected
                            var photoUrl: String? = null
                            if (selectedFile != null) {
                                photoUrl = ApiClient.uploadImage(selectedFile!!)
                            } else if (payment != null) {
                                photoUrl = payment.photoReport
                            }
                            
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
                                console.log("Photo URL: $photoUrl")
                            }
                        } catch (e: Exception) {
                            console.log("Error uploading photo: ${e.message}")
                        } finally {
                            isUploading = false
                        }
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
