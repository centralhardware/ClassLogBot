package me.centralhardware.znatoki.telegram.statistic.webapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.I
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import kotlin.math.roundToInt

object TodayView {
    var manage by mutableStateOf(false)
    var fTutor by mutableStateOf<Long?>(null)
    var fSubject by mutableStateOf<Long?>(null)
    var fStatus by mutableStateOf("live")
    var fQuery by mutableStateOf("")
    val selected = mutableStateListOf<String>()
}

@Composable
fun WhoBar() {
    if (!App.seesAll) return
    val opts = listOf("all" to "Все", "me" to (if (App.caps.time && !App.caps.admin) "Я" else App.tutorShort(App.me))) +
        App.teaching().filter { it.id != App.me }.map { it.id.toString() to App.tutorShort(it.id) }
    Seg(opts, App.who) { App.who = it }
}

@Composable
fun MonthBar() {
    if (!App.seesAll) return
    val ms = App.b.months
    val i = ms.indexOf(App.month)
    Box("monthbar") {
        Btn("‹", "iconbtn", enabled = i > 0) { App.month = ms[i - 1] }
        B { Text(monthName(App.month)) }
        Btn("›", "iconbtn", enabled = i in 0 until ms.size - 1) { App.month = ms[i + 1] }
    }
}

fun viewMonth() = if (App.seesAll) App.month else App.currentMonth

@Composable
fun LessonRow(l: LessonDto, selectable: Boolean = false) {
    Box("row" + if (l.deleted) " deleted" else "", onClick = { openLesson(l.id) }) {
        if (selectable) Input(InputType.Checkbox) {
            classes("lsel"); checked(l.id in TodayView.selected)
            onClick { e -> e.stopPropagation() }
            onChange { if (it.value) TodayView.selected.add(l.id) else TodayView.selected.remove(l.id) }
        }
        Box("num", "width:44px;color:var(--hint)") { Text(l.time) }
        Box("main") {
            Box("name") { Text(names(l)) }
            Box("meta") {
                if (App.seesAll && App.who != "me") Text(App.tutorShort(l.tutor) + " · ")
                Text(App.subj(l.subject).name + " · ")
                if (isGroup(l)) Chip("группа" + if (l.students.size > 1) " · ${l.students.size}" else "", "grp") else Chip("индивид.")
                if (l.half) { Text(" "); Chip("+½ ч", "half") }
                l.flags.forEach { Text(" "); Chip("⚠ $it", "del") }
                if (l.deleted) { Text(" "); Chip("удалено", "del") }
            }
        }
        if (App.money) Box("num right") { Text(rub(price(l) * l.students.size)) }
    }
}

@Composable
fun ViewSwitch() {
    if (App.caps.admin) Seg(listOf(false to "Лента", true to "Управление"), TodayView.manage) { TodayView.manage = it; TodayView.selected.clear() }
}

suspend fun loadLessons(month: String, who: String) = Api.get<List<LessonDto>>("/api/lessons?month=$month&tutor=$who")

// ---------- Занятия ----------
@Composable
fun TodayScreen() {
    val m = viewMonth()
    val isCur = m == App.currentMonth
    Title(if (isCur) "${WDF[dow(App.today)]}, ${dayMonth(App.today)}" else "Занятия")
    ViewSwitch(); WhoBar(); MonthBar()
    val who = if (App.seesAll) App.who else "me"
    Show(remote(m, who) { loadLessons(m, who) }) { all ->
        val todays = if (isCur) all.filter { it.date == App.today }.sortedBy { it.time } else emptyList()
        val live = all.filterNot { it.deleted }
        val cards = listOf(isCur, true, App.money).count { it }
        Box("stats", if (cards == 3) null else "grid-template-columns:repeat($cards,1fr)") {
            if (isCur) StatCard(todays.count { !it.deleted }.toString(), "занятий сегодня")
            StatCard(live.size.toString(), "за месяц")
            if (App.money) StatCard(k1(live.sumOf { price(it) * it.students.size }), "₽ начислено", true)
        }
        val flagged = all.filter { it.flags.isNotEmpty() }
        if (flagged.isNotEmpty()) Box("card", "border-left:3px solid var(--bad);margin-top:10px") {
            B { Text("Требуют проверки: ${flagged.size}") }
            Box("meta", "color:var(--hint);font-size:12px;margin-top:4px") {
                flagged.forEach { l -> Div { Text("${dayMonth(l.date)} ${l.time} · ${App.tutorShort(l.tutor)} · ${l.flags.joinToString(", ")}") } }
            }
        }
        if (isCur && App.caps.time && who == "me") {
            val sch = remote("sched") { Api.get<ScheduleDto>("/api/schedule") }
            if (sch is Remote.Ok) {
                val todo = slotsOn(App.today, sch.data.slots).count { markOf(sch.data, it.id, App.today) == null }
                if (todo > 0) Box("card", "margin-top:10px;display:flex;justify-content:space-between;align-items:center;cursor:pointer", onClick = { App.tab = "schedule" }) {
                    Span { Text("По расписанию не отмечено: "); B { Text(todo.toString()) } }; Chip("График ›", "grp")
                }
            }
        }
        if (isCur) {
            Sec("Сегодня")
            Box("group") { if (todays.isEmpty()) Empty("Сегодня занятий пока нет.") else todays.forEach { LessonRow(it) } }
        }
        all.filter { !(isCur && it.date == App.today) }.groupBy { it.date }.entries.sortedByDescending { it.key }.forEach { (d, ls) ->
            Sec(dayMonth(d)); Box("group") { ls.forEach { LessonRow(it) } }
        }
        if (!isCur) {
            if (all.isEmpty()) Empty("Занятий за ${monthName(m).lowercase()} нет.")
            Lock("Закрытый месяц." + if (App.caps.admin) " Как админ вы можете править эти занятия; правка попадёт в журнал изменений." else "")
        }
    }
}

