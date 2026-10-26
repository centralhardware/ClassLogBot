package me.centralhardware.znatoki.telegram.statistic.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

/** Deep links from bot messages: tab=report&month=2026-10 or slot=12. */
fun applyDeepLink(link: String = platform.deepLink) {
    if (link.isEmpty()) return
    val p = link.split("&").mapNotNull { it.split("=").takeIf { kv -> kv.size == 2 }?.let { kv -> kv[0] to kv[1] } }.toMap()
    p["tab"]?.let { App.tab = it }
    p["month"]?.let { if (it in App.b.months) App.month = it }
    p["slot"]?.toIntOrNull()?.let { slot ->
        App.tab = "schedule"
        App.scope.launch {
            runCatching { Api.get<ScheduleDto>("/api/schedule") }.getOrNull()?.slots?.find { it.id == slot }?.let { openAddLesson(LessonPrefill.fromSlot(it)) }
        }
    }
}

/** Signs in and opens the deep link; any failure becomes the blocking screen. */
suspend fun start() {
    try { signIn(); applyDeepLink() } catch (e: ApiError) { blocked = e } catch (e: Throwable) { blocked = ApiError(e.message ?: "Ошибка", 0) }
}

private val TABS = listOf(
    Triple("today", "Занятия", Icons.today),
    Triple("payments", "Оплаты", Icons.payments),
    Triple("schedule", "График", Icons.schedule),
    Triple("students", "Ученики", Icons.students),
    Triple("tutors", "Команда", Icons.tutors),
    Triple("report", "Отчёт", Icons.report),
    Triple("audit", "Журнал", Icons.audit),
)

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

/** The whole app: platforms call this from their entry point. */
@Composable
fun ClassLogApp() {
    LaunchedEffect(Unit) {
        tg?.onBack { Sheets.closeTop() }
        start()
    }
    ClassLogTheme {
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density * UiScale.value, d.fontScale)) {
            Box(Modifier.fillMaxSize().background(colors.ground), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 520.dp).fillMaxSize().background(colors.bg2).windowInsetsPadding(WindowInsets.safeDrawing)) {
                    Header()
                    Box(Modifier.weight(1f)) {
                        val boot = App.boot
                        when {
                            blocked != null -> NoAccess(blocked!!)
                            boot == null -> Spinner()
                            else -> AppBody()
                        }
                    }
                }
                Overlays()
            }
        }
    }
}

