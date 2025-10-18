package me.centralhardware.znatoki.telegram.statistic.web

import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import kotlinx.coroutines.launch
import me.centralhardware.znatoki.telegram.statistic.dto.*

@Composable
fun TodayPage(appState: AppState) {
    var activeTab by remember { mutableStateOf(0) }
    val hasAddTime = appState.userPermissions.contains("ADD_TIME") || appState.isAdmin
    val hasAddPayment = appState.userPermissions.contains("ADD_PAYMENT") || appState.isAdmin

    var lessons by remember { mutableStateOf(appState.todayLessons) }
    var payments by remember { mutableStateOf(appState.todayPayments) }
    var isLessonModalOpen by remember { mutableStateOf(false) }
    var isPaymentModalOpen by remember { mutableStateOf(false) }
    var selectedLesson by remember { mutableStateOf<LessonDto?>(null) }
    var selectedPayment by remember { mutableStateOf<PaymentDto?>(null) }

    val scope = rememberCoroutineScope()

    Card {
        H2({ style { fontSize(20.px); marginBottom(16.px); color(Color("#2d3748")); fontWeight(700) } }) {
            Text("📅 Сегодня")
        }

        if (hasAddTime && hasAddPayment) {
            TabButtons(
                tabs = listOf("Занятия", "Оплаты"),
                activeTab = activeTab,
                onTabChange = { activeTab = it }
            )
        }

        when (activeTab) {
            0 -> if (hasAddTime) {
                Div {
                    Div({
                        style {
                            marginBottom(16.px)
                            display(DisplayStyle.Flex)
                        }
                    }) {
                        PrimaryButton("Добавить занятие") {
                            selectedLesson = null
                            isLessonModalOpen = true
                        }
                    }

                    if (lessons.isEmpty()) {
                        EmptyState("📚", "Нет занятий на сегодня")
                    } else {
                        lessons.forEach { lesson ->
                            LessonCard(lesson) {
                                selectedLesson = lesson
                                isLessonModalOpen = true
                            }
                        }
                    }
                }
            }

            1 -> if (hasAddPayment) {
                Div {
                    Div({
                        style {
                            marginBottom(16.px)
                            display(DisplayStyle.Flex)
                        }
                    }) {
                        PrimaryButton("Добавить оплату") {
                            selectedPayment = null
                            isPaymentModalOpen = true
                        }
                    }

                    if (payments.isEmpty()) {
                        EmptyState("💰", "Нет оплат на сегодня")
                    } else {
                        payments.forEach { payment ->
                            PaymentCard(payment) {
                                selectedPayment = payment
                                isPaymentModalOpen = true
                            }
                        }
                    }
                }
            }
        }
    }

    if (isLessonModalOpen) {
        LessonModal(
            lesson = selectedLesson,
            subjects = appState.subjects,
            tutorId = appState.currentTutorId,
            onClose = { isLessonModalOpen = false },
            onCreate = { request ->
                scope.launch {
                    try {
                        ApiClient.createLesson(request)
                        lessons = ApiClient.getTodayLessons()
                        isLessonModalOpen = false
                    } catch (e: Exception) {
                        console.log("Error creating lesson: ${e.message}")
                    }
                }
            },
            onUpdate = { request ->
                scope.launch {
                    try {
                        selectedLesson?.let { lesson ->
                            ApiClient.updateLesson(lesson.id, request)
                            lessons = ApiClient.getTodayLessons()
                            isLessonModalOpen = false
                        }
                    } catch (e: Exception) {
                        console.log("Error updating lesson: ${e.message}")
                    }
                }
            },
            onDelete = selectedLesson?.let { lesson ->
                {
                    scope.launch {
                        try {
                            ApiClient.deleteLesson(lesson.id)
                            lessons = ApiClient.getTodayLessons()
                            isLessonModalOpen = false
                        } catch (e: Exception) {
                            console.log("Error deleting lesson: ${e.message}")
                        }
                    }
                }
            }
        )
    }

    if (isPaymentModalOpen) {
        PaymentModal(
            payment = selectedPayment,
            subjects = appState.subjects,
            tutorId = appState.currentTutorId,
            onClose = { isPaymentModalOpen = false },
            onCreate = { request ->
                scope.launch {
                    try {
                        ApiClient.createPayment(request)
                        payments = ApiClient.getTodayPayments()
                        isPaymentModalOpen = false
                    } catch (e: Exception) {
                        console.log("Error creating payment: ${e.message}")
                    }
                }
            },
            onUpdate = { request ->
                scope.launch {
                    try {
                        selectedPayment?.let { payment ->
                            ApiClient.updatePayment(payment.id, request)
                            payments = ApiClient.getTodayPayments()
                            isPaymentModalOpen = false
                        }
                    } catch (e: Exception) {
                        console.log("Error updating payment: ${e.message}")
                    }
                }
            },
            onDelete = selectedPayment?.let { payment ->
                {
                    scope.launch {
                        try {
                            ApiClient.deletePayment(payment.id)
                            payments = ApiClient.getTodayPayments()
                            isPaymentModalOpen = false
                        } catch (e: Exception) {
                            console.log("Error deleting payment: ${e.message}")
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ReportsPage(appState: AppState) {
    var selectedSubject by remember { mutableStateOf<Long?>(null) }
    var selectedPeriod by remember { mutableStateOf(getCurrentPeriod()) }
    var selectedTutor by remember { mutableStateOf<Long?>(appState.currentTutorId) }
    var report by remember { mutableStateOf<ReportDto?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Получаем список предметов для выбранного преподавателя
    val availableSubjects = remember(selectedTutor, appState.tutors, appState.subjects) {
        if (appState.isAdmin && selectedTutor != null) {
            appState.tutors.find { it.id == selectedTutor }?.subjects ?: emptyList()
        } else {
            appState.subjects
        }
    }

    // Сбрасываем выбранный предмет, если его нет в доступных для нового преподавателя
    LaunchedEffect(availableSubjects) {
        if (selectedSubject != null && availableSubjects.none { it.id == selectedSubject }) {
            selectedSubject = null
        }
    }

    LaunchedEffect(selectedSubject, selectedPeriod, selectedTutor) {
        if (selectedSubject != null) {
            isLoading = true
            try {
                report = ApiClient.getReport(selectedSubject!!, selectedPeriod, selectedTutor)
            } catch (e: Exception) {
                console.log("Error loading report: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Card {
        H2({ style { fontSize(20.px); marginBottom(16.px); color(Color("#2d3748")); fontWeight(700) } }) {
            Text("📊 Отчеты")
        }

        FormGroup("Предмет") {
            SelectInput(
                value = selectedSubject?.toString() ?: "",
                options = listOf("" to "Выберите предмет") + availableSubjects.map { it.id.toString() to it.name },
                onValueChange = { selectedSubject = it.toLongOrNull() }
            )
        }

        FormGroup("Период") {
            SelectInput(
                value = selectedPeriod,
                options = getPeriodOptions(),
                onValueChange = { selectedPeriod = it }
            )
        }

        if (appState.isAdmin) {
            FormGroup("Преподаватель") {
                SelectInput(
                    value = selectedTutor?.toString() ?: "",
                    options = listOf("" to "Выберите преподавателя") + appState.tutors.map { it.id.toString() to it.name },
                    onValueChange = { selectedTutor = it.toLongOrNull() }
                )
            }
        }

        if (isLoading) {
            P { Text("Загрузка...") }
        } else if (report != null) {
            ReportContent(report!!)
        }
    }
}

@Composable
fun StatisticsPage(appState: AppState) {
    var selectedPeriod by remember { mutableStateOf(getCurrentPeriod()) }
    var selectedSubject by remember { mutableStateOf<String?>(null) }
    var selectedTutor by remember { mutableStateOf<Long?>(appState.currentTutorId) }
    var statistics by remember { mutableStateOf<StatisticsDto?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Получаем список предметов для выбранного преподавателя
    val availableSubjects = remember(selectedTutor, appState.tutors, appState.subjects) {
        if (appState.isAdmin && selectedTutor != null) {
            appState.tutors.find { it.id == selectedTutor }?.subjects ?: emptyList()
        } else {
            appState.subjects
        }
    }

    // Сбрасываем выбранный предмет, если его нет в доступных для нового преподавателя
    LaunchedEffect(availableSubjects) {
        if (selectedSubject != null && availableSubjects.none { it.name == selectedSubject }) {
            selectedSubject = null
        }
    }

    LaunchedEffect(selectedPeriod, selectedSubject, selectedTutor) {
        isLoading = true
        try {
            statistics = ApiClient.getStatistics(selectedPeriod, selectedSubject, selectedTutor)
        } catch (e: Exception) {
            console.log("Error loading statistics: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Card {
        H2({ style { fontSize(20.px); marginBottom(16.px); color(Color("#2d3748")); fontWeight(700) } }) {
            Text("📈 Статистика")
        }

        FormGroup("Период") {
            SelectInput(
                value = selectedPeriod,
                options = getPeriodOptions(),
                onValueChange = { selectedPeriod = it }
            )
        }

        FormGroup("Предмет") {
            SelectInput(
                value = selectedSubject ?: "all",
                options = listOf("all" to "Все предметы") + availableSubjects.map { it.name to it.name },
                onValueChange = { selectedSubject = if (it == "all") null else it }
            )
        }

        if (appState.isAdmin) {
            FormGroup("Преподаватель") {
                SelectInput(
                    value = selectedTutor?.toString() ?: "",
                    options = listOf("" to "Выберите преподавателя") + appState.tutors.map { it.id.toString() to it.name },
                    onValueChange = { selectedTutor = it.toLongOrNull() }
                )
            }
        }

        if (isLoading) {
            P { Text("Загрузка...") }
        } else if (statistics != null) {
            StatisticsContent(statistics!!)
        }
    }
}

@Composable
fun StudentsPage(appState: AppState) {
    var searchQuery by remember { mutableStateOf("") }
    var students by remember { mutableStateOf<List<StudentDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isModalOpen by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<StudentDto?>(null) }

    val scope = rememberCoroutineScope()

    // Load students when search query changes or on initialization
    LaunchedEffect(searchQuery) {
        isLoading = true
        try {
            students = ApiClient.searchStudents(searchQuery)
        } catch (e: Exception) {
            console.log("Error loading students: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Card {
        H2({ style { fontSize(20.px); marginBottom(16.px); color(Color("#2d3748")); fontWeight(700) } }) {
            Text("👥 Ученики")
        }

        FormGroup("Поиск") {
            TextInput(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Введите имя ученика..."
            )
        }

        if (isLoading) {
            P { Text("Загрузка...") }
        } else if (students.isNotEmpty()) {
            students.forEach { student ->
                StudentCard(student) {
                    selectedStudent = student
                    isModalOpen = true
                }
            }
        } else if (searchQuery.isNotEmpty()) {
            P({ style { color(Color("#718096")) } }) {
                Text("Ничего не найдено")
            }
        }
    }

    if (isModalOpen && selectedStudent != null) {
        StudentModal(
            student = selectedStudent,
            onClose = { isModalOpen = false },
            onUpdate = { request ->
                scope.launch {
                    try {
                        ApiClient.updateStudent(selectedStudent!!.id.toLong(), request)
                        // Reload students list after update
                        students = if (searchQuery.isEmpty()) {
                            ApiClient.searchStudents("")
                        } else {
                            ApiClient.searchStudents(searchQuery)
                        }
                        isModalOpen = false
                    } catch (e: Exception) {
                        console.log("Error updating student: ${e.message}")
                    }
                }
            }
        )
    }
}

@Composable
fun TeachersPage(appState: AppState) {
    var teachers by remember { mutableStateOf<List<TutorDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            teachers = ApiClient.getTeachers()
        } catch (e: Exception) {
            console.log("Error loading teachers: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Card {
        H2({ style { fontSize(20.px); marginBottom(16.px); color(Color("#2d3748")); fontWeight(700) } }) {
            Text("👨‍🏫 Учителя")
        }

        if (isLoading) {
            P { Text("Загрузка...") }
        } else if (teachers.isNotEmpty()) {
            teachers.forEach { teacher ->
                TeacherCard(teacher)
            }
        } else {
            P({ style { color(Color("#718096")) } }) {
                Text("Нет учителей")
            }
        }
    }
}

@Composable
fun AuditLogPage(appState: AppState) {
    var logs by remember { mutableStateOf<List<AuditLogEntryDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var offset by remember { mutableStateOf(0) }
    val limit = 50

    val scope = rememberCoroutineScope()

    LaunchedEffect(offset) {
        isLoading = true
        try {
            val newLogs = ApiClient.getAuditLog(offset, limit)
            logs = if (offset == 0) newLogs else logs + newLogs
        } catch (e: Exception) {
            console.log("Error loading audit log: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Card {
        H2({ style { fontSize(20.px); marginBottom(16.px); color(Color("#2d3748")); fontWeight(700) } }) {
            Text("📋 Журнал действий")
        }

        if (logs.isNotEmpty()) {
            logs.forEach { log ->
                AuditLogCard(log)
            }

            if (!isLoading) {
                PrimaryButton("Загрузить еще") {
                    offset += limit
                }
            }
        } else if (isLoading) {
            P { Text("Загрузка...") }
        } else {
            P({ style { color(Color("#718096")) } }) {
                Text("Нет записей")
            }
        }
    }
}

// Helper functions
fun getCurrentPeriod(): String {
    val now = kotlinx.datetime.Clock.System.now()
    val instant = now.toString()
    val parts = instant.split("-")
    return "${parts[0]}-${parts[1]}"
}

fun getPeriodOptions(): List<Pair<String, String>> {
    val months = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    val now = kotlinx.datetime.Clock.System.now()
    val instant = now.toString()
    val parts = instant.split("-")
    val currentYear = parts[0].toInt()
    val currentMonth = parts[1].toInt()

    return (0 until 12).map { i ->
        val month = (currentMonth - i).let { if (it <= 0) it + 12 else it }
        val year = if (currentMonth - i <= 0) currentYear - 1 else currentYear
        val monthStr = if (month < 10) "0$month" else "$month"
        val value = "$year-$monthStr"
        val label = "${months[month - 1]} $year"
        value to label
    }
}