// ---------- Управление (админ) ----------
@Composable
fun ManageScreen() {
    val m = App.month
    Title("Управление занятиями"); ViewSwitch(); MonthBar()
    Box("filt") {
        Dropdown(listOf<Pair<Long?, String>>(null to "Все репетиторы") + App.teaching().map { it.id to it.name }, TodayView.fTutor, cls = "") { TodayView.fTutor = it }
        Dropdown(listOf<Pair<Long?, String>>(null to "Все предметы") + App.b.subjects.map { it.id to it.name }, TodayView.fSubject, cls = "") { TodayView.fSubject = it }
        Dropdown(listOf("live" to "Действующие", "flag" to "Требуют проверки", "deleted" to "Удалённые", "all" to "Все, включая удалённые"), TodayView.fStatus, css = "grid-column:1/-1", cls = "") { TodayView.fStatus = it }
        TextField(TodayView.fQuery, "Ученик", InputType.Search, cls = "") { TodayView.fQuery = it }
    }
    Show(remote(m, "all") { loadLessons(m, "all") }) { all ->
        val q = TodayView.fQuery.trim().lowercase()
        val list = all.filter { l ->
            (TodayView.fTutor == null || l.tutor == TodayView.fTutor) && (TodayView.fSubject == null || l.subject == TodayView.fSubject) &&
                when (TodayView.fStatus) { "live" -> !l.deleted; "deleted" -> l.deleted; "flag" -> l.flags.isNotEmpty(); else -> true } &&
                (q.isEmpty() || l.students.any { it.fio.lowercase().contains(q) })
        }
        TodayView.selected.retainAll(list.map { it.id }.toSet())
        val live = list.filterNot { it.deleted }
        Box("stats") {
            StatCard(list.size.toString(), "найдено")
            StatCard(live.sumOf { it.students.size }.toString(), "ученико-занятий")
            StatCard(k1(live.sumOf { price(it) * it.students.size }), "₽ начислено", true)
        }
        if (list.isNotEmpty()) Box("sec", "display:flex;justify-content:space-between;align-items:center") {
            Span { Text("Занятия") }
            Btn(if (TodayView.selected.size == list.size) "Снять выбор" else "Выбрать все", "iconbtn", css = "text-transform:none;letter-spacing:0") {
                if (TodayView.selected.size == list.size) TodayView.selected.clear() else { TodayView.selected.clear(); TodayView.selected.addAll(list.map { it.id }) }
            }
        }
        if (list.isEmpty()) Empty("Под фильтр ничего не подходит.")
        list.groupBy { it.date }.entries.sortedByDescending { it.key }.forEach { (d, ls) -> Sec(dayMonth(d)); Box("group") { ls.forEach { LessonRow(it, true) } } }
        if (m != App.currentMonth) Lock("Закрытый месяц: правки попадут в журнал и в уже сданный отчёт.")
        val chosen = list.filter { it.id in TodayView.selected }
        if (chosen.isNotEmpty()) Box("bulk") {
            B { Text("Выбрано: ${chosen.size}") }
            Box("acts") {
                if (chosen.any { !it.deleted }) Btn("Удалить", "mini del") { bulk(chosen, "delete", null) }
                if (chosen.any { it.deleted }) Btn("Восстановить", "mini") { bulk(chosen, "restore", null) }
                Btn("Репетитор", "mini") { openBulk(chosen, "tutor") }
                Btn("Цена", "mini") { openBulk(chosen, "price") }
                Btn("Перенести", "mini") { openBulk(chosen, "day") }
                Btn("✕", "mini") { TodayView.selected.clear() }
            }
        }
    }
}

fun bulk(ls: List<LessonDto>, action: String, value: String?) = App.act {
    val r: Map<String, Int> = Api.post("/api/lessons/bulk", BulkInput(ls.map { it.id }, action, value))
    TodayView.selected.clear()
    val n = r["changed"] ?: 0
    if (action == "delete" || action == "restore") Toasts.withUndo((if (action == "delete") "Удалено: " else "Восстановлено: ") + n) {
        App.act { Api.post<Map<String, Int>, BulkInput>("/api/lessons/bulk", BulkInput(ls.map { it.id }, if (action == "delete") "restore" else "delete")) }
    } else Toasts.show("Изменено занятий: $n")
}

// ---------- График ----------
object ScheduleView { var cal by mutableStateOf(true); var day by mutableStateOf<String?>(null) }

fun slotsOn(iso: String, slots: List<SlotDto>): List<SlotDto> { val wd = dow(iso); return slots.filter { if (it.onceDate != null) it.onceDate == iso else it.weekday == wd } }
fun markOf(s: ScheduleDto, slot: Int, iso: String) = s.marks.find { it.slot == slot && it.date == iso }

@Composable
fun SlotTodayRow(s: ScheduleDto, sl: SlotDto) {
    val mk = markOf(s, sl.id, App.today)
    Box("row" + if (mk?.status == "skip") " skipped" else "", onClick = mk?.lesson?.let { id -> { openLesson(id) } }) {
        Box("num", "width:44px;color:var(--hint)") { Text(sl.time) }
        Box("main") {
            Box("name") { Text(sl.students.joinToString(", ") { it.fio }) }
            Box("meta") { Text(App.subj(sl.subject).name + (if (sl.students.size > 1) " · группа" else "") + (if (sl.onceDate != null) " · разово" else "") + (if (mk?.status == "skip") " · отменено" else "")) }
        }
        when (mk?.status) {
            "done" -> Chip("проведено", "ok")
            "skip" -> Btn("Вернуть", "mini ghost") { App.act { Api.post<OkDto, DateInput>("/api/schedule/slots/${sl.id}/unmark", DateInput(App.today)) } }
            else -> Box("slotacts") {
                Btn("Не было", "mini ghost") { App.act("Отмечено: занятия не было") { Api.post<OkDto, DateInput>("/api/schedule/slots/${sl.id}/skip", DateInput(App.today)) } }
                Btn("Внести", "mini") { openAddLesson(LessonPrefill.fromSlot(sl)) }
            }
        }
    }
}

@Composable
fun ScheduleToday(s: ScheduleDto) {
    val list = slotsOn(App.today, s.slots).sortedBy { it.time }
    val todo = list.count { markOf(s, it.id, App.today) == null }
    Sec("По расписанию · ${WDF[dow(App.today)].lowercase()}")
    Box("group") { if (list.isEmpty()) Empty("На сегодня в расписании ничего нет.") else list.forEach { SlotTodayRow(s, it) } }
    Lock((if (todo > 0) "Не отмечено: $todo. " + (if (s.reminders.evening) "В ${s.reminders.at} бот напомнит, если так и останется." else "Напоминания выключены.") else "Все занятия по расписанию отмечены.") +
        " «Внести» открывает обычную форму занятия, уже заполненную из слота.")
}

