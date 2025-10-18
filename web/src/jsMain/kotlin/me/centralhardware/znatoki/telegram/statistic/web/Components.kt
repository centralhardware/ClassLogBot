package me.centralhardware.znatoki.telegram.statistic.web

import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.attributes.*

@Composable
fun LoadingScreen(logs: List<LoadingLog>) {
    Div({ classes(AppStyles.loading) }) {
        Div({ classes(AppStyles.loadingHeader) }) {
            Div({ classes(AppStyles.loadingSpinner) }) {
                // Spinner rings
                repeat(3) {
                    Div({
                        style {
                            position(Position.Absolute)
                            width(100.percent)
                            height(100.percent)
                            border(3.px, LineStyle.Solid, Color.transparent)
                            borderRadius(50.percent)
                            property("animation", "spin 1.5s cubic-bezier(0.5, 0, 0.5, 1) infinite")
                            property("border-top-color", "rgba(255, 255, 255, ${0.9 - it * 0.3})")
                            property("animation-delay", "${-0.45 + it * 0.15}s")
                        }
                    })
                }
            }
            Div({ classes(AppStyles.loadingTitle) }) {
                Text("Загрузка приложения")
            }
        }

        Div({ classes(AppStyles.loadingLog) }) {
            logs.forEach { log ->
                Div({
                    classes(AppStyles.loadingLogItem)
                    if (log.isSuccess) classes(AppStyles.loadingLogItemSuccess)
                }) {
                    Text(log.message)
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(message: String) {
    Div({ classes(AppStyles.card) }) {
        Div({
            style {
                backgroundColor(Color("#fed7d7"))
                color(Color("#c53030"))
                padding(16.px)
                borderRadius(12.px)
                marginBottom(20.px)
            }
        }) {
            Text(message)
        }
    }
}

@Composable
fun MainNavigation(currentPage: Page, onPageChange: (Page) -> Unit) {
    var isDropdownOpen by remember { mutableStateOf(false) }

    Div({ classes(AppStyles.mainNav) }) {
        Div({ classes(AppStyles.pageSelector) }) {
            Button({
                classes(AppStyles.pageSelectorButton)
                onClick { isDropdownOpen = !isDropdownOpen }
            }) {
                Span { Text("${currentPage.icon} ${currentPage.label}") }
                Span({
                    style {
                        fontSize(12.px)
                        property("transition", "transform 0.3s ease")
                        if (isDropdownOpen) {
                            transform { rotate(180.deg) }
                        }
                    }
                }) {
                    Text("▼")
                }
            }

            if (isDropdownOpen) {
                Div({
                    classes(AppStyles.pageDropdown)
                    style {
                        opacity(1)
                        transform { translateY(0.px); scale(1) }
                    }
                }) {
                    Page.values().forEach { page ->
                        Div({
                            classes(AppStyles.pageOption)
                            onClick {
                                onPageChange(page)
                                isDropdownOpen = false
                            }
                        }) {
                            Text("${page.icon} ${page.label}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContentArea(page: Page, appState: AppState) {
    Div({
        style {
            property("animation", "pageFadeIn 0.4s ease-out")
        }
    }) {
        when (page) {
            Page.TODAY -> TodayPage(appState)
            Page.REPORTS -> ReportsPage(appState)
            Page.STATISTICS -> StatisticsPage(appState)
            Page.STUDENTS -> StudentsPage(appState)
            Page.TEACHERS -> TeachersPage(appState)
            Page.AUDIT_LOG -> AuditLogPage(appState)
        }
    }
}

@Composable
fun Card(content: @Composable () -> Unit) {
    Div({ classes(AppStyles.card) }) {
        content()
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button({
        classes(AppStyles.button)
        onClick { onClick() }
    }) {
        Text(text)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit) {
    Button({
        classes(AppStyles.button, AppStyles.secondaryButton)
        onClick { onClick() }
    }) {
        Text(text)
    }
}

@Composable
fun TabButtons(tabs: List<String>, activeTab: Int, onTabChange: (Int) -> Unit) {
    Div({ classes(AppStyles.tabButtons) }) {
        tabs.forEachIndexed { index, tab ->
            Button({
                classes(AppStyles.tabButton)
                if (index == activeTab) classes(AppStyles.tabButtonActive)
                onClick { onTabChange(index) }
            }) {
                Text(tab)
            }
        }
    }
}

@Composable
fun SummaryCard(label: String, value: String, onClick: (() -> Unit)? = null) {
    Div({
        classes(AppStyles.summaryItem)
        onClick?.let { onClick { it() } }
    }) {
        Div({
            style {
                fontSize(28.px)
                fontWeight(700)
                marginBottom(4.px)
                property("animation", "counterUp 0.6s ease-out")
            }
        }) {
            Text(value)
        }
        Div({
            style {
                fontSize(13.px)
                opacity(0.9)
            }
        }) {
            Text(label)
        }
    }
}

@Composable
fun Badge(text: String, type: BadgeType = BadgeType.DEFAULT) {
    Span({
        classes(AppStyles.badge)
        when (type) {
            BadgeType.GROUP -> classes(AppStyles.badgeGroup)
            BadgeType.EXTRA -> classes(AppStyles.badgeExtra)
            else -> {}
        }
    }) {
        Text(text)
    }
}

enum class BadgeType {
    DEFAULT, GROUP, EXTRA
}

@Composable
fun Modal(isOpen: Boolean, onClose: () -> Unit, content: @Composable () -> Unit) {
    if (isOpen) {
        Div({
            classes(AppStyles.modal, AppStyles.modalActive)
            onClick { onClose() }
        }) {
            Div({
                classes(AppStyles.modalContent)
                style {
                    transform { scale(1); translateY(0.px) }
                    opacity(1)
                    property("animation", "modalSlideIn 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)")
                }
                onClick { it.stopPropagation() }
            }) {
                content()
            }
        }
    }
}

@Composable
fun FormGroup(label: String, content: @Composable () -> Unit) {
    Div({
        style {
            marginBottom(16.px)
            property("animation", "formGroupSlideIn 0.4s ease-out backwards")
        }
    }) {
        Div({
            style {
                display(DisplayStyle.Block)
                marginBottom(8.px)
                fontSize(14.px)
                fontWeight(600)
                color(Color("#4a5568"))
            }
        }) {
            Text(label)
        }
        content()
    }
}

@Composable
fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    type: String = "text"
) {
    Input(InputType.Text) {
        attr("type", type)
        value(value)
        onInput { onValueChange(it.value) }
        attr("placeholder", placeholder)
        style {
            width(100.percent)
            padding(12.px, 14.px)
            fontSize(16.px)
            border(2.px, LineStyle.Solid, Color("#e2e8f0"))
            borderRadius(12.px)
            backgroundColor(Color.white)
            color(Color("#2d3748"))
            property("transition", "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)")
        }
    }
}

@Composable
fun SelectInput(
    value: String,
    options: List<Pair<String, String>>,
    onValueChange: (String) -> Unit,
    disabled: Boolean = false
) {
    Select({
        if (disabled) {
            attr("disabled", "disabled")
        }
        onChange { event ->
            onValueChange(event.value!!)
        }
        style {
            width(100.percent)
            padding(12.px, 14.px)
            fontSize(16.px)
            border(2.px, LineStyle.Solid, Color("#e2e8f0"))
            borderRadius(12.px)
            backgroundColor(Color.white)
            color(Color("#2d3748"))
            property("transition", "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)")
            property("appearance", "none")
            property("-webkit-appearance", "none")
        }
    }) {
        options.forEach { (optValue, optLabel) ->
            Option(optValue, {
                if (optValue == value) {
                    selected()
                }
            }) {
                Text(optLabel)
            }
        }
    }
}

@Composable
fun FileInput(
    selectedFile: org.w3c.files.File?,
    previewUrl: String?,
    onFileSelected: (org.w3c.files.File) -> Unit,
    required: Boolean = false
) {
    Div({
        style {
            marginBottom(16.px)
        }
    }) {
        // File input (hidden)
        Input(InputType.File) {
            attr("accept", "image/*")
            if (required) {
                attr("required", "required")
            }
            onChange { event ->
                val files = event.target.asDynamic().files
                if (files.length > 0) {
                    val file = files[0] as org.w3c.files.File
                    onFileSelected(file)
                }
            }
            style {
                display(DisplayStyle.None)
            }
            id("file-input")
        }

        // Custom file input button
        Label(forId = "file-input", attrs = {
            style {
                display(DisplayStyle.InlineBlock)
                padding(12.px, 20.px)
                fontSize(16.px)
                fontWeight(600)
                color(Color.white)
                backgroundColor(Color("#4299e1"))
                borderRadius(12.px)
                property("cursor", "pointer")
                property("transition", "all 0.3s ease")
                property("user-select", "none")
            }
        }) {
            Text(if (selectedFile != null) "Изменить фото" else "Выбрать фото *")
        }

        // Preview
        previewUrl?.let { url ->
            Div({
                style {
                    marginTop(12.px)
                    borderRadius(12.px)
                    property("overflow", "hidden")
                    border(2.px, LineStyle.Solid, Color("#e2e8f0"))
                }
            }) {
                Img(src = url) {
                    style {
                        width(100.percent)
                        property("max-height", "300px")
                        property("object-fit", "contain")
                        backgroundColor(Color("#f7fafc"))
                    }
                }
            }
        }

        // File name
        selectedFile?.let { file ->
            Div({
                style {
                    marginTop(8.px)
                    fontSize(14.px)
                    color(Color("#718096"))
                }
            }) {
                Text("Выбран файл: ${file.name}")
            }
        }
    }
}

@Composable
fun StudentSelector(
    selectedStudents: List<me.centralhardware.znatoki.telegram.statistic.dto.StudentDto>,
    onStudentsChange: (List<me.centralhardware.znatoki.telegram.statistic.dto.StudentDto>) -> Unit,
    multiple: Boolean = false,
    required: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember {
        mutableStateOf<List<me.centralhardware.znatoki.telegram.statistic.dto.StudentDto>>(
            emptyList()
        )
    }
    var isSearching by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }

    // Search students with debounce
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            isSearching = true
            showResults = true
            try {
                kotlinx.coroutines.delay(300) // Debounce
                searchResults = ApiClient.searchStudents(searchQuery)
            } catch (e: Exception) {
                console.log("Error searching students: ${e.message}")
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        } else {
            searchResults = emptyList()
            showResults = false
        }
    }

    Div({
        style {
            marginBottom(16.px)
        }
    }) {
        // Search input
        Div({
            style {
                position(Position.Relative)
            }
        }) {
            TextInput(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Поиск ученика${if (required) " *" else ""}..."
            )

            // Search results dropdown
            if (showResults && searchQuery.length >= 2) {
                Div({
                    style {
                        position(Position.Absolute)
                        top(100.percent)
                        left(0.px)
                        right(0.px)
                        backgroundColor(Color.white)
                        border(2.px, LineStyle.Solid, Color("#e2e8f0"))
                        borderRadius(12.px)
                        property("margin-top", "4px")
                        property("max-height", "200px")
                        property("overflow-y", "auto")
                        property("z-index", "1000")
                        property("box-shadow", "0 4px 6px rgba(0, 0, 0, 0.1)")
                    }
                }) {
                    if (isSearching) {
                        Div({
                            style {
                                padding(12.px)
                                textAlign("center")
                                color(Color("#718096"))
                            }
                        }) {
                            Text("Поиск...")
                        }
                    } else if (searchResults.isEmpty()) {
                        Div({
                            style {
                                padding(12.px)
                                textAlign("center")
                                color(Color("#718096"))
                            }
                        }) {
                            Text("Ничего не найдено")
                        }
                    } else {
                        searchResults.forEach { student ->
                            val isSelected = selectedStudents.any { it.id == student.id }

                            Div({
                                style {
                                    padding(12.px)
                                    property("cursor", "pointer")
                                    property("transition", "background-color 0.2s")
                                    if (isSelected) {
                                        backgroundColor(Color("#e6f7ff"))
                                    }
                                }
                                onClick {
                                    if (multiple) {
                                        if (isSelected) {
                                            onStudentsChange(selectedStudents.filter { it.id != student.id })
                                        } else {
                                            onStudentsChange(selectedStudents + student)
                                        }
                                    } else {
                                        onStudentsChange(listOf(student))
                                        searchQuery = ""
                                        showResults = false
                                    }
                                }
                            }) {
                                Div({
                                    style {
                                        fontWeight(600)
                                        color(Color("#2d3748"))
                                    }
                                }) {
                                    Text("${student.lastName} ${student.name} ${student.secondName}")
                                }
                                student.schoolClass?.let { schoolClass ->
                                    Div({
                                        style {
                                            fontSize(12.px)
                                            color(Color("#718096"))
                                            marginTop(4.px)
                                        }
                                    }) {
                                        Text("${schoolClass} класс")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected students
        if (selectedStudents.isNotEmpty()) {
            Div({
                style {
                    marginTop(12.px)
                    display(DisplayStyle.Flex)
                    property("flex-wrap", "wrap")
                    property("gap", "8px")
                }
            }) {
                selectedStudents.forEach { student ->
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            property("align-items", "center")
                            property("gap", "8px")
                            padding(8.px, 12.px)
                            backgroundColor(Color("#e6f7ff"))
                            borderRadius(8.px)
                            fontSize(14.px)
                        }
                    }) {
                        Span {
                            Text("${student.lastName} ${student.name}")
                        }
                        Button({
                            style {
                                backgroundColor(Color.transparent)
                                border(0.px)
                                color(Color("#4299e1"))
                                property("cursor", "pointer")
                                fontSize(16.px)
                                padding(0.px)
                                property("line-height", "1")
                            }
                            onClick {
                                onStudentsChange(selectedStudents.filter { it.id != student.id })
                            }
                        }) {
                            Text("×")
                        }
                    }
                }
            }
        }
    }
}
