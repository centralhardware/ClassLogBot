package me.centralhardware.znatoki.telegram.statistic.webapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Nav
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable

/** Why the app can't open: not signed in, failed check, or no WEB_INTERFACE. */
private var blocked by mutableStateOf<ApiError?>(null)

/**
 * Loads who the user is and what they may do; the Telegram credential itself is the sign-in.
 * Outside Telegram without a (still valid) sign-in, it asks for the login screen.
 */
suspend fun signIn() {
    if (Api.credential.isEmpty()) throw ApiError("Войдите через Telegram.", 401, "login")
    val boot: BootDto = try { Api.get("/api/boot") } catch (e: ApiError) {
        if (e.code != "auth" || Api.initData.isNotEmpty()) throw e
        Login.data = ""
        throw ApiError("Вход истёк, войдите ещё раз.", 401, "login")
    }
    App.boot = boot
    if (App.month.isEmpty()) App.month = boot.today.substring(0, 7)
    if (App.who == "me" && boot.caps.seesAll) App.who = "all"
}

/** Deep links from bot messages: #tab=report&month=2026-10 or #slot=12. */
private fun applyHash() {
    val h = window.location.hash.removePrefix("#")
    if (h.isEmpty()) return
    val p = h.split("&").mapNotNull { it.split("=").takeIf { kv -> kv.size == 2 }?.let { kv -> kv[0] to kv[1] } }.toMap()
    p["tab"]?.let { App.tab = it }
    p["month"]?.let { if (it in App.b.months) App.month = it }
    p["slot"]?.toIntOrNull()?.let { slot ->
        App.tab = "schedule"
        App.scope.launch {
            runCatching { Api.get<ScheduleDto>("/api/schedule") }.getOrNull()?.slots?.find { it.id == slot }?.let { openAddLesson(LessonPrefill.fromSlot(it)) }
        }
    }
}

fun main() {
    tg?.ready(); tg?.expand()
    tg?.BackButton?.onClick { Sheets.closeTop() }
    if (tg?.colorScheme == "dark") document.documentElement?.setAttribute("data-theme", "dark")
    else if (tg != null) document.documentElement?.setAttribute("data-theme", "light")
    App.scope.launch {
        try { signIn(); applyHash() } catch (e: ApiError) { blocked = e } catch (e: Throwable) { blocked = ApiError(e.message ?: "Ошибка", 0) }
    }
    // index.html shows a static header and spinner while the bundle loads; Compose takes over from here.
    document.getElementById("root")?.innerHTML = ""
    renderComposable(rootElementId = "root") { Root() }
}

private val TAB_ICONS = mapOf(
    "today" to """<rect x="3.5" y="5" width="17" height="15" rx="3"/><path d="M3.5 10h17M8 3v4M16 3v4"/>""",
    "payments" to """<rect x="3" y="6" width="18" height="13" rx="2.5"/><path d="M3 10.5h18M7 15h4"/>""",
    "schedule" to """<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>""",
    "students" to """<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c.8-3.6 3.4-5.5 6.5-5.5s5.7 1.9 6.5 5.5"/><circle cx="17.5" cy="9" r="2.6"/><path d="M16.5 14.6c2.6.2 4.4 1.9 5 4.9"/>""",
    "tutors" to """<circle cx="12" cy="8" r="3.5"/><path d="M5 20c.8-3.6 3.6-5.5 7-5.5s6.2 1.9 7 5.5"/><path d="M18.5 4.5l1.5 1.5-1.5 1.5M5.5 4.5L4 6l1.5 1.5"/>""",
    "report" to """<rect x="5" y="3.5" width="14" height="17" rx="2"/><path d="M9 8h6M9 12h6M9 16h3"/>""",
    "audit" to """<path d="M12 7v5l3 2"/><path d="M3.5 12a8.5 8.5 0 1 0 2.5-6"/><path d="M3 4v4h4"/>""",
)
private val TAB_TITLES = mapOf("today" to "Занятия", "schedule" to "График", "students" to "Ученики", "payments" to "Оплаты", "tutors" to "Команда", "report" to "Отчёт", "audit" to "Журнал")

fun tabVisible(t: String): Boolean {
    val c = App.caps
    return when (t) {
        "schedule" -> c.time
        "students" -> c.client || c.director
        "payments" -> c.readPayments
        "tutors" -> c.manageTutors
        "audit" -> c.admin
        else -> true
    }
}

@Composable
fun Svg(inner: String, size: Int = 22) = SvgRaw("""<svg viewBox="0 0 24 24" width="$size" height="$size" fill="none" stroke="currentColor" stroke-width="1.8">$inner</svg>""")