@Composable
fun SlotLine(x: SlotDto, showDate: Boolean) = Box("row") {
    Box("num", "width:44px;color:var(--hint)") { Text(x.time) }
    Box("main") {
        Box("name") { Text(x.students.joinToString(", ") { it.fio }) }
        Box("meta") { Text((if (showDate && x.onceDate != null) dayMonth(x.onceDate) + " · " else "") + App.subj(x.subject).name + (if (App.money) " · " + rub(x.amount) else "")) }
    }
    Btn("Удалить", "mini ghost") {
        App.act {
            Api.postEmpty<OkDto>("/api/schedule/slots/${x.id}", io.ktor.http.HttpMethod.Delete)
            Toasts.withUndo("Слот удалён") { App.act { Api.postEmpty<OkDto>("/api/schedule/slots/${x.id}/restore") } }
        }
    }
}

@Composable
fun ScheduleScreen() {
    Box("", "display:flex;justify-content:space-between;align-items:center") {
        Title("Расписание")
    }
    Box("", "display:flex;gap:8px;align-items:center;margin-bottom:10px") {
        Seg(listOf(true to "Календарь", false to "Список"), ScheduleView.cal, "flex:1;margin:0") { ScheduleView.cal = it }
        Show(remote("sched") { Api.get<ScheduleDto>("/api/schedule") }) { s -> Btn("Напоминания", "iconbtn", css = "font-size:12px") { openReminders(s.reminders) } }
    }
    val m = App.currentMonth
    val sch = remote("sched") { Api.get<ScheduleDto>("/api/schedule") }
    val mine = remote(m, "me") { loadLessons(m, "me") }
    Show(sch) { s ->
        Show(mine) { ls ->
            if (ScheduleView.cal) Calendar(s, ls) else ScheduleList(s)
        }
    }
}

@Composable
private fun Calendar(s: ScheduleDto, mine: List<LessonDto>) {
    val m = App.currentMonth
    val sel = ScheduleView.day ?: App.today
    fun iso(d: Int) = m + "-" + d.toString().padStart(2, '0')
    Box("monthbar") { Span {}; B { Text(monthName(m)) }; Span {} }
    Box("calgrid") {
        WD.forEach { Box("calh") { Text(it) } }
        repeat(dow("$m-01")) { Div {} }
        for (d in 1..daysInMonth(m)) {
            val x = iso(d); val past = x < App.today
            val held = mine.count { !it.deleted && it.date == x }
            val n = when { past -> held; x == App.today -> maxOf(held, slotsOn(x, s.slots).size); else -> slotsOn(x, s.slots).size }
            Button({
                classes("cal"); if (x == sel) classes("sel"); if (x == App.today) classes("today"); if (past) classes("past")
                onClick { ScheduleView.day = x }
            }) { Span { Text(d.toString()) }; I { Text("•".repeat(minOf(n, 4))) } }
        }
    }
    when {
        sel == App.today -> ScheduleToday(s)
        sel < App.today -> {
            val items = mine.filter { !it.deleted && it.date == sel }.sortedBy { it.time }
            Sec("${dayMonth(sel)} · проведено"); Box("group") { if (items.isEmpty()) Empty("Занятий не было.") else items.forEach { LessonRow(it) } }
        }
        else -> {
            val items = slotsOn(sel, s.slots).sortedBy { it.time }
            Sec("${dayMonth(sel)}, ${WDF[dow(sel)].lowercase()} · запланировано")
            Box("group") {
                if (items.isEmpty()) Empty("Ничего не запланировано.")
                items.forEach { x -> if (x.onceDate != null) SlotLine(x, false) else Box("row") {
                    Box("num", "width:44px;color:var(--hint)") { Text(x.time) }
                    Box("main") { Box("name") { Text(x.students.joinToString(", ") { it.fio }) }; Box("meta") { Text(App.subj(x.subject).name + " · каждую неделю") } }
                } }
            }
        }
    }
}

@Composable
private fun ScheduleList(s: ScheduleDto) {
    ScheduleToday(s)
    val once = s.slots.filter { it.onceDate != null && it.onceDate > App.today }.sortedBy { it.onceDate + it.time }
    if (once.isNotEmpty()) { Box("sec", "margin-top:22px") { Text("Разовые") }; Box("group") { once.forEach { SlotLine(it, true) } } }
    Box("sec", "margin-top:22px") { Text("Каждую неделю") }
    val weekly = s.slots.filter { it.onceDate == null }
    if (weekly.isEmpty()) Empty("Регулярных занятий нет. Добавьте их кнопкой внизу.")
    WD.indices.forEach { i ->
        val l = weekly.filter { it.weekday == i }.sortedBy { it.time }
        if (l.isNotEmpty()) { Sec(WDF[i]); Box("group") { l.forEach { SlotLine(it, false) } } }
    }
    Lock("Слоты повторяются каждую неделю. В свой день они появляются вверху с кнопкой «Внести».")
}

// ---------- Ученики ----------
object StudentsView { var query by mutableStateOf(""); var filter by mutableStateOf("active") }
const val LONG = 14

fun lastAgo(s: StudentRowDto) = s.lastDate?.let { daysBetween(it, App.today) }
fun isLong(s: StudentRowDto) = s.status == "active" && (lastAgo(s) ?: Int.MAX_VALUE) >= LONG

fun fuzzyScore(word: String, target: String): Int {
    val w = word.lowercase(); val tg = target.lowercase()
    if (tg.startsWith(w)) return 0
    val t = tg.take(maxOf(w.length, 3))
    val dp = Array(w.length + 1) { IntArray(t.length + 1) }
    for (i in 0..w.length) dp[i][0] = i
    for (j in 0..t.length) dp[0][j] = j
    for (i in 1..w.length) for (j in 1..t.length) dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + if (w[i - 1] == t[j - 1]) 0 else 1)
    return dp[w.length][t.length]
}

suspend fun loadStudents() = Api.get<List<StudentRowDto>>("/api/students")

@Composable
fun StudentRow(s: StudentRowDto) = Box("row" + if (s.deleted) " deleted" else "", onClick = { openStudent(s.id) }) {
    Avatar(s.id, sFio(s))
    Box("main") {
        Box("name") { Text(sFio(s)); if (s.status != "active") { Text(" "); Chip(STATUS[s.status]!!.lowercase(), if (s.status == "left") "del" else "half") } }
        Box("meta") {
            Text((s.cls?.let { "$it класс · " } ?: "") + (s.subjects.joinToString(", ") { App.subj(it).name }.ifEmpty { "занятий ещё не было" }))
            if (isLong(s)) { Text(" · "); T(s.lastDate?.let { "последнее ${dayMonth(it)}, ${lastAgo(s)} дн. назад" } ?: "занятий не было", css = "color:var(--warn)") }
            if ((s.balance ?: 0) < 0) { Text(" · "); T("долг ${rub(-s.balance!!)}", css = "color:var(--bad)") }
        }
    }
    if (s.deleted) Chip("удалён", "del") else Box("right") { Span({ classes("num") }) { Text(s.monthCount.toString()) }; Box("meta") { Text("в " + inMonth(App.currentMonth)) } }
}