@Composable
private fun Header() {
    val boot = App.boot
    Row(Modifier.fillMaxWidth().background(colors.bg).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton({ openScale() }) { Ico(Icons.scale, 18.dp, colors.accent) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ClassLog", fontWeight = FontWeight.Bold)
            boot?.let { Text(it.me.name, color = colors.hint, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
        }
        if (tg == null && Login.data.isNotEmpty()) IconButton({ logout() }) { Ico(Icons.logout, 18.dp, colors.accent) }
        if (boot != null) IconButton({ openSearch() }) { Ico(Icons.search, 18.dp, colors.accent) }
    }
    HorizontalDivider(color = colors.line)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Overlays() {
    Sheets.stack.toList().forEach { content ->
        key(content) {
            val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val scope = rememberCoroutineScope()
            val close: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { Sheets.close(content) } }
            ModalBottomSheet({ Sheets.close(content) }, sheetState = state, containerColor = colors.bg2) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding()) {
                    content(close)
                }
            }
        }
    }
    Box(Modifier.fillMaxSize().padding(bottom = 140.dp), contentAlignment = Alignment.BottomCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Toasts.items.forEach { (_, msg) -> Toast { Text(msg, color = Color.White, fontSize = 13.sp) } }
            Toasts.undo?.let { (msg, left, onUndo) ->
                Toast {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(msg, color = Color.White, fontSize = 13.sp)
                        Text("Отменить · $left", Modifier.clickable { Toasts.undo = null; onUndo() }, color = Color(0xFF7CC4FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
    viewImage?.let { (photo, cap) ->
        Dialog({ viewImage = null }, DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .85f)).clickable { viewImage = null }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                val img = rememberPhoto(photo.first, photo.second)
                if (img != null) Image(img, cap, Modifier.fillMaxWidth(.92f).heightIn(max = 600.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Fit) else Spinner()
                Text("$cap · нажмите, чтобы закрыть", Modifier.padding(top = 10.dp), color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Toast(content: @Composable () -> Unit) =
    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xE615202B), shadowElevation = 4.dp) { Box(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) { content() } }

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

@Composable
private fun Centered(content: @Composable () -> Unit) =
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }

/**
 * The first screen outside Telegram (browser, Android app): sign-in through the bot. The button opens
 * `t.me/<bot>?start=login_<token>`; once the user presses Start there, polling gets the credential.
 */
@Composable
private fun LoginScreen() {
    var waiting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Centered {
        Title("Вход в ClassLog")
        Text(error ?: if (waiting) "Нажмите «Старт» в боте и вернитесь сюда." else "Войдите через свой Telegram-аккаунт — тот же, что в боте.", textAlign = TextAlign.Center, lineHeight = 21.sp)
        if (waiting) Spinner()
        else Btn("Войти через Telegram", "primary") {
            App.scope.launch {
                error = null
                val login = try { Api.postEmpty<BotLoginDto>("/auth/bot-login") } catch (e: ApiError) { error = e.message; return@launch }
                platform.openUrl(login.link)
                waiting = true
                // The token lives 10 minutes on the server.
                repeat(300) {
                    delay(2000)
                    val credential = runCatching { Api.get<BotLoginPollDto>("/auth/bot-login/${login.token}") }.getOrNull()?.credential
                    if (credential != null) {
                        Login.data = credential
                        blocked = null
                        start()
                        return@launch
                    }
                }
                waiting = false
                error = "Вход не подтверждён. Попробуйте ещё раз."
            }
        }
    }
}

@Composable
private fun NoAccess(e: ApiError) {
    if (e.code == "login") return LoginScreen()
    var sent by remember { mutableStateOf(false) }
    val noWeb = e.code == "no_web" || e.code == "unknown"
    Centered {
        Title(if (noWeb) "Нет доступа к веб-интерфейсу" else "Не удалось войти")
        Text(if (noWeb) "Запросите доступ у администратора." else e.message ?: "", textAlign = TextAlign.Center, lineHeight = 21.sp)
        if (noWeb && Api.credential.isNotEmpty()) {
            Btn(if (sent) "Запрос отправлен" else "Запросить доступ", "primary", enabled = !sent) {
                App.scope.launch {
                    runCatching { Api.post<OkDto, AuthRequest>("/auth/access-request", AuthRequest(Api.initData, platform.name, Login.data.takeIf { Api.initData.isEmpty() && it.isNotEmpty() })) }
                        .onSuccess { sent = true; Toasts.show("Администратор получит запрос в Telegram") }
                        .onFailure { Toasts.show(it.message ?: "Ошибка") }
                }
            }
        } else if (e.code == "auth" && tg != null) {
            Btn("Закрыть", "primary") { tg?.close() }
        } else if (e.code != "auth") {
            Btn("Повторить", "primary") { blocked = null; App.scope.launch { start() } }
        }
    }
}

@Composable
private fun AppBody() {
    if (!tabVisible(App.tab)) App.tab = "today"
    val visible = TABS.filter { tabVisible(it.first) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            key(App.tab) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).padding(top = 14.dp, bottom = 90.dp)) {
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
            }
            MainButton(Modifier.align(Alignment.BottomCenter))
        }
        BoxWithConstraints {
            // Below ~54dp a tab's label no longer fits, so the bar keeps only the icons.
            val labels = maxWidth / visible.size >= 54.dp
            NavigationBar(containerColor = colors.bg, tonalElevation = 0.dp, windowInsets = WindowInsets(0)) {
                visible.forEach { (t, title, icon) ->
                    NavigationBarItem(
                        selected = t == App.tab, onClick = { App.tab = t },
                        icon = { Ico(icon) },
                        label = if (labels) ({ Text(title, fontSize = if (visible.size > 5) 9.sp else 10.sp, maxLines = 1) }) else null,
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = colors.accent, selectedTextColor = colors.accent, unselectedIconColor = colors.hint, unselectedTextColor = colors.hint, indicatorColor = colors.accentBg),
                    )
                }
            }
        }
    }
}

@Composable
private fun MainButton(modifier: Modifier) {
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
    if (label != null) Button(action, modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp).heightIn(min = 50.dp), shape = RoundedCornerShape(14.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}