@Composable
fun Root() {
    Box("wrap") {
        Box("phone") {
            val boot = App.boot
            Box("tg-head") {
                Span({ classes("t") }) { Text("ClassLog"); boot?.let { Span({ classes("sub") }) { Text(it.me.name) } } }
                Button({ classes("iconbtn"); attr("aria-label", "Размер интерфейса"); attr("style", "display:inline-flex;padding:6px"); onClick { openScale() } }) {
                    Svg("""<path d="M3 19l5-13 5 13M4.8 14.5h6.4"/><path d="M14.5 19l3.5-9 3.5 9M15.7 16h4.6"/>""", 16)
                }
                if (tg?.initData.isNullOrEmpty() && Login.data.isNotEmpty()) Button({ classes("iconbtn"); attr("aria-label", "Выйти"); attr("style", "display:inline-flex;padding:6px"); onClick { logout() } }) {
                    Svg("""<path d="M14 4h4.5A1.5 1.5 0 0 1 20 5.5v13a1.5 1.5 0 0 1-1.5 1.5H14"/><path d="M10 8l-4 4 4 4M6 12h10"/>""", 16)
                }
                if (boot != null) Button({ classes("iconbtn"); attr("aria-label", "Поиск"); attr("style", "display:inline-flex;padding:6px"); onClick { openSearch() } }) {
                    Svg("""<circle cx="11" cy="11" r="6.5"/><path d="M16 16l4.5 4.5"/>""", 16)
                }
            }
            when {
                blocked != null -> NoAccess(blocked!!)
                boot == null -> Box("screen") { Spinner() }
                else -> AppBody()
            }
            Sheets.stack.toList().forEach { content ->
                Div({ classes("scrim"); onClick { e -> if (e.target == e.currentTarget) Sheets.stack.remove(content).also { Sheets.sync() } } }) {
                    Div({ classes("sheet"); attr("role", "dialog") }) {
                        Box("grab") {}
                        content { Sheets.stack.remove(content); Sheets.sync() }
                    }
                }
            }
            Toasts.items.forEach { (_, msg) -> Box("toast") { Text(msg) } }
            Toasts.undo?.let { (msg, left, onUndo) ->
                Box("toast", "display:flex;gap:14px;align-items:center") {
                    Text(msg)
                    Button({ attr("style", "background:none;border:0;color:#7cc4ff;font:700 13px var(--body);cursor:pointer"); onClick { Toasts.undo = null; onUndo() } }) { Text("Отменить · $left") }
                }
            }
            viewImage?.let { (src, cap) ->
                Div({ classes("scrim"); attr("style", "display:flex;align-items:center;justify-content:center;flex-direction:column;gap:10px;background:rgba(0,0,0,.85);z-index:60"); onClick { viewImage = null } }) {
                    Img(src, cap) { attr("style", "max-width:92%;max-height:80%;border-radius:10px") }
                    Div({ attr("style", "color:#fff;font-size:13px") }) { Text("$cap · нажмите, чтобы закрыть") }
                }
            }
        }
    }
}

private fun openScale() = Sheets.open { close ->
    Title("Размер интерфейса")
    Seg(UiScale.options, UiScale.value) { UiScale.set(it) }
    Lock("Сохраняется на этом устройстве.")
    Actions { Btn("Готово", "primary") { close() } }
}

/** Forgets this device's sign-in; only outside Telegram, where the login is the app's own. */
private fun logout() {
    Login.data = ""
    App.boot = null
    blocked = ApiError("Войдите через Telegram.", 401, "login")
}

/**
 * The first screen outside Telegram (browser, Android app): sign-in through the bot. The button opens
 * `t.me/<bot>?start=login_<token>`; once the user presses Start there, polling gets the credential.
 */