@Composable
fun StudentsScreen() {
    Title("Ученики")
    TextField(StudentsView.query, "Поиск по ФИО, опечатки не страшны", InputType.Search, cls = "search") { StudentsView.query = it }
    Lock("Удалённые ученики здесь не показываются.")
    Show(remote("students") { loadStudents() }) { all ->
        val live = all.filterNot { it.deleted }
        val longN = live.count { isLong(it) }
        val f = StudentsView.filter
        Seg(listOf("active" to "Занимаются", "long" to "Давно", "paused" to "Пауза", "left" to "Ушли", "all" to "Все").map { (v, t) ->
            v to "$t " + when (v) { "all" -> live.size; "long" -> longN; else -> live.count { it.status == v } }
        }, f) { StudentsView.filter = it }
        if (f != "long" && longN > 0) Box("card", "margin-top:10px;display:flex;justify-content:space-between;align-items:center;cursor:pointer;border-left:3px solid var(--warn)", onClick = { StudentsView.filter = "long" }) {
            Span { Text("Давно не было занятий: "); B { Text(longN.toString()) } }; Chip("$LONG+ дней", "half")
        }
        var list = live.filter { f == "all" || (if (f == "long") isLong(it) else it.status == f) }
        if (f == "long") list = list.sortedByDescending { lastAgo(it) ?: Int.MAX_VALUE }
        val q = StudentsView.query.trim()
        if (q.isNotEmpty()) {
            val words = q.split(Regex("\\s+"))
            list = list.map { s -> s to words.minOf { w -> minOf(fuzzyScore(w, s.last), fuzzyScore(w, s.first)) } }.filter { it.second <= 2 }.sortedBy { it.second }.map { it.first }
        }
        Sec(if (q.isNotEmpty()) "Найдено: ${list.size}" else "Учеников: ${list.size}")
        Box("group") { if (list.isEmpty()) Empty(if (q.isNotEmpty()) "По запросу «$q» никого нет. Проверьте написание фамилии." else "Никого нет.") else list.forEach { StudentRow(it) } }
        val deleted = all.filter { it.deleted }
        if (App.caps.admin && deleted.isNotEmpty()) { Sec("Удалённые"); Box("group") { deleted.forEach { StudentRow(it) } } }
    }
}

// ---------- Оплаты ----------
@Composable
fun PaymentsScreen() {
    val m = viewMonth()
    Title("Оплаты"); WhoBar(); MonthBar()
    val who = if (App.seesAll) App.who else "me"
    Show(remote(m, who, "pay") { Api.get<List<PaymentDto>>("/api/payments?month=$m&tutor=$who") }) { list ->
        Box("group") {
            if (list.isEmpty()) Empty("В этом месяце оплат нет.")
            list.forEach { p ->
                Box("row" + if (p.deleted) " deleted" else "", onClick = { openPayment(p.id) }) {
                    Avatar(p.student.id, p.student.fio)
                    Box("main") {
                        Box("name") { Text(p.student.fio) }
                        Box("meta") {
                            Text("${dayMonth(p.date)} · ${App.subj(p.subject).name}" + (if (App.seesAll && App.who != "me") " · " + App.tutorShort(p.tutor) else "") + " · ")
                            when { p.deleted -> Chip("удалена", "del"); p.hasPhoto -> Chip("чек", "ok"); else -> Chip("без чека", "half") }
                        }
                    }
                    if (App.money) Box("num right") { Text(rub(p.amount)) }
                }
            }
        }
        if (App.money) {
            Sec("Всего получено")
            Box("group") { Box("row") { Box("main") { Box("name") { Text(monthName(m)) } }; Box("num right") { Text(rub(list.filterNot { it.deleted }.sumOf { it.amount })) } } }
        }
        if (m != App.currentMonth) Lock(if (App.caps.admin) "Месяц закрыт. Как админ вы можете править оплаты." else "Месяц закрыт: оплаты можно смотреть, но не изменять.")
    }
}

// ---------- Отчёт и итоги ----------
object ReportView { var view by mutableStateOf("report"); var period by mutableStateOf(6) }

@Composable
fun ReportTab() {
    if (ReportView.view == "stats") StatsScreen() else if (App.caps.director) InboxScreen() else ReportScreen()
}

@Composable
fun ReportSwitch() = Seg(listOf("report" to (if (App.caps.director) "Отчёты" else "Отчёт"), "stats" to "Итоги"), ReportView.view) { ReportView.view = it }

fun reportTutor() = if (App.caps.admin && App.who != "all" && App.who != "me") App.who.toLong() else App.me

