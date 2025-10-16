package me.centralhardware.znatoki.telegram.statistic.web

import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import kotlinx.browser.document
import kotlinx.browser.window

fun main() {
    // Check if opened from Telegram WebApp
    if (!checkTelegramWebApp()) {
        renderComposable(rootElementId = "root") {
            AccessDeniedScreen()
        }
        return
    }

    window.asDynamic().Telegram.WebApp.expand()

    renderComposable(rootElementId = "root") {
        Style(AppStyles)
        App()
    }
}

@Composable
fun App() {
    var isLoading by remember { mutableStateOf(true) }
    var loadingLogs by remember { mutableStateOf(listOf<LoadingLog>()) }
    var currentPage by remember { mutableStateOf(Page.TODAY) }
    var appState by remember { mutableStateOf<AppState?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            addLog(loadingLogs, false, { loadingLogs = it }, "Инициализация приложения...")

            val state = loadInitialData { log ->
                addLog(loadingLogs, false, { loadingLogs = it }, log)
            }

            appState = state
            addLog(loadingLogs, true, { loadingLogs = it }, "Готово!")

            kotlinx.coroutines.delay(300)
            isLoading = false
        } catch (e: Exception) {
            error = e.message
            addLog(loadingLogs, false, { loadingLogs = it }, "❌ Ошибка: ${e.message}")
        }
    }

    Div({ classes(AppStyles.container) }) {
        if (isLoading) {
            LoadingScreen(loadingLogs)
        } else if (error != null) {
            ErrorScreen(error!!)
        } else if (appState != null) {
            MainNavigation(currentPage) { currentPage = it }
            ContentArea(currentPage, appState!!)
        }
    }
}

private fun addLog(
    logs: List<LoadingLog>,
    isSuccess: Boolean = false,
    update: (List<LoadingLog>) -> Unit,
    message: String
) {
    update(logs + LoadingLog(message, isSuccess))
}

data class LoadingLog(val message: String, val isSuccess: Boolean = false)

enum class Page(val label: String, val icon: String) {
    TODAY("Сегодня", "📅"),
    REPORTS("Отчеты", "📊"),
    STATISTICS("Статистика", "📈"),
    STUDENTS("Ученики", "👥"),
    TEACHERS("Учителя", "👨‍🏫"),
    AUDIT_LOG("Журнал действий", "📋")
}

fun checkTelegramWebApp(): Boolean {
    val tg = window.asDynamic().Telegram?.WebApp
    return tg != null && tg.initData != null && tg.initData.toString().isNotEmpty()
}

@Composable
fun AccessDeniedScreen() {
    Div({
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            alignItems(AlignItems.Center)
            justifyContent(JustifyContent.Center)
            height(100.vh)
            padding(20.px)
            property("text-align", "center")
            fontFamily("system-ui", "-apple-system", "BlinkMacSystemFont", "Segoe UI", "Roboto", "sans-serif")
            backgroundColor(Color("#f5f5f5"))
        }
    }) {
        Div({ style { fontSize(64.px); property("margin-bottom", "20px") } }) {
            Text("🚫")
        }
        H1({
            style {
                fontSize(28.px)
                property("margin-bottom", "16px")
                color(Color("#1a1a1a"))
                fontWeight(600)
            }
        }) {
            Text("Доступ запрещен")
        }
        P({
            style {
                fontSize(18.px)
                color(Color("#333"))
                property("max-width", "450px")
                property("line-height", "1.6")
            }
        }) {
            Text("Это приложение доступно только через Telegram WebApp.")
            Br()
            Br()
            Text("Пожалуйста, откройте приложение через Telegram бот.")
        }
    }
}