@Composable
private fun LoginScreen() {
    var waiting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Box("screen") {
        Box("empty", "padding-top:60px") {
            Title("Вход в ClassLog")
            P({ attr("style", "line-height:1.5") }) {
                Text(error ?: if (waiting) "Нажмите «Старт» в боте и вернитесь сюда." else "Войдите через свой Telegram-аккаунт — тот же, что в боте.")
            }
            if (waiting) Spinner()
            else Btn("Войти через Telegram", "primary", css = "border:0;border-radius:12px;padding:12px 18px;font:700 14px var(--body)") {
                App.scope.launch {
                    error = null
                    val start = try { Api.postEmpty<BotLoginDto>("/auth/bot-login") } catch (e: ApiError) { error = e.message; return@launch }
                    window.open(start.link, "_blank")
                    waiting = true
                    // The token lives 10 minutes on the server.
                    repeat(300) {
                        kotlinx.coroutines.delay(2000)
                        val credential = runCatching { Api.get<BotLoginPollDto>("/auth/bot-login/${start.token}") }.getOrNull()?.credential
                        if (credential != null) {
                            Login.data = credential
                            blocked = null
                            try { signIn(); applyHash() } catch (e: ApiError) { blocked = e } catch (e: Throwable) { blocked = ApiError(e.message ?: "Ошибка", 0) }
                            return@launch
                        }
                    }
                    waiting = false
                    error = "Вход не подтверждён. Попробуйте ещё раз."
                }
            }
        }
    }
}

@Composable
private fun NoAccess(e: ApiError) {
    if (e.code == "login") return LoginScreen()
    var sent by remember { mutableStateOf(false) }
    Box("screen") {
        Box("empty", "padding-top:60px") {
            Title(if (e.code == "no_web" || e.code == "unknown") "Нет доступа к веб-интерфейсу" else "Не удалось войти")
            P({ attr("style", "line-height:1.5") }) {
                Text(if (e.code == "no_web" || e.code == "unknown") "Запросите доступ у администратора." else e.message ?: "")
            }
            if ((e.code == "no_web" || e.code == "unknown") && Api.credential.isNotEmpty()) {
                Btn(if (sent) "Запрос отправлен" else "Запросить доступ", "primary", enabled = !sent, css = "border:0;border-radius:12px;padding:12px 18px;font:700 14px var(--body)") {
                    App.scope.launch {
                        runCatching { Api.post<OkDto, AuthRequest>("/auth/access-request", AuthRequest(Api.initData, tg?.platform ?: "web", Login.data.takeIf { Api.initData.isEmpty() && it.isNotEmpty() })) }
                            .onSuccess { sent = true; Toasts.show("Администратор получит запрос в Telegram") }
                            .onFailure { Toasts.show(it.message ?: "Ошибка") }
                    }
                }
            } else if (e.code == "auth" && tg != null) {
                Btn("Закрыть", "primary", css = "border:0;border-radius:12px;padding:12px 18px;font:700 14px var(--body)") { tg?.close() }
            }
        }
    }
}

@Composable
private fun AppBody() {
    if (!tabVisible(App.tab)) App.tab = "today"
    LaunchedEffect(App.tab) { window.scrollTo(0.0, 0.0) }
    Box("screen") {
        when (App.tab) {
            "today" -> if (App.caps.admin && TodayView.manage) ManageScreen() else TodayScreen()
            "schedule" -> ScheduleScreen()
            "students" -> StudentsScreen()
            "payments" -> PaymentsScreen()
            "report" -> ReportTab()
            "tutors" -> TutorsScreen()
            "audit" -> AuditScreen()
        }
    }
    MainButton()
    val visible = TAB_ICONS.keys.filter { tabVisible(it) }
    // Below ~54px a tab's label no longer fits even when dense, so the bar keeps only the icons.
    val perTab = Viewport.width / visible.size
    Nav({ classes("tabs"); if (visible.size > 5) classes("dense"); if (perTab < 54) classes("icons"); attr("role", "tablist") }) {
        visible.forEach { t ->
            Button({ attr("role", "tab"); attr("aria-selected", (t == App.tab).toString()); attr("title", TAB_TITLES[t]!!); onClick { App.tab = t } }) {
                Svg(TAB_ICONS[t]!!); Span { Text(TAB_TITLES[t]!!) }
            }
        }
    }
}

@Composable
private fun MainButton() {
    val c = App.caps
    val (label, action) = when (App.tab) {
        "today" -> ("Добавить занятие" to { openAddLesson(null) }).takeIf { c.time && !(c.admin && TodayView.manage) }
        "schedule" -> ("Добавить в расписание" to { openAddSlot() }).takeIf { c.time }
        "students" -> ("Добавить ученика" to { openStudentForm(null) }).takeIf { c.client }
        "payments" -> ("Добавить оплату" to { openAddPayment() }).takeIf { c.payment }
        "report" -> ("Выгрузить отчёт" to { openExport(null) }).takeIf { ReportView.view == "report" }
        "tutors" -> ("Добавить преподавателя" to { openTutor(null) }).takeIf { c.manageTutors }
        else -> null
    } ?: (null to {})
    if (label != null) Button({ classes("mainbtn"); onClick { action() } }) { Text(label) }
}