@Composable
fun ReportScreen() {
    val rt = reportTutor()
    val m = viewMonth()
    Title("Отчёт за месяц"); ReportSwitch()
    if (App.caps.admin) Seg(App.teaching().map { (if (it.id == App.me) "me" else it.id.toString()) to App.tutorShort(it.id) }, if (rt == App.me) "me" else rt.toString()) { App.who = it }
    MonthBar()
    Show(remote(rt, m, "rep") { Api.get<ReportDto>("/api/report?tutor=$rt&month=$m") }) { rep ->
        if (rt == App.me && App.caps.time) StatusCard(rep)
        val pay = App.caps.readPayments
        rep.subjects.forEach { sb ->
            Box("sec", "display:flex;justify-content:space-between;align-items:center") {
                Text(App.subj(sb.subject).name)
                Span({ style("display:flex;gap:4px;flex-shrink:0") }) {
                    val css = "text-transform:none;letter-spacing:0;white-space:nowrap;padding:5px 7px"
                    Btn("Просмотр", "iconbtn", css = css) { openPreview(rt, m, listOf(sb.subject)) }
                    Btn("Скачать", "iconbtn", css = css) { exportNow(ExportInput("month", m, listOf(sb.subject), if (rt == App.me) "me" else rt.toString(), "file")) }
                    Btn("В чат", "iconbtn", css = css) { exportNow(ExportInput("month", m, listOf(sb.subject), if (rt == App.me) "me" else rt.toString(), "chat")) }
                }
            }
            Box("group") {
                sb.students.forEach { r ->
                    Box("row", "align-items:flex-start") {
                        Box("main") {
                            Box("name") { Text(r.fio); if (r.deleted) { Text(" "); Chip("удалён", "del") } }
                            Box("meta") { Text("индивид.: ${r.ind} · групп.: ${r.grp}") }
                            if (App.money && pay) Box("bar") { I({ style("width:${if (r.sum > 0) minOf(100, (r.paid / r.sum * 100).roundToInt()) else 0}%") }) {} }
                        }
                        Box("right") {
                            if (App.money) { Box("num") { Text(rub(r.sum)) }; if (pay) Box("meta num") { Text("оплачено ${rub(r.paid)}") } }
                            else { Box("num") { Text((r.ind + r.grp).toString()) }; Box("meta") { Text("занятий") } }
                        }
                    }
                }
                Box("row") {
                    Box("main") { Box("name") { Text("Итого") }; if (App.money) Box("meta") { Text(if (pay) "начислено / получено" else "начислено") } }
                    Box("right") {
                        if (App.money) { Box("num") { Text(rub(sb.billed)) }; if (pay) Box("meta num") { Text(rub(sb.received)) } }
                        else { Box("num") { Text(sb.lessons.toString()) }; Box("meta") { Text("занятий") } }
                    }
                }
            }
        }
        if (rep.subjects.isEmpty()) Empty("Занятий за ${monthName(m).lowercase()} нет.")
        rep.payroll?.let { pr ->
            Sec("Зарплата репетиторов · ${monthName(m).lowercase()}")
            Box("group") {
                if (pr.isEmpty()) Empty("Начислений нет.")
                pr.forEach { x ->
                    Box("row") {
                        Box("main") { Box("name") { Text(App.tutorName(x.tutor)) }; Box("meta") { Text(x.rate?.let { "$it% от начисленного ${rub(x.billed)}" } ?: "ставка не задана · начислено ${rub(x.billed)}") } }
                        Box("num") { Text(x.pay?.let { rub(it) } ?: "—") }
                    }
                }
                Box("row") { Box("main") { Box("name") { Text("Итого к выплате") } }; Box("num") { Text(rub(pr.sumOf { it.pay ?: 0 })) } }
            }
            Actions { Btn("Ведомость .xlsx") { exportNow(ExportInput("payroll", m)) } }
            Lock("Ставка своя у каждого преподавателя, задаётся в «Команде». Считается от начисленного.")
        }
        Lock(if (App.money) "Те же цифры, что в .xlsx по команде /report. Полтора часа считаются ×1,5. Другие месяцы и несколько предметов разом — через «Выгрузить отчёт»."
            else "Отчёт за текущий месяц: занятия и посещаемость. Суммы видят администратор и директор.")
    }
}

@Composable
fun StatusChip(s: SubmissionDto?) = when (s?.status) {
    null -> Chip("черновик"); "submitted" -> Chip("на проверке", "wait"); "accepted" -> Chip("принят", "ok"); else -> Chip("возвращён", "del")
}

@Composable
private fun StatusCard(rep: ReportDto) {
    val s = rep.submission
    Box("status") {
        Box("line") { B { Text("Отчёт директору") }; StatusChip(s) }
        Box("meta", "color:var(--hint);font-size:12px;line-height:1.45") {
            Text(when (s?.status) {
                null -> "Директор пока не видит этот отчёт."
                "submitted" -> "Сдан ${s.submittedAt?.let { dateTime(it) }}. Директор видит данные и проверяет."
                "accepted" -> "Принят ${s.decidedAt?.let { dateTime(it) }}. Данные за месяц зафиксированы."
                else -> "Возвращён ${s.decidedAt?.let { dateTime(it) }}: «${s.comment}»"
            })
        }
        if (s == null || s.status == "returned") Btn(if (s == null) "Сдать отчёт директору" else "Сдать повторно", "primary", css = "border:0;border-radius:10px;padding:10px;font:700 14px var(--body)") { openSubmit(rep) }
    }
}

@Composable
fun InboxScreen() {
    val m = App.month
    Title("Отчёты репетиторов"); ReportSwitch(); MonthBar()
    Show(remote(m, "inbox") { Api.get<List<InboxRowDto>>("/api/report/inbox?month=$m") }) { rows ->
        val order = mapOf("submitted" to 0, "returned" to 1, "accepted" to 3)
        val sorted = rows.sortedBy { order[it.submission?.status] ?: 2 }
        val waiting = rows.count { it.submission?.status == "submitted" }; val accepted = rows.count { it.submission?.status == "accepted" }
        Box("stats") { StatCard(waiting.toString(), "ждут проверки"); StatCard(accepted.toString(), "принято"); StatCard((rows.size - waiting - accepted).toString(), "не сдано или возвращено") }
        Sec(monthName(m))
        Box("group") {
            if (rows.isEmpty()) Empty("Репетиторов нет.")
            sorted.forEach { r ->
                Box("row", onClick = { openSubmission(r, m) }) {
                    Box("main") {
                        Box("name") { Text(App.tutorName(r.tutor)) }
                        Box("meta") { Text("${r.totals.lessons} зан. · ${rub(r.totals.billed)}" + (r.submission?.submittedAt?.let { " · " + dateTime(it) } ?: "")) }
                    }
                    when (r.submission?.status) { null -> Chip("не сдан"); "submitted" -> Chip("ждёт проверки", "wait"); "accepted" -> Chip("принят", "ok"); else -> Chip("возвращён", "del") }
                }
            }
        }
        Box("actions", "margin-top:12px") { Btn("Скачать все отчёты за ${monthName(m).lowercase()} (.zip)") { exportNow(ExportInput("month", m, emptyList(), "all", "file")) } }
        Lock("Отчёт можно принять, даже если репетитор его ещё не сдал. Скачать можно в любом статусе.")
    }
}

@Composable
private fun Delta(cur: Double, prev: Double) {
    if (prev == 0.0) { Span({ classes("delta", "flat") }) { Text("нет данных за прошлый период") }; return }
    val p = ((cur - prev) / prev * 100).roundToInt()
    Span({ classes("delta", if (p > 0) "up" else if (p < 0) "down" else "flat") }) { Text("${if (p > 0) "▲" else if (p < 0) "▼" else "■"} ${kotlin.math.abs(p)}% к прошлому периоду") }
}

