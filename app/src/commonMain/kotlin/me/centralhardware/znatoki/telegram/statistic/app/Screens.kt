package me.centralhardware.znatoki.telegram.statistic.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.http.encodeURLParameter
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

/** ‹ Month › stepping through the months with data. */
@Composable
fun Stepper(title: String, canBack: Boolean, canForward: Boolean, showForward: Boolean = true, onBack: () -> Unit, onForward: () -> Unit) =
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Btn("‹", "iconbtn", enabled = canBack, modifier = Modifier.width(48.dp)) { onBack() }
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        // Hidden rather than removed on the current month, so the title stays centred.
        Box(Modifier.width(48.dp).alpha(if (showForward) 1f else 0f)) { Btn("›", "iconbtn", enabled = canForward && showForward) { onForward() } }
    }

@Composable
fun MonthBar() {
    if (!App.seesAll) return
    val ms = App.b.months
    val i = ms.indexOf(App.month)
    Stepper(monthName(App.month), i > 0, i in 0 until ms.size - 1, App.month != App.currentMonth, { App.month = ms[i - 1] }, { App.month = ms[i + 1] })
}

fun viewMonth() = if (App.seesAll) App.month else App.currentMonth

@Composable
fun LessonRow(l: LessonDto, selectable: Boolean = false) = ListRow({ openLesson(l.id) }, dim = l.deleted) {
    if (selectable) Checkbox(l.id in TodayView.selected, { if (it) TodayView.selected.add(l.id) else TodayView.selected.remove(l.id) }, Modifier.size(24.dp))
    TimeCol(l.time)
    Main {
        Name(names(l))
        Meta {
            MetaText((if (App.seesAll && App.who != "me") App.tutorShort(l.tutor) + " · " else "") + App.subj(l.subject).name + " ·")
            if (isGroup(l)) Chip("группа" + if (l.students.size > 1) " · ${l.students.size}" else "", "grp") else Chip("индивид.")
            if (l.half) Chip("+½ ч", "half")
            l.flags.forEach { Chip("⚠ $it", "del") }
            if (l.deleted) Chip("удалено", "del")
        }
    }
    if (App.money) Num(rub(price(l) * l.students.size))
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
        // The feed also shows lessons the tutor entered for others; the counters are only their own, as in the report.
        val own = { l: LessonDto -> who != "me" || l.tutor == App.me }
        val live = all.filter { !it.deleted && own(it) }
        Stats {
            if (isCur) StatCard(todays.count { !it.deleted && own(it) }.toString(), "занятий сегодня")
            StatCard(live.size.toString(), "за месяц")
            if (App.money) StatCard(k1(live.sumOf { price(it) * it.students.size }), "₽ начислено", true)
        }
        val flagged = all.filter { it.flags.isNotEmpty() }
        if (flagged.isNotEmpty()) Card(accent = colors.bad) {
            Text("Требуют проверки: ${flagged.size}", fontWeight = FontWeight.Bold)
            flagged.forEach { l -> MetaText("${dayMonth(l.date)} ${l.time} · ${App.tutorShort(l.tutor)} · ${l.flags.joinToString(", ")}") }
        }
        if (isCur && App.caps.time && who == "me") {
            val sch = remote("sched") { Api.get<ScheduleDto>("/api/schedule") }
            if (sch is Remote.Ok) {
                val todo = slotsOn(App.today, sch.data.slots).count { markOf(sch.data, it.id, App.today) == null }
                if (todo > 0) Card(onClick = { App.tab = "schedule" }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("По расписанию не отмечено: ", Modifier.weight(1f, fill = false)); Text(todo.toString(), fontWeight = FontWeight.Bold)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { Chip("График ›", "grp") }
                    }
                }
            }
        }
        if (isCur) {
            Sec("Сегодня")
            Group { if (todays.isEmpty()) Empty("Сегодня занятий пока нет.") else todays.forEach { LessonRow(it) } }
        }
        all.filter { !(isCur && it.date == App.today) }.groupBy { it.date }.entries.sortedByDescending { it.key }.forEach { (d, ls) ->
            Sec(dayMonth(d)); Group { ls.forEach { LessonRow(it) } }
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Dropdown(listOf<Pair<Long?, String>>(null to "Все репетиторы") + App.teaching().map { it.id to it.name }, TodayView.fTutor, modifier = Modifier.weight(1f)) { TodayView.fTutor = it }
            Dropdown(listOf<Pair<Long?, String>>(null to "Все предметы") + App.b.subjects.map { it.id to it.name }, TodayView.fSubject, modifier = Modifier.weight(1f)) { TodayView.fSubject = it }
        }
        Dropdown(listOf("live" to "Действующие", "flag" to "Требуют проверки", "deleted" to "Удалённые", "all" to "Все, включая удалённые"), TodayView.fStatus) { TodayView.fStatus = it }
        TextField(TodayView.fQuery, "Ученик", search = true) { TodayView.fQuery = it }
    }
    Show(remote(m, "all") { loadLessons(m, "all") }) { all ->
        val q = TodayView.fQuery.trim().lowercase()
        val list = all.filter { l ->
            (TodayView.fTutor == null || l.tutor == TodayView.fTutor) && (TodayView.fSubject == null || l.subject == TodayView.fSubject) &&
                when (TodayView.fStatus) { "live" -> !l.deleted; "deleted" -> l.deleted; "flag" -> l.flags.isNotEmpty(); else -> true } &&
                (q.isEmpty() || l.students.any { it.fio.lowercase().contains(q) })
        }
        // Selection outside the filter is ignored here, not pruned: writing state during composition re-triggers it forever.
        val chosen = list.filter { it.id in TodayView.selected }
        val live = list.filterNot { it.deleted }
        Gap(8.dp)
        Stats {
            StatCard(list.size.toString(), "найдено")
            StatCard(live.sumOf { it.students.size }.toString(), "ученико-занятий")
            StatCard(k1(live.sumOf { price(it) * it.students.size }), "₽ начислено", true)
        }
        if (list.isNotEmpty()) Sec("Занятия") {
            Btn(if (chosen.size == list.size) "Снять выбор" else "Выбрать все", "iconbtn") {
                if (chosen.size == list.size) TodayView.selected.clear() else { TodayView.selected.clear(); TodayView.selected.addAll(list.map { it.id }) }
            }
        }
        if (list.isEmpty()) Empty("Под фильтр ничего не подходит.")
        list.groupBy { it.date }.entries.sortedByDescending { it.key }.forEach { (d, ls) -> Sec(dayMonth(d)); Group { ls.forEach { LessonRow(it, true) } } }
        if (m != App.currentMonth) Lock("Закрытый месяц: правки попадут в журнал и в уже сданный отчёт.")
        if (chosen.isNotEmpty()) BulkBar(chosen)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BulkBar(chosen: List<LessonDto>) = Card(accent = colors.accent) {
    Text("Выбрано: ${chosen.size}", fontWeight = FontWeight.Bold)
    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (chosen.any { !it.deleted }) Btn("Удалить", "mini del") { bulk(chosen, "delete", null) }
        if (chosen.any { it.deleted }) Btn("Восстановить", "mini") { bulk(chosen, "restore", null) }
        Btn("Репетитор", "mini") { openBulk(chosen, "tutor") }
        Btn("Цена", "mini") { openBulk(chosen, "price") }
        Btn("Перенести", "mini") { openBulk(chosen, "day") }
        Btn("✕", "mini ghost") { TodayView.selected.clear() }
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
    ListRow(mk?.lesson?.let { id -> { openLesson(id) } }, dim = mk?.status == "skip") {
        TimeCol(sl.time)
        Main {
            Name(sl.students.joinToString(", ") { it.fio })
            MetaText(App.subj(sl.subject).name + (if (sl.students.size > 1) " · группа" else "") + (if (sl.onceDate != null) " · разово" else "") + (if (mk?.status == "skip") " · отменено" else ""))
        }
        when (mk?.status) {
            "done" -> Chip("проведено", "ok")
            "skip" -> Btn("Вернуть", "mini ghost") { App.act { Api.post<OkDto, DateInput>("/api/schedule/slots/${sl.id}/unmark", DateInput(App.today)) } }
            else -> Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Btn("Внести", "mini") { openAddLesson(LessonPrefill.fromSlot(sl)) }
                Btn("Не было", "mini ghost") { App.act("Отмечено: занятия не было") { Api.post<OkDto, DateInput>("/api/schedule/slots/${sl.id}/skip", DateInput(App.today)) } }
            }
        }
    }
}