@Composable
fun StatsScreen() {
    val full = App.seesAll
    val n = if (full) ReportView.period else 1
    Title("Статистика"); ReportSwitch()
    if (full) Seg(listOf(1 to "Месяц", 3 to "3 месяца", 6 to "6 месяцев"), n) { ReportView.period = it }
    else Box("lock", "margin-bottom:10px") { Text("Статистика за текущий месяц. Прошлые месяцы видят администратор и директор.") }
    Show(remote(n, "stats") { Api.get<StatsDto>("/api/stats?months=$n") }) { st ->
        val data = st.months.takeLast(n)
        val prev = if (full) st.months.dropLast(n).takeLast(n).takeIf { it.size == n } ?: emptyList() else emptyList()
        Box("kpis") {
            Box("kpi") { Span { Text("Занятия") }; B { Text(data.sumOf { it.lessons }.toString()) }; if (full) Delta(data.sumOf { it.lessons }.toDouble(), prev.sumOf { it.lessons }.toDouble()) }
            if (App.money) {
                Box("kpi") { Span { Text("Начислено") }; B({ classes("num") }) { Text(rub(data.sumOf { it.billed })) }; Delta(data.sumOf { it.billed }, prev.sumOf { it.billed }) }
                Box("kpi") { Span { Text("Получено") }; B({ classes("num") }) { Text(rub(data.sumOf { it.received })) }; Delta(data.sumOf { it.received }.toDouble(), prev.sumOf { it.received }.toDouble()) }
            }
            Box("kpi") { Span { Text("Активных учеников") }; B { Text((data.maxOfOrNull { it.students } ?: 0).toString()) }; Span({ classes("delta", "flat") }) { Text("максимум за период") } }
        }
        if (n > 1 && App.money) { Sec("Начислено и получено"); Box("card") { BarChart(data) } }
        val ind = data.sumOf { it.ind }; val grp = data.sumOf { it.grp }; val tot = maxOf(1, ind + grp)
        Sec("Типы занятий")
        Box("card") {
            Box("split") { I({ style("width:${ind * 100.0 / tot}%;background:var(--accent)") }) {}; I({ style("width:${grp * 100.0 / tot}%;background:var(--warn)") }) {} }
            Box("legend", "margin-bottom:6px") {
                Span { I({ style("background:var(--accent)") }) {}; Text("индивидуальные $ind (${ind * 100 / tot}%)") }
                Span { I({ style("background:var(--warn)") }) {}; Text("групповые $grp (${grp * 100 / tot}%)") }
            }
        }
        val subjTot = mutableMapOf<String, Int>(); data.forEach { it.subj.forEach { (k, v) -> subjTot[k] = (subjTot[k] ?: 0) + v } }
        val subjMax = maxOf(1, subjTot.values.maxOrNull() ?: 1)
        Sec("По предметам")
        Box("card hbars") {
            if (subjTot.isEmpty()) Empty("Занятий нет.")
            App.b.subjects.filter { subjTot[it.id.toString()] != null }.forEach { sb ->
                val v = subjTot[sb.id.toString()]!!
                Box("r") { Span { Text(sb.name) }; Span({ classes("num") }) { Text(v.toString()) }; Box("bar") { I({ style("width:${v * 100.0 / subjMax}%") }) {} } }
            }
        }
        Sec("Загрузка по дням недели")
        Box("card") { WeekChart(st.weekday) }
        Sec("Больше всего занятий в ${inMonth(App.currentMonth)}")
        Box("group") {
            if (st.top.isEmpty()) Empty("Занятий пока нет.")
            st.top.forEach { x -> Box("row", onClick = { openStudent(x.id) }) { Avatar(x.id, x.fio); Box("main") { Box("name") { Text(x.fio) } }; Box("num") { Text(x.n.toString()) } } }
        }
        st.debts?.let { debts ->
            Sec("Долги за текущий месяц")
            Box("group") {
                if (debts.isEmpty()) Empty("Долгов нет.")
                debts.forEach { x -> Box("row", onClick = { openStudent(x.id) }) { Avatar(x.id, x.fio); Box("main") { Box("name") { Text(x.fio) }; Box("meta") { Text("начислено минус оплачено") } }; Box("num", "color:var(--bad)") { Text(rub(x.owe)) } } }
            }
        }
        st.admin?.let { a ->
            Sec("По репетиторам · ${monthName(App.currentMonth).lowercase()}")
            Box("group") {
                if (a.perTutor.isEmpty()) Empty("Нет данных.")
                a.perTutor.forEach { x ->
                    Box("row", "align-items:flex-start") {
                        Box("main") {
                            Box("name") { Text(App.tutorName(x.tutor)) }
                            Box("meta") {
                                Text("${x.n} занятий ")
                                if (x.np > 0) Span({ classes("delta", if (x.n >= x.np) "up" else "down") }) { Text("${if (x.n >= x.np) "▲" else "▼"} к прошлому ${x.np}") }
                                Text(" · ${x.slots} слотов в неделю")
                            }
                        }
                        Box("right") {
                            Box("num") { Text(rub(x.billed)) }; Box("meta num") { Text("получено ${rub(x.received)}") }
                            if (x.billed - x.received > 0) Box("meta num", "color:var(--bad)") { Text("долг ${rub(x.billed - x.received)}") }
                        }
                    }
                }
            }
            Sec("Прогноз на ${monthName(App.currentMonth).lowercase()}")
            Box("card") {
                B({ classes("num"); style("font-size:22px") }) { Text("${a.forecastLo}–${a.forecastHi} тыс ₽") }
                Box("meta", "color:var(--hint);font-size:12px;margin-top:6px;line-height:1.45") { Text("Уже начислено ${rub(a.forecastDone)} + оставшиеся слоты расписания за вычетом обычных 7–18% пропусков.") }
            }
            Sec("Ученики: приход и уход")
            Box("card") {
                Box("legend", "margin-bottom:8px") { Span { Text("занимаются ${a.active}") }; Span { Text("пауза ${a.paused}") }; Span { Text("ушли ${a.left}") } }
                Box("meta", "font-size:12px;color:var(--hint);margin-bottom:4px") { Text("Причины ухода") }
                if (a.reasons.isEmpty()) Text("—")
                a.reasons.forEach { (r, c) -> Div({ style("display:flex;justify-content:space-between;font-size:13px;padding:3px 0") }) { Span { Text(r) }; Span({ classes("num") }) { Text(c.toString()) } } }
            }
            Sec("Какие источники приводят тех, кто остаётся")
            Box("card hbars") {
                if (a.sources.isEmpty()) Text("—")
                a.sources.forEach { r -> Box("r") { Span { Text(r.source) }; Span({ classes("num") }) { Text("${r.stay} из ${r.n}") }; Box("bar") { I({ style("width:${r.stay * 100.0 / r.n}%") }) {} } } }
            }
        }
    }
}