@Composable
fun ScheduleToday(s: ScheduleDto) {
    val list = slotsOn(App.today, s.slots).sortedBy { it.time }
    val todo = list.count { markOf(s, it.id, App.today) == null }
    Sec("По расписанию · ${WDF[dow(App.today)].lowercase()}")
    Group { if (list.isEmpty()) Empty("На сегодня в расписании ничего нет.") else list.forEach { SlotTodayRow(s, it) } }
    Lock((if (todo > 0) "Не отмечено: $todo. " + (if (s.reminders.evening) "В ${s.reminders.at} бот напомнит, если так и останется." else "Напоминания выключены.") else "Все занятия по расписанию отмечены.") +
        " «Внести» открывает обычную форму занятия, уже заполненную из слота.")
}

@Composable
fun SlotLine(x: SlotDto, showDate: Boolean) = ListRow {
    TimeCol(x.time)
    Main {
        Name(x.students.joinToString(", ") { it.fio })
        MetaText((if (showDate && x.onceDate != null) dayMonth(x.onceDate) + " · " else "") + App.subj(x.subject).name + (if (App.money) " · " + rub(x.amount) else ""))
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
    Title("Расписание")
    val sch = remote("sched") { Api.get<ScheduleDto>("/api/schedule") }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Seg(listOf(true to "Календарь", false to "Список"), ScheduleView.cal, Modifier.weight(1f)) { ScheduleView.cal = it }
        if (sch is Remote.Ok) Btn("Напоминания", "iconbtn") { openReminders(sch.data.reminders) }
    }
    val m = App.currentMonth
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
    Text(monthName(m), Modifier.fillMaxWidth().padding(vertical = 8.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    val cells: List<Int?> = List(dow("$m-01")) { null } + (1..daysInMonth(m)).toList()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.bg).padding(6.dp)) {
        EqualRow(gap = 2.dp) { WD.forEach { Text(it, color = colors.hint, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) } }
        cells.chunked(7).forEach { week ->
            EqualRow(Modifier.padding(top = 2.dp), 2.dp) {
                (week + List(7 - week.size) { null }).forEach { d ->
                    if (d == null) Box(Modifier.height(46.dp)) {}
                    else {
                        val x = iso(d); val past = x < App.today
                        val held = mine.count { !it.deleted && it.date == x }
                        val n = when { past -> held; x == App.today -> maxOf(held, slotsOn(x, s.slots).size); else -> slotsOn(x, s.slots).size }
                        val c = colors
                        Column(
                            Modifier.height(46.dp).clip(RoundedCornerShape(9.dp))
                                .background(if (x == sel) c.accent else if (x == App.today) c.accentBg else Color.Transparent)
                                .clickable { ScheduleView.day = x },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            val fg = if (x == sel) c.accentFg else if (past) c.hint else c.fg
                            Text(d.toString(), color = fg, fontWeight = if (x == App.today) FontWeight.ExtraBold else FontWeight.SemiBold, fontSize = 13.sp)
                            Text("•".repeat(minOf(n, 4)), color = if (x == sel) c.accentFg else c.accent, fontSize = 9.sp, lineHeight = 9.sp)
                        }
                    }
                }
            }
        }
    }
    when {
        sel == App.today -> ScheduleToday(s)
        sel < App.today -> {
            val items = mine.filter { !it.deleted && it.date == sel }.sortedBy { it.time }
            Sec("${dayMonth(sel)} · проведено"); Group { if (items.isEmpty()) Empty("Занятий не было.") else items.forEach { LessonRow(it) } }
        }
        else -> {
            val items = slotsOn(sel, s.slots).sortedBy { it.time }
            Sec("${dayMonth(sel)}, ${WDF[dow(sel)].lowercase()} · запланировано")
            Group {
                if (items.isEmpty()) Empty("Ничего не запланировано.")
                items.forEach { x ->
                    if (x.onceDate != null) SlotLine(x, false) else ListRow {
                        TimeCol(x.time)
                        Main { Name(x.students.joinToString(", ") { it.fio }); MetaText(App.subj(x.subject).name + " · каждую неделю") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleList(s: ScheduleDto) {
    ScheduleToday(s)
    val once = s.slots.filter { it.onceDate != null && it.onceDate > App.today }.sortedBy { it.onceDate + it.time }
    if (once.isNotEmpty()) { Sec("Разовые", top = 22.dp); Group { once.forEach { SlotLine(it, true) } } }
    Sec("Каждую неделю", top = 22.dp)
    val weekly = s.slots.filter { it.onceDate == null }
    if (weekly.isEmpty()) Empty("Регулярных занятий нет. Добавьте их кнопкой внизу.")
    WD.indices.forEach { i ->
        val l = weekly.filter { it.weekday == i }.sortedBy { it.time }
        if (l.isNotEmpty()) { Sec(WDF[i]); Group { l.forEach { SlotLine(it, false) } } }
    }
    Lock("Слоты повторяются каждую неделю. В свой день они появляются вверху с кнопкой «Внести».")
}

// ---------- Ученики ----------
object StudentsView { var query by mutableStateOf(""); var filter by mutableStateOf("active") }
const val LONG = 182 // half a year

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
fun StudentRow(s: StudentRowDto) = ListRow({ openStudent(s.id) }, dim = s.deleted) {
    Avatar(s.id, sFio(s))
    Main {
        NameLine { Name(sFio(s)); if (s.status != "active") Chip(STATUS[s.status]!!.lowercase(), if (s.status == "left") "del" else "half") }
        Meta {
            MetaText((s.cls?.let { "$it класс · " } ?: "") + (s.subjects.joinToString(", ") { App.subj(it).name }.ifEmpty { "занятий ещё не было" }))
            if (isLong(s)) MetaText("· " + (s.lastDate?.let { "последнее ${dayMonth(it)}, ${lastAgo(s)} дн. назад" } ?: "занятий не было"), colors.warn)
            if ((s.balance ?: 0) < 0) MetaText("· долг ${rub(-s.balance!!)}", colors.bad)
        }
    }
    if (s.deleted) Chip("удалён", "del") else Right { Num(s.monthCount.toString()); MetaText("в " + inMonth(App.currentMonth)) }
}

@Composable
fun StudentsScreen() {
    Title("Ученики")
    TextField(StudentsView.query, "Поиск по ФИО, опечатки не страшны", search = true) { StudentsView.query = it }
    Lock("Удалённые ученики здесь не показываются.")
    Show(remote("students") { loadStudents() }) { all ->
        val live = all.filterNot { it.deleted }
        val longN = live.count { isLong(it) }
        val f = StudentsView.filter
        Seg(listOf("active" to "Занимаются", "long" to "Давно", "paused" to "Пауза", "left" to "Ушли", "all" to "Все").map { (v, t) ->
            v to "$t " + when (v) { "all" -> live.size; "long" -> longN; else -> live.count { it.status == v } }
        }, f) { StudentsView.filter = it }
        if (f != "long" && longN > 0) Card(accent = colors.warn, onClick = { StudentsView.filter = "long" }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Давно не было занятий: "); Text(longN.toString(), Modifier.weight(1f), fontWeight = FontWeight.Bold); Chip("полгода+", "half")
            }
        }
        var list = live.filter { f == "all" || (if (f == "long") isLong(it) else it.status == f) }
        if (f == "long") list = list.sortedByDescending { lastAgo(it) ?: Int.MAX_VALUE }
        val q = StudentsView.query.trim()
        if (q.isNotEmpty()) {
            val words = q.split(Regex("\\s+"))
            list = list.map { s -> s to words.minOf { w -> minOf(fuzzyScore(w, s.last), fuzzyScore(w, s.first)) } }.filter { it.second <= 2 }.sortedBy { it.second }.map { it.first }
        }
        Sec(if (q.isNotEmpty()) "Найдено: ${list.size}" else "Учеников: ${list.size}")
        Group { if (list.isEmpty()) Empty(if (q.isNotEmpty()) "По запросу «$q» никого нет. Проверьте написание фамилии." else "Никого нет.") else list.forEach { StudentRow(it) } }
        val deleted = all.filter { it.deleted }
        if (App.caps.admin && deleted.isNotEmpty()) { Sec("Удалённые"); Group { deleted.forEach { StudentRow(it) } } }
    }
}

// ---------- Оплаты ----------
@Composable
fun PaymentsScreen() {
    val m = viewMonth()
    Title("Оплаты"); WhoBar(); MonthBar()
    val who = if (App.seesAll) App.who else "me"
    Show(remote(m, who, "pay") { Api.get<List<PaymentDto>>("/api/payments?month=$m&tutor=$who") }) { list ->
        Group {
            if (list.isEmpty()) Empty("В этом месяце оплат нет.")
            list.forEach { p ->
                ListRow({ openPayment(p.id) }, dim = p.deleted) {
                    Avatar(p.student.id, p.student.fio)
                    Main {
                        Name(p.student.fio)
                        Meta {
                            MetaText("${dayMonth(p.date)} · ${App.subj(p.subject).name}" + (if (App.seesAll && App.who != "me") " · " + App.tutorShort(p.tutor) else "") + " ·")
                            when { p.deleted -> Chip("удалена", "del"); p.hasPhoto -> Chip("чек", "ok"); else -> Chip("без чека", "half") }
                        }
                    }
                    if (App.money) Num(rub(p.amount))
                }
            }
        }
        if (App.money) {
            Sec("Всего получено")
            Group { ListRow { Main { Name(monthName(m)) }; Num(rub(list.filterNot { it.deleted }.sumOf { it.amount })) } }
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
        val who = if (rt == App.me) "me" else rt.toString()
        rep.subjects.forEach { sb ->
            Sec(App.subj(sb.subject).name) {
                Btn("Просмотр", "iconbtn") { openPreview(rt, m, listOf(sb.subject)) }
                Btn("Скачать", "iconbtn") { exportNow(ExportInput("month", m, listOf(sb.subject), who, "file")) }
                Btn("В чат", "iconbtn") { exportNow(ExportInput("month", m, listOf(sb.subject), who, "chat")) }
            }
            Group {
                sb.students.forEach { r ->
                    ListRow(top = true) {
                        Main {
                            NameLine { Name(r.fio); if (r.deleted) Chip("удалён", "del") }
                            MetaText("индивид.: ${r.ind} · групп.: ${r.grp}")
                            if (App.money && pay) Bar(if (r.sum > 0) (r.paid / r.sum).toFloat() else 0f)
                        }
                        Right {
                            if (App.money) { Num(rub(r.sum)); if (pay) Num("оплачено ${rub(r.paid)}", colors.hint, Modifier) }
                            else { Num((r.ind + r.grp).toString()); MetaText("занятий") }
                        }
                    }
                }
                ListRow {
                    Main { Name("Итого"); if (App.money) MetaText(if (pay) "начислено / получено" else "начислено") }
                    Right {
                        if (App.money) { Num(rub(sb.billed)); if (pay) Num(rub(sb.received), colors.hint) }
                        else { Num(sb.lessons.toString()); MetaText("занятий") }
                    }
                }
            }
        }
        if (rep.subjects.isEmpty()) Empty("Занятий за ${monthName(m).lowercase()} нет.")
        rep.payroll?.let { pr ->
            Sec("Зарплата репетиторов · ${monthName(m).lowercase()}")
            Group {
                if (pr.isEmpty()) Empty("Начислений нет.")
                pr.forEach { x ->
                    ListRow {
                        Main { Name(App.tutorName(x.tutor)); MetaText(x.rate?.let { "$it% от начисленного ${rub(x.billed)}" } ?: "ставка не задана · начислено ${rub(x.billed)}") }
                        Num(x.pay?.let { rub(it) } ?: "—")
                    }
                }
                ListRow { Main { Name("Итого к выплате") }; Num(rub(pr.sumOf { it.pay ?: 0 })) }
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
    Card(accent = colors.accent) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Отчёт директору", Modifier.weight(1f), fontWeight = FontWeight.Bold); StatusChip(s) }
        Gap(4.dp)
        MetaText(when (s?.status) {
            null -> "Директор пока не видит этот отчёт."
            "submitted" -> "Сдан ${s.submittedAt?.let { dateTime(it) }}. Директор видит данные и проверяет."
            "accepted" -> "Принят ${s.decidedAt?.let { dateTime(it) }}. Данные за месяц зафиксированы."
            else -> "Возвращён ${s.decidedAt?.let { dateTime(it) }}: «${s.comment}»"
        })
        if (s == null || s.status == "returned") Actions(10.dp) { Btn(if (s == null) "Сдать отчёт директору" else "Сдать повторно", "primary") { openSubmit(rep) } }
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
        Stats { StatCard(waiting.toString(), "ждут проверки"); StatCard(accepted.toString(), "принято"); StatCard((rows.size - waiting - accepted).toString(), "не сдано или возвращено") }
        Sec(monthName(m))
        Group {
            if (rows.isEmpty()) Empty("Репетиторов нет.")
            sorted.forEach { r ->
                ListRow({ openSubmission(r, m) }) {
                    Main {
                        Name(App.tutorName(r.tutor))
                        MetaText("${r.totals.lessons} зан. · ${rub(r.totals.billed)}" + (r.submission?.submittedAt?.let { " · " + dateTime(it) } ?: ""))
                    }
                    when (r.submission?.status) { null -> Chip("не сдан"); "submitted" -> Chip("ждёт проверки", "wait"); "accepted" -> Chip("принят", "ok"); else -> Chip("возвращён", "del") }
                }
            }
        }
        Actions { Btn("Скачать все отчёты за ${monthName(m).lowercase()} (.zip)") { exportNow(ExportInput("month", m, emptyList(), "all", "file")) } }
        Lock("Отчёт можно принять, даже если репетитор его ещё не сдал. Скачать можно в любом статусе.")
    }
}

@Composable
private fun Delta(cur: Double, prev: Double) {
    if (prev == 0.0) { MetaText("нет данных за прошлый период"); return }
    val p = ((cur - prev) / prev * 100).roundToInt()
    MetaText("${if (p > 0) "▲" else if (p < 0) "▼" else "■"} ${kotlin.math.abs(p)}% к прошлому периоду", if (p > 0) colors.ok else if (p < 0) colors.bad else colors.hint)
}

/** A legend entry: a colour swatch and its label. */
@Composable
private fun Swatch(color: Color?, text: String) = Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
    if (color != null) Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(color))
    MetaText(text)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(content: @Composable () -> Unit) =
    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { content() }

@Composable
fun StatsScreen() {
    val full = App.seesAll
    val n = if (full) ReportView.period else 1
    Title("Статистика"); ReportSwitch()
    if (full) Seg(listOf(1 to "Месяц", 3 to "3 месяца", 6 to "6 месяцев"), n) { ReportView.period = it }
    else Lock("Статистика за текущий месяц. Прошлые месяцы видят администратор и директор.")
    Show(remote(n, "stats") { Api.get<StatsDto>("/api/stats?months=$n") }) { st ->
        val data = st.months.takeLast(n)
        val prev = if (full) st.months.dropLast(n).takeLast(n).takeIf { it.size == n } ?: emptyList() else emptyList()
        Grid2 {
            Kpi("Занятия", data.sumOf { it.lessons }.toString()) { if (full) Delta(data.sumOf { it.lessons }.toDouble(), prev.sumOf { it.lessons }.toDouble()) }
            if (App.money) {
                Kpi("Начислено", rub(data.sumOf { it.billed }), true) { Delta(data.sumOf { it.billed }, prev.sumOf { it.billed }) }
                Kpi("Получено", rub(data.sumOf { it.received }), true) { Delta(data.sumOf { it.received }.toDouble(), prev.sumOf { it.received }.toDouble()) }
            }
            Kpi("Активных учеников", (data.maxOfOrNull { it.students } ?: 0).toString()) { MetaText("максимум за период") }
        }
        if (n > 1 && App.money) { Sec("Начислено и получено"); Card { BarChart(data) } }
        val ind = data.sumOf { it.ind }; val grp = data.sumOf { it.grp }; val tot = maxOf(1, ind + grp)
        Sec("Типы занятий")
        Card {
            Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(colors.line)) {
                if (ind > 0) Box(Modifier.weight(ind.toFloat()).fillMaxHeight().background(colors.accent))
                if (grp > 0) Box(Modifier.weight(grp.toFloat()).fillMaxHeight().background(colors.warn))
            }
            Legend {
                Swatch(colors.accent, "индивидуальные $ind (${ind * 100 / tot}%)")
                Swatch(colors.warn, "групповые $grp (${grp * 100 / tot}%)")
            }
        }
        val subjTot = mutableMapOf<String, Int>(); data.forEach { it.subj.forEach { (k, v) -> subjTot[k] = (subjTot[k] ?: 0) + v } }
        val subjMax = maxOf(1, subjTot.values.maxOrNull() ?: 1)
        Sec("По предметам")
        Card {
            if (subjTot.isEmpty()) Empty("Занятий нет.")
            App.b.subjects.filter { subjTot[it.id.toString()] != null }.sortedByDescending { subjTot[it.id.toString()] }.forEach { sb ->
                val v = subjTot[sb.id.toString()]!!
                Column(Modifier.padding(vertical = 4.dp)) {
                    Row { Text(sb.name, Modifier.weight(1f), fontSize = 13.sp); Num(v.toString()) }
                    Bar(v.toFloat() / subjMax)
                }
            }
        }
        Sec("Загрузка по дням недели")
        Card { WeekChart(st.weekday) }
        Sec("Больше всего занятий в ${inMonth(App.currentMonth)}")
        Group {
            if (st.top.isEmpty()) Empty("Занятий пока нет.")
            st.top.forEach { x -> ListRow({ openStudent(x.id) }) { Avatar(x.id, x.fio); Main { Name(x.fio) }; Num(x.n.toString()) } }
        }
        st.debts?.let { debts ->
            Sec("Долги за текущий месяц")
            Group {
                if (debts.isEmpty()) Empty("Долгов нет.")
                debts.forEach { x -> ListRow({ openStudent(x.id) }) { Avatar(x.id, x.fio); Main { Name(x.fio); MetaText("начислено минус оплачено") }; Num(rub(x.owe), colors.bad) } }
            }
        }
        st.admin?.let { a ->
            Sec("По репетиторам · ${monthName(App.currentMonth).lowercase()}")
            Group {
                if (a.perTutor.isEmpty()) Empty("Нет данных.")
                a.perTutor.forEach { x ->
                    ListRow(top = true) {
                        Main {
                            Name(App.tutorName(x.tutor))
                            Meta {
                                MetaText("${x.n} занятий")
                                if (x.np > 0) MetaText("${if (x.n >= x.np) "▲" else "▼"} к прошлому ${x.np}", if (x.n >= x.np) colors.ok else colors.bad)
                                MetaText("· ${x.slots} слотов в неделю")
                            }
                        }
                        Right {
                            Num(rub(x.billed)); Num("получено ${rub(x.received)}", colors.hint)
                            if (x.billed - x.received > 0) Num("долг ${rub(x.billed - x.received)}", colors.bad)
                        }
                    }
                }
            }
            Sec("Прогноз на ${monthName(App.currentMonth).lowercase()}")
            Card {
                Text("${a.forecastLo}–${a.forecastHi} тыс ₽", style = mono.copy(fontSize = 22.sp))
                Gap(6.dp)
                MetaText("Уже начислено ${rub(a.forecastDone)} + оставшиеся слоты расписания за вычетом обычных 7–18% пропусков.")
            }
            Sec("Ученики: приход и уход")
            Card {
                Legend { Swatch(null, "занимаются ${a.active}"); Swatch(null, "пауза ${a.paused}"); Swatch(null, "ушли ${a.left}") }
                Gap(8.dp)
                MetaText("Причины ухода")
                if (a.reasons.isEmpty()) Text("—")
                a.reasons.forEach { (r, c) -> Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(r, Modifier.weight(1f), fontSize = 13.sp); Num(c.toString()) } }
            }
        }
    }
}

@Composable
private fun BarChart(data: List<MonthAggDto>) {
    val c = colors
    val tm = rememberTextMeasurer()
    val label = TextStyle(fontSize = 10.sp, color = c.hint)
    val max = (kotlin.math.ceil(maxOf(1.0, data.maxOf { maxOf(it.billed, it.received.toDouble()) }) / 10000) * 10000).coerceAtLeast(10000.0)
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val padL = 34.dp.toPx(); val padB = 20.dp.toPx(); val padT = 8.dp.toPx()
        val w = size.width; val h = size.height
        val bw = (w - padL - 6) / data.size
        fun y(v: Double) = (padT + (h - padT - padB) * (1 - v / max)).toFloat()
        var t = 0.0
        while (t <= max) {
            drawLine(c.line, Offset(padL, y(t)), Offset(w, y(t)), 1f)
            val tl = tm.measure("${(t / 1000).roundToInt()}к", label)
            drawText(tl, topLeft = Offset(padL - 6 - tl.size.width, y(t) - tl.size.height / 2))
            t += max / 2
        }
        data.forEachIndexed { i, x ->
            val cx = padL + i * bw + bw / 2; val bwid = minOf(14.dp.toPx(), bw / 3); val op = if (x.partial) .55f else 1f
            drawRoundRect(c.accent, Offset(cx - bwid - 1, y(x.billed)), Size(bwid, y(0.0) - y(x.billed)), CornerRadius(2f), alpha = op)
            drawRoundRect(c.ok, Offset(cx + 1, y(x.received.toDouble())), Size(bwid, y(0.0) - y(x.received.toDouble())), CornerRadius(2f), alpha = op)
            val tl = tm.measure(MS[x.m.substring(5, 7).toInt() - 1] + if (x.partial) "*" else "", label)
            drawText(tl, topLeft = Offset(cx - tl.size.width / 2, h - tl.size.height))
        }
    }
    Legend {
        Swatch(c.accent, "начислено"); Swatch(c.ok, "получено"); Swatch(null, "* текущий месяц, неполный")
    }
}

@Composable
private fun WeekChart(load: List<Int>) {
    val c = colors
    val tm = rememberTextMeasurer()
    val wmax = maxOf(1, load.maxOrNull() ?: 1)
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        val slot = size.width / 7
        val bw = minOf(26.dp.toPx(), slot * .6f)
        val base = size.height - 18.dp.toPx(); val top = 16.dp.toPx()
        WD.forEachIndexed { i, d ->
            val hh = load[i] * (base - top) / wmax; val cx = slot * i + slot / 2
            drawRoundRect(c.accent, Offset(cx - bw / 2, base - hh), Size(bw, hh), CornerRadius(3.dp.toPx()), alpha = if (load[i] == wmax) 1f else .5f)
            val dl = tm.measure(d, TextStyle(fontSize = 10.sp, color = c.hint))
            drawText(dl, topLeft = Offset(cx - dl.size.width / 2, size.height - dl.size.height))
            if (load[i] > 0) {
                val nl = tm.measure(load[i].toString(), TextStyle(fontSize = 10.sp, color = c.fg, fontWeight = FontWeight.Bold))
                drawText(nl, topLeft = Offset(cx - nl.size.width / 2, base - hh - nl.size.height - 2))
            }
        }
    }
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

/** A permission name in code type. */
@Composable
fun PermChip(text: String, new: Boolean = false) =
    Text(text, Modifier.clip(RoundedCornerShape(5.dp)).background(if (new) colors.accentBg else colors.bg2).padding(horizontal = 5.dp, vertical = 1.dp),
        style = TextStyle(fontFamily = LocalMono.current, fontSize = 10.sp), color = if (new) colors.accent else colors.hint)

@Composable
fun PermChips(list: List<String>) = Meta(top = 5.dp) {
    if (list.isEmpty()) PermChip("нет прав — нет доступа")
    list.forEach { p -> PermChip(p, PERMS.find { it.k == p }?.isNew == true) }
}

@Composable
fun TutorsScreen() {
    Title("Команда")
    Show(remote("tutors") { Api.get<List<TutorDto>>("/api/tutors") }) { list ->
        fun staff(t: TutorDto) = t.subjects.isEmpty() && "ADD_TIME" !in t.perms
        listOf("Преподаватели" to list.filter { it.perms.isNotEmpty() && !staff(it) }, "Руководство" to list.filter { it.perms.isNotEmpty() && staff(it) }, "Отключены" to list.filter { it.perms.isEmpty() })
            .filter { it.second.isNotEmpty() }.forEach { (title, l) ->
                Sec(title)
                Group {
                    l.forEach { t ->
                        ListRow({ openTutor(t) }, top = true) {
                            Avatar(t.id, t.name)
                            Main {
                                Name(t.name + if (t.id == App.me) " (вы)" else "")
                                Meta {
                                    MetaText((if (t.subjects.isNotEmpty()) t.subjects.joinToString(", ") { App.subj(it).name } else "без предметов") + " ·")
                                    UserLink(t.id, "Telegram")
                                    t.rate?.let { MetaText("· ставка $it%") }
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
    Actions(0.dp) {
        Btn("Выгрузить .csv") { exportNow(ExportInput("audit", App.currentMonth, filter = AuditView.type)) }
        Btn("Оповещения") { openAlerts() }
    }
    Gap(10.dp)
    val rec = AuditView.record
    if (rec != null) Card(accent = colors.accent) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("${ETYPES[rec.first] ?: rec.first}: ${rec.third}", Modifier.weight(1f), fontWeight = FontWeight.Bold); Btn("Все записи", "iconbtn") { AuditView.record = null } }
    }
    else {
        Seg(listOf("all" to "Все", "lesson" to "Занятия", "payment" to "Оплаты", "student" to "Ученики", "slot" to "График", "tutor" to "Команда", "report" to "Отчёты"), AuditView.type) { AuditView.type = it; AuditView.limit = 25 }
        TextField(AuditView.query, "Ученик, преподаватель или действие", search = true) { AuditView.query = it; AuditView.limit = 25 }
    }
    val url = buildString {
        append("/api/audit?limit=${AuditView.limit}")
        if (rec != null) append("&rtype=${rec.first}&rid=${rec.second}")
        else { append("&type=${AuditView.type}"); if (AuditView.query.isNotBlank()) append("&q=" + AuditView.query.encodeURLParameter()) }
    }
    Show(remote(url) { Api.get<AuditPageDto>(url) }) { page ->
        Sec("${page.total} ${if (page.total == 1) "запись" else "записей"}")
        Group {
            if (page.items.isEmpty()) Empty("Ничего не найдено.")
            page.items.forEach { e ->
                val ch = auditChanges(e)
                ListRow({ openAudit(e) }, top = true) {
                    TimeCol(shortDay(e.at.substring(0, 10)) + ", " + e.at.substring(11, 16), 62.dp, small = true)
                    Main {
                        if (e.type == "session" && (e.action == "Вход" || e.action == "Нет доступа")) {
                            Name(if (e.action == "Вход") "Вход в приложение" else "Отказ во входе")
                            MetaText(e.title.ifEmpty { e.actorName })
                            MetaText(ch.find { it.first == "устройство" }?.third ?: "")
                        } else {
                            Name("${e.action} · ${(ETYPES[e.type] ?: e.type).lowercase()}")
                            MetaText(e.title)
                            MetaText(e.actorName + if (ch.isNotEmpty()) " · изменений: ${ch.size}" else "")
                        }
                    }
                    Chip(e.source, "src")
                }
            }
        }
        if (page.total > page.items.size) Actions { Btn("Показать ещё") { AuditView.limit += 25 } }
        else if (page.total > 25) Empty("Это всё: начало журнала.")
        Lock("Журнал хранится в базе (таблица audit_log): его можно искать, фильтровать и выгружать. Записи пишут и веб, и бот (метка WEB / BOT); изменить или удалить запись нельзя.")
    }
}