@Composable
fun SvgRaw(svg: String) = androidx.compose.runtime.key(svg) { Div({ ref { el -> el.innerHTML = svg; onDispose {} } }) }

@Composable
private fun BarChart(data: List<MonthAggDto>) {
    val w = 330.0; val h = 150.0; val padL = 34.0; val padB = 20.0; val padT = 8.0
    val max = (kotlin.math.ceil(maxOf(1.0, data.maxOf { maxOf(it.billed, it.received.toDouble()) }) / 10000) * 10000).coerceAtLeast(10000.0)
    val bw = (w - padL - 6) / data.size
    fun y(v: Double) = padT + (h - padT - padB) * (1 - v / max)
    val sb = StringBuilder("""<svg viewBox="0 0 $w $h" role="img" aria-label="Начислено и получено по месяцам">""")
    var t = 0.0
    while (t <= max) { sb.append("""<line x1="$padL" x2="$w" y1="${y(t)}" y2="${y(t)}" stroke="var(--line)"/><text x="${padL - 6}" y="${y(t) + 3}" text-anchor="end">${(t / 1000).roundToInt()}к</text>"""); t += max / 2 }
    data.forEachIndexed { i, x ->
        val cx = padL + i * bw + bw / 2; val bwid = minOf(14.0, bw / 3); val op = if (x.partial) .55 else 1.0
        val m = MS[x.m.substring(5, 7).toInt() - 1]
        sb.append("""<rect x="${cx - bwid - 1}" y="${y(x.billed)}" width="$bwid" height="${y(0.0) - y(x.billed)}" rx="2" fill="var(--accent)" opacity="$op"><title>$m: начислено ${rub(x.billed)}</title></rect>""")
        sb.append("""<rect x="${cx + 1}" y="${y(x.received.toDouble())}" width="$bwid" height="${y(0.0) - y(x.received.toDouble())}" rx="2" fill="var(--ok)" opacity="$op"><title>$m: получено ${rub(x.received)}</title></rect>""")
        sb.append("""<text x="$cx" y="${h - 5}" text-anchor="middle" class="${if (x.partial) "lbl" else ""}">$m${if (x.partial) "*" else ""}</text>""")
    }
    sb.append("</svg>")
    SvgRaw(sb.toString())
    Box("legend") {
        Span { I({ style("background:var(--accent)") }) {}; Text("начислено") }
        Span { I({ style("background:var(--ok)") }) {}; Text("получено") }
        Span { Text("* текущий месяц, неполный") }
    }
}

@Composable
private fun WeekChart(load: List<Int>) {
    val wmax = maxOf(1, load.maxOrNull() ?: 1)
    val sb = StringBuilder("""<svg viewBox="0 0 330 92" role="img" aria-label="Занятия по дням недели">""")
    WD.forEachIndexed { i, d ->
        val hh = (load[i] * 58.0 / wmax).roundToInt(); val x = 12 + i * 45
        sb.append("""<rect x="$x" y="${66 - hh}" width="26" height="$hh" rx="3" fill="var(--accent)" opacity="${if (load[i] == wmax) 1 else .5}"/><text x="${x + 13}" y="82" text-anchor="middle">$d</text>""")
        if (load[i] > 0) sb.append("""<text x="${x + 13}" y="${62 - hh}" text-anchor="middle" class="lbl">${load[i]}</text>""")
    }
    sb.append("</svg>")
    SvgRaw(sb.toString())
}

// ---------- Команда ----------
data class Perm(val k: String, val t: String, val d: String, val isNew: Boolean = false)
val PERMS = listOf(
    Perm("ADD_TIME", "Вносить занятия", "/addlesson и форма занятия"),
    Perm("ADD_PAYMENT", "Вносить и видеть оплаты", "/addpayment, вкладка «Оплаты»"),
    Perm("ADD_CLIENT", "Добавлять учеников", "/addstudent и форма ученика"),
    Perm("FORCE_GROUP", "Делать занятие групповым", "кнопка «Сделать групповым»"),
    Perm("EXTRA_HALF_HOUR", "Полтора часа", "кнопка «Сделать полтора часа», ×1,5"),
    Perm("ADD_TIME_FOR_OTHERS", "Занятия за других", "/addlessonforother, выбор репетитора"),
    Perm("ADD_PAYMENT_FOR_OTHERS", "Оплаты за других", "/addpaymentforother"),
    Perm("WEB_INTERFACE", "Веб-интерфейс", "вход в мини-приложение"),
    Perm("DIRECTOR", "Проверка отчётов", "входящие отчёты, принять или вернуть; только просмотр", true),
    Perm("MANAGE_TUTORS", "Управление преподавателями", "добавлять преподавателей, менять права и предметы", true),
    Perm("ADMIN", "Администратор", "всё перечисленное, у всех, в любом месяце"),
)
val ADMIN_IMPLIES = listOf("ADD_TIME", "ADD_PAYMENT", "ADD_CLIENT", "ADD_TIME_FOR_OTHERS", "ADD_PAYMENT_FOR_OTHERS", "MANAGE_TUTORS", "FORCE_GROUP", "EXTRA_HALF_HOUR")
val TUTOR_PRESET = listOf("ADD_TIME", "ADD_PAYMENT", "ADD_CLIENT", "FORCE_GROUP", "EXTRA_HALF_HOUR", "WEB_INTERFACE")
val PRESETS = listOf(
    "Репетитор" to TUTOR_PRESET,
    "Без оплат" to listOf("ADD_TIME", "EXTRA_HALF_HOUR", "WEB_INTERFACE"),
    "Старший" to listOf("ADD_TIME", "ADD_PAYMENT", "ADD_CLIENT", "FORCE_GROUP", "EXTRA_HALF_HOUR", "ADD_TIME_FOR_OTHERS", "ADD_PAYMENT_FOR_OTHERS", "WEB_INTERFACE"),
    "Директор" to listOf("DIRECTOR", "WEB_INTERFACE"),
    "Админ" to listOf("ADMIN", "WEB_INTERFACE"),
)

@Composable
fun PermChips(list: List<String>) = Box("", "margin-top:5px;display:flex;gap:3px;flex-wrap:wrap") {
    if (list.isEmpty()) Span({ classes("pc") }) { Text("нет прав — нет доступа") }
    list.forEach { p -> Span({ classes("pc"); if (PERMS.find { it.k == p }?.isNew == true) classes("new") }) { Text(p) } }
}

@Composable
fun TutorsScreen() {
    Title("Команда")
    Show(remote("tutors") { Api.get<List<TutorDto>>("/api/tutors") }) { list ->
        fun staff(t: TutorDto) = t.subjects.isEmpty() && "ADD_TIME" !in t.perms
        listOf("Преподаватели" to list.filter { it.perms.isNotEmpty() && !staff(it) }, "Руководство" to list.filter { it.perms.isNotEmpty() && staff(it) }, "Отключены" to list.filter { it.perms.isEmpty() })
            .filter { it.second.isNotEmpty() }.forEach { (title, l) ->
                Sec(title)
                Box("group") {
                    l.forEach { t ->
                        Box("row", "align-items:flex-start", onClick = { openTutor(t) }) {
                            Box("av", "background:${PALETTE[(t.id % PALETTE.size).toInt()]}") { Text(t.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("")) }
                            Box("main") {
                                Box("name") { Text(t.name + if (t.id == App.me) " (вы)" else "") }
                                Box("meta") {
                                    Text((if (t.subjects.isNotEmpty()) t.subjects.joinToString(", ") { App.subj(it).name } else "без предметов") + " · ")
                                    UserLink(t.id, "Telegram")
                                    t.rate?.let { Text(" · ставка $it%") }
                                }
                                PermChips(t.perms)
                            }
                        }
                    }
                }
            }
        Lock("Права хранятся в tutors.permissions. Изменения сразу действуют в веб-интерфейсе, а меню команд в боте обновляется в течение 10 минут.")
    }
}

// ---------- Журнал ----------
val ETYPES = mapOf("slot" to "Расписание", "lesson" to "Занятие", "payment" to "Оплата", "student" to "Ученик", "tutor" to "Преподаватель", "report" to "Отчёт", "session" to "Вход")

object AuditView {
    var type by mutableStateOf("all"); var query by mutableStateOf(""); var limit by mutableStateOf(25)
    var record by mutableStateOf<Triple<String, String, String>?>(null)
    fun showRecord(type: String, id: String, title: String) { record = Triple(type, id, title); limit = 25; App.tab = "audit"; Sheets.closeAll() }
}

fun auditChanges(e: AuditDto): List<Triple<String, String, String>> = runCatching {
    (e.changes as kotlinx.serialization.json.JsonArray).map { c ->
        val a = c as kotlinx.serialization.json.JsonArray
        fun v(i: Int) = a[i].let { if (it is kotlinx.serialization.json.JsonNull) "—" else (it as kotlinx.serialization.json.JsonPrimitive).content.let { s -> if (s == "true") "да" else if (s == "false") "нет" else s.ifEmpty { "—" } } }
        Triple(v(0), v(1), v(2))
    }
}.getOrDefault(emptyList())

@Composable
fun AuditScreen() {
    Title("Журнал изменений")
    Box("actions", "margin:0 0 10px") {
        Btn("Выгрузить .csv") { exportNow(ExportInput("audit", App.currentMonth, filter = AuditView.type)) }
        Btn("Оповещения") { openAlerts() }
    }
    val rec = AuditView.record
    if (rec != null) Box("status") { Box("line") { B { Text("${ETYPES[rec.first] ?: rec.first}: ${rec.third}") }; Btn("Все записи", "iconbtn") { AuditView.record = null } } }
    else {
        Seg(listOf("all" to "Все", "lesson" to "Занятия", "payment" to "Оплаты", "student" to "Ученики", "slot" to "График", "tutor" to "Команда", "report" to "Отчёты"), AuditView.type, "grid-auto-columns:max-content;overflow-x:auto") { AuditView.type = it; AuditView.limit = 25 }
        TextField(AuditView.query, "Ученик, преподаватель или действие", InputType.Search, cls = "search") { AuditView.query = it; AuditView.limit = 25 }
    }
    val url = buildString {
        append("/api/audit?limit=${AuditView.limit}")
        if (rec != null) append("&rtype=${rec.first}&rid=${rec.second}")
        else { append("&type=${AuditView.type}"); if (AuditView.query.isNotBlank()) append("&q=" + js("encodeURIComponent")(AuditView.query) as String) }
    }
    Show(remote(url) { Api.get<AuditPageDto>(url) }) { page ->
        Sec("${page.total} ${if (page.total == 1) "запись" else "записей"}")
        Box("group") {
            if (page.items.isEmpty()) Empty("Ничего не найдено.")
            page.items.forEach { e ->
                val ch = auditChanges(e)
                Box("row", "align-items:flex-start", onClick = { openAudit(e) }) {
                    Box("num", "width:62px;color:var(--hint);font-size:12px;flex:none") { Text(shortDay(e.at.substring(0, 10)) + ", " + e.at.substring(11, 16)) }
                    Box("main") {
                        if (e.type == "session" && (e.action == "Вход" || e.action == "Нет доступа")) {
                            Box("name") { Text(if (e.action == "Вход") "Вход в приложение" else "Отказ во входе") }
                            Box("meta") { Text(e.title.ifEmpty { e.actorName }) }
                            Box("meta") { Text(ch.find { it.first == "устройство" }?.third ?: "") }
                        } else {
                            Box("name") { Text("${e.action} · ${(ETYPES[e.type] ?: e.type).lowercase()}") }
                            Box("meta") { Text(e.title) }
                            Box("meta") { Text(e.actorName + if (ch.isNotEmpty()) " · изменений: ${ch.size}" else "") }
                        }
                    }
                    Span({ classes("chip", "src") }) { Text(e.source) }
                }
            }
        }
        if (page.total > page.items.size) Actions { Btn("Показать ещё") { AuditView.limit += 25 } }
        else if (page.total > 25) Empty("Это всё: начало журнала.")
        Lock("Журнал хранится в базе (таблица audit_log): его можно искать, фильтровать и выгружать. Записи пишут и веб, и бот (метка WEB / BOT); изменить или удалить запись нельзя.")
    }
}
