package me.centralhardware.znatoki.telegram.statistic.app

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** Who the record is for, when the caller may enter lessons or payments for other tutors. */
@Composable
fun TutorPick(forOthers: Boolean, value: Long, onPick: (Long) -> Unit) {
    if (!forOthers) return
    Sec("Репетитор", top = 0.dp)
    Opts { App.teaching().forEach { t -> Opt(t.name + if (t.id == App.me) " (вы)" else "", t.id == value) { onPick(t.id) } } }
    Gap(10.dp)
}

@Composable
fun HistoryButton(type: String, id: String, title: String) {
    if (!App.caps.admin) return
    Actions { Btn("История изменений") { AuditView.showRecord(type, id, title) } }
}

/** Fields stacked with a small gap (the mockup's `display:grid;gap:8px`). */
@Composable
fun Fields(top: Dp = 0.dp, content: @Composable () -> Unit) =
    Column(Modifier.fillMaxWidth().padding(top = top), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }

@Composable
fun SheetKicker(text: String) = Sec(text, top = 0.dp)

// ---------- Занятие ----------
fun editDenied(month: String, tutor: Long, hasPerm: Boolean): String? = when {
    App.caps.admin -> null
    App.caps.director -> "Директор только просматривает данные."
    month != App.currentMonth -> "Месяц закрыт, запись изменить нельзя. Это может сделать админ."
    !(hasPerm && tutor == App.me) -> "Изменить запись может только её репетитор или админ."
    else -> null
}

fun openLesson(id: String) = Sheets.open { close -> LessonSheet(id, close) }

@Composable
private fun LessonSheet(id: String, close: () -> Unit) {
    var edit by remember { mutableStateOf(false) }
    Show(remote(id, "lesson") { Api.get<LessonDto>("/api/lessons/$id") }) { l ->
        val why = editDenied(l.date.substring(0, 7), l.tutor, App.caps.time)
        val title = names(l) + " · " + App.subj(l.subject).name
        fun patch(p: LessonPatch, msg: String) = App.act(msg) { Api.post<LessonDto, LessonPatch>("/api/lessons/$id", p, HttpMethod.Patch) }
        SheetKicker("${dayMonth(l.date)} · ${l.time}")
        Title(names(l))
        if (edit) LessonEdit(l, onCancel = { edit = false }) { p -> patch(p, "Занятие изменено"); edit = false }
        else {
            Group {
                SummaryRow("Репетитор", App.tutorName(l.tutor))
                SummaryRow("Предмет", App.subj(l.subject).name)
                SummaryRow("Тип", if (isGroup(l)) "Групповое" else "Индивидуальное")
                if (App.money) SummaryRow("Цена за ученика", rub(price(l)) + if (l.half) " (×1,5)" else "", true)
                else if (l.half) SummaryRow("Длительность", "полтора часа")
                if (l.flags.isNotEmpty()) SummaryRow("Проверить", l.flags.joinToString(", "))
            }
            if (l.hasPhoto) Thumb("lesson", l.id, "Фотоотчёт")
            if (why != null) Lock(why) else if (l.date.substring(0, 7) != App.currentMonth) Lock("Закрытый месяц: вы правите как админ.")
            val single = l.students.size == 1
            if (App.caps.extraHalfHour || (single && App.caps.forceGroup)) {
                Gap(12.dp)
                Group {
                    if (App.caps.extraHalfHour) Toggle("Полтора часа", "стоимость ×1,5", l.half, why == null && !l.deleted) { patch(LessonPatch(half = it), if (it) "Теперь полтора часа" else "Снова один час") }
                    if (single && App.caps.forceGroup) Toggle("Групповое", "один ученик, но занятие групповое", l.forceGroup, why == null && !l.deleted) { patch(LessonPatch(forceGroup = it), if (it) "Отмечено как групповое" else "Отмечено как индивидуальное") }
                }
            }
            if (App.caps.time && !l.deleted && (l.tutor == App.me || App.caps.admin)) Actions {
                Btn("Повторить сегодня") { close(); openAddLesson(LessonPrefill(l.subject, l.students.map { it.id }, l.amount, l.tutor, from = dayMonth(l.date))) }
            }
            Actions {
                if (App.caps.admin) Btn("Изменить", enabled = !l.deleted) { edit = true }
                Btn(if (l.deleted) "Восстановить" else "Удалить занятие", if (l.deleted) "primary" else "danger", enabled = why == null) {
                    patch(LessonPatch(deleted = !l.deleted), if (l.deleted) "Занятие восстановлено" else "Занятие удалено"); close()
                }
            }
            HistoryButton("lesson", l.id, title)
        }
    }
}

@Composable
private fun LessonEdit(l: LessonDto, onCancel: () -> Unit, onSave: (LessonPatch) -> Unit) {
    var amount by remember { mutableStateOf<Int?>(l.amount) }
    var subject by remember { mutableStateOf(l.subject) }
    var date by remember { mutableStateOf(l.date) }
    var time by remember { mutableStateOf(l.time) }
    var tutor by remember { mutableStateOf(l.tutor) }
    Fields {
        FieldLabel("Стоимость за ученика, ₽"); NumberField(amount) { amount = it }
        FieldLabel("Предмет"); Dropdown(App.b.subjects.map { it.id to it.name }, subject) { subject = it }
        FieldLabel("Дата"); DateField(date) { date = it }
        FieldLabel("Время"); TimeField(time) { time = it }
        FieldLabel("Репетитор"); Dropdown(App.teaching().map { it.id to it.name }, tutor) { tutor = it }
    }
    Lock(if ((amount ?: 0) <= 0) "Стоимость должна быть больше нуля." else if (l.date.substring(0, 7) != App.currentMonth) "Правка закрытого месяца попадёт в журнал изменений и в уже принятый отчёт." else "")
    Actions {
        Btn("Отмена") { onCancel() }
        Btn("Сохранить", "primary", enabled = (amount ?: 0) > 0) { onSave(LessonPatch(amount = amount, subject = subject, tutor = tutor, date = date, time = time)) }
    }
}

fun openBulk(ls: List<LessonDto>, kind: String) = Sheets.open { close ->
    val m = App.month
    val last = if (m == App.currentMonth) App.today.substring(8).toInt() else daysInMonth(m)
    var tutor by remember { mutableStateOf(App.teaching().first().id) }
    var value by remember { mutableStateOf<Int?>(if (kind == "price") ls[0].amount else ls[0].date.substring(8).toInt()) }
    Title(mapOf("tutor" to "Сменить репетитора", "price" to "Изменить стоимость", "day" to "Перенести на другой день")[kind]!!)
    MetaText("Занятий: ${ls.size}. Каждое изменение попадёт в журнал" + if (m != App.currentMonth) " и в отчёт за ${monthName(m).lowercase()}." else ".")
    Fields(8.dp) {
        when (kind) {
            "tutor" -> { FieldLabel("Новый репетитор"); Dropdown(App.teaching().map { it.id to it.name }, tutor) { tutor = it } }
            "price" -> { FieldLabel("Стоимость за ученика, ₽"); NumberField(value) { value = it } }
            else -> { FieldLabel("Новый день, " + MG[m.substring(5).toInt() - 1]); NumberField(value) { value = it } }
        }
    }
    val bad = kind != "tutor" && ((value ?: 0) <= 0 || (kind == "day" && (value ?: 0) > last))
    Lock(if (bad) (if (kind == "day") "День от 1 до $last." else "Стоимость должна быть больше нуля.") else "")
    Actions {
        Btn("Отмена") { close() }
        Btn("Применить", "primary", enabled = !bad) { bulk(ls, kind, if (kind == "tutor") tutor.toString() else value.toString()); close() }
    }
}

// ---------- Новое занятие ----------
@Serializable
data class LessonPrefill(
    val subject: Long? = null,
    val students: List<Int> = emptyList(),
    val amount: Int? = null,
    val tutor: Long? = null,
    val slot: Int? = null,
    val time: String? = null,
    val from: String? = null,
) {
    companion object {
        fun fromSlot(s: SlotDto) = LessonPrefill(s.subject, s.students.map { it.id }, s.amount, s.tutor, s.id, s.time)
    }
}

private const val DRAFT_KEY = "classlog-draft"

fun openAddLesson(pre: LessonPrefill?) = Sheets.open { close -> AddLesson(pre, close) }

/** A pickable student row, ticked when chosen. */
@Composable
private fun PickRow(s: StudentRowDto, picked: Boolean, onClick: () -> Unit) = ListRow(onClick) {
    Avatar(s.id, sFio(s)); Main { Name(sFio(s)); MetaText("${s.cls ?: "—"} класс") }
    if (picked) Chip("✓", "grp")
}

@Composable
private fun AddLesson(pre: LessonPrefill?, close: () -> Unit) {
    val draft = remember {
        if (pre == null) runCatching { platform.load(DRAFT_KEY)?.let { Api.json.decodeFromString<LessonPrefill>(it) } }.getOrNull() else null
    }
    val start = pre ?: draft
    var restored by remember { mutableStateOf(draft != null) }
    var step by remember { mutableStateOf(if (pre?.subject != null && pre.slot == null) 3 else 0) }
    var tutor by remember { mutableStateOf(start?.tutor ?: App.me) }
    var subject by remember { mutableStateOf(start?.subject) }
    var studentsSel by remember { mutableStateOf(start?.students ?: emptyList()) }
    var amount by remember { mutableStateOf(start?.amount ?: 1000) }
    var from by remember { mutableStateOf(start?.from) }
    var photo by remember { mutableStateOf<Photo?>(null) }
    var q by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(App.today) }
    var time by remember { mutableStateOf(pre?.time ?: "") }
    var saving by remember { mutableStateOf(false) }
    val total = 5

    fun saveDraft() {
        if (pre?.slot != null) return
        platform.save(DRAFT_KEY, Api.json.encodeToString(LessonPrefill.serializer(), LessonPrefill(subject, studentsSel, amount, tutor, from = from)))
    }
    fun clearDraft() = platform.save(DRAFT_KEY, null)

    val tutorObj = App.b.tutors.find { it.id == tutor }
    val subjects = App.b.subjects.filter { App.caps.admin || tutorObj == null || tutorObj.subjects.isEmpty() || it.id in tutorObj.subjects }
    Steps(total, step)
    if (restored) Card(Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Черновик восстановлен: продолжите с того места, где остановились.", Modifier.weight(1f), fontSize = 13.sp)
            Btn("Заново", "mini ghost") { clearDraft(); restored = false; subject = null; studentsSel = emptyList(); amount = 1000; from = null; step = 0 }
        }
    }
    if (pre?.slot != null) MetaText("По расписанию: ${WDF[dow(App.today)].lowercase()} ${pre.time}. Всё заполнено из слота — проверьте и нажимайте «Далее».")
    val canNext = when (step) {
        0 -> subject != null
        1 -> studentsSel.isNotEmpty()
        2 -> amount > 0
        3 -> photo != null
        else -> !saving
    }
    when (step) {
        0 -> {
            TutorPick(App.caps.timeForOthers, tutor) { tutor = it; subject = null; studentsSel = emptyList() }
            Title("Какой предмет?")
            Opts { subjects.forEach { s -> Opt(s.name, subject == s.id) { if (subject != s.id) studentsSel = emptyList(); subject = s.id } } }
            val groupNames = subjects.filter { it.group }.joinToString(" и ") { it.name.lowercase() }
            if (groupNames.isNotEmpty()) Lock("В предметах $groupNames можно отметить нескольких учеников на одном занятии.")
        }
        1 -> {
            val sb = App.subj(subject!!)
            Title(if (sb.group) "Кто был на занятии?" else "Какой ученик?")
            TextField(q, "Фильтр по ФИО", search = true) { q = it }
            Show(remote("students") { loadStudents() }) { all ->
                // A restored draft or a repeated lesson can carry a student who is no longer active: drop them, or they
                // count towards "Выбрано" without ever showing as ticked.
                val active = all.filter { !it.deleted && it.status == "active" }
                val kept = studentsSel.distinct().filter { id -> active.any { it.id == id } }
                if (kept != studentsSel) studentsSel = kept
                val list = active.filter { q.isBlank() || sFio(it).lowercase().contains(q.lowercase()) }
                Gap(8.dp)
                Group {
                    if (list.isEmpty()) Empty("Никого не нашлось.")
                    list.forEach { s ->
                        PickRow(s, s.id in studentsSel) {
                            studentsSel = if (sb.group) (if (s.id in studentsSel) studentsSel - s.id else studentsSel + s.id) else listOf(s.id)
                        }
                    }
                }
            }
            Lock(if (sb.group) "Выбрано: ${studentsSel.size}. Двое и больше — групповое занятие." else "${sb.name} только индивидуально.")
        }
        2 -> {
            Title("Стоимость за ученика")
            NumberField(amount) { amount = it ?: 0 }
            Opts(8.dp) { listOf(800, 900, 1000, 1200, 1400).forEach { v -> Opt(rub(v), amount == v) { amount = v } } }
            Lock(if (amount <= 0) "Сумма должна быть больше нуля." else from?.let { "Как на прошлом занятии $it." } ?: "")
        }
        3 -> {
            if (pre?.from != null) MetaText("Повтор занятия от ${pre.from}. Можно вернуться и поменять.")
            Title("Фотоотчёт")
            PhotoBox(photo, "Нажмите, чтобы приложить фото отчётности") { photo = it }
            Lock("Откроется камера или галерея.")
        }
        else -> {
            val grp = studentsSel.size > 1
            Title("Сохранить занятие?")
            Show(remote("students") { loadStudents() }) { all ->
                Group {
                    if (tutor != App.me) LabelRow("Репетитор") { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text(App.tutorName(tutor)); Chip("внесено вами") } }
                    SummaryRow("Предмет", App.subj(subject!!).name)
                    SummaryRow("Ученики", studentsSel.mapNotNull { id -> all.find { it.id == id }?.let { sFio(it) } }.joinToString(", "))
                    SummaryRow("Тип", if (grp) "Групповое" else "Индивидуальное")
                    SummaryRow("Стоимость", rub(amount) + if (grp) " × ${studentsSel.size}" else "", true)
                    LabelRow("Фото") { Chip("приложено", "ok") }
                }
            }
            if (App.caps.admin && pre?.slot == null) {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) { FieldLabel("Дата"); DateField(date) { date = it } }
                    Column(Modifier.weight(1f)) { FieldLabel("Время"); TimeField(time) { time = it } }
                }
                Lock("Админ может внести занятие задним числом, в том числе в закрытый месяц.")
            }
            Lock("После сохранения в лог-чат уйдёт обычное сообщение #занятие с фото.")
        }
    }
    LaunchedEffect(subject, studentsSel, amount, tutor, from, step) { if (subject != null && step < 4) saveDraft() }
    Actions {
        Btn(if (step > 0) "Назад" else "Отмена") { if (step > 0) step-- else { clearDraft(); close() } }
        Btn(if (step == 4) (if (saving) "Сохраняем…" else "Сохранить") else "Далее", "primary", enabled = canNext) {
            if (step == 1 && pre?.slot == null) {
                App.scope.launch {
                    runCatching { Api.get<LastPriceDto>("/api/lessons/last-price?student=${studentsSel[0]}&subject=$subject&tutor=$tutor") }.getOrNull()
                        ?.let { lp -> lp.amount?.toIntOrNull()?.let { amount = it; from = lp.date?.let { d -> dayMonth(d) } } }
                    step++
                }
            } else if (step < 4) step++
            else {
                saving = true
                val input = LessonInput(
                    tutor = tutor, subject = subject!!, students = studentsSel, amount = amount, slot = pre?.slot,
                    date = if (App.caps.admin && pre?.slot == null && date != App.today) date else null,
                    time = if (App.caps.admin && pre?.slot == null && time.isNotBlank()) time else null,
                )
                App.scope.launch {
                    try {
                        Api.postForm<LessonDto>("/api/lessons", Api.json.encodeToString(LessonInput.serializer(), input), photo!!.bytes)
                        clearDraft(); close(); App.refresh()
                        Toasts.show(if (tutor == App.me) "Занятие сохранено" else "Занятие сохранено за: ${App.tutorName(tutor)}")
                    } catch (e: Throwable) { Toasts.show(e.message ?: "Ошибка") } finally { saving = false }
                }
            }
        }
    }
}

// ---------- Расписание ----------
fun openAddSlot() = Sheets.open { close ->
    var weekly by remember { mutableStateOf(true) }
    var weekday by remember { mutableStateOf(dow(App.today)) }
    var date by remember { mutableStateOf(App.today) }
    var time by remember { mutableStateOf("16:00") }
    var subject by remember { mutableStateOf(App.b.subjects.firstOrNull()?.id) }
    var student by remember { mutableStateOf<Int?>(null) }
    var amount by remember { mutableStateOf<Int?>(1000) }
    Title("Новое занятие в расписании")
    Seg(listOf(true to "Регулярное", false to "Разовое"), weekly) { weekly = it }
    Fields(10.dp) {
        if (weekly) { FieldLabel("Каждую неделю, день"); Dropdown(WDF.indices.map { it to WDF[it] }, weekday) { weekday = it } }
        else { FieldLabel("Дата"); DateField(date) { date = it } }
        FieldLabel("Время"); TimeField(time) { time = it }
        FieldLabel("Предмет"); Dropdown(App.b.subjects.map { it.id to it.name }, subject) { subject = it }
        FieldLabel("Ученик")
        Show(remote("students") { loadStudents() }) { all ->
            val list = all.filter { !it.deleted && it.status == "active" }
            if (student == null) student = list.firstOrNull()?.id
            Dropdown(list.map { it.id to sFio(it) }, student) { student = it }
        }
        FieldLabel("Стоимость за ученика, ₽"); NumberField(amount) { amount = it }
    }
    Lock(if (weekly) "Будет появляться каждую неделю, пока не удалите." else "Появится один раз, в выбранный день. Например, перенос или пробное занятие.")
    Actions {
        Btn("Отмена") { close() }
        Btn("Добавить", "primary", enabled = (amount ?: 0) > 0 && student != null && subject != null && time.isNotBlank()) {
            App.act(if (weekly) "Регулярное занятие добавлено" else "Разовое занятие добавлено") {
                Api.post<SlotDto, SlotInput>("/api/schedule/slots", SlotInput(if (weekly) weekday else null, if (weekly) null else date, time, subject!!, listOf(student!!), amount!!))
                close()
            }
        }
    }
}

fun openReminders(r: RemindersDto) = Sheets.open { close ->
    var evening by remember { mutableStateOf(r.evening) }
    var at by remember { mutableStateOf(r.at) }
    var monthEnd by remember { mutableStateOf(r.monthEnd) }
    Title("Напоминания в боте")
    Group {
        Toggle("Неотмеченные занятия", "Вечером, если по расписанию что-то не отмечено", evening) { evening = it }
        ListRow { Main { Name("Время") }; Dropdown(listOf("20:00", "21:00", "22:00").map { it to it }, at, modifier = Modifier.width(130.dp)) { at = it } }
        Toggle("Конец месяца", "За 2 дня: проверить занятия и сдать отчёт", monthEnd) { monthEnd = it }
    }
    Lock("Сообщение придёт в чат с ботом, кнопка в нём открывает приложение. Если всё отмечено, вечернего сообщения не будет.")
    Actions { Btn("Готово", "primary") { App.act("Напоминания сохранены") { Api.post<RemindersDto, RemindersDto>("/api/reminders", RemindersDto(evening, at, monthEnd), HttpMethod.Put); close() } } }
}

// ---------- Ученик ----------
fun openStudent(id: Int) = Sheets.open { close -> StudentSheet(id, close) }

@Composable
private fun StudentSheet(id: Int, close: () -> Unit) {
    var mode by remember { mutableStateOf("view") }
    var note by remember { mutableStateOf("") }
    Show(remote(id, "student") { Api.get<StudentDetailDto>("/api/students/$id") }) { d ->
        val s = d.row
        when (mode) {
            "status" -> StatusEditor(s) { mode = "view" }
            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(s.id, sFio(s))
                    Column(Modifier.weight(1f)) {
                        Text(listOf(s.last, s.first, s.middle).filter { it.isNotBlank() }.joinToString(" "), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                        MetaText("ID ${s.id} · ${s.cls ?: "—"} класс · " + s.subjects.joinToString(", ") { App.subj(it).name }.ifEmpty { "занятий ещё не было" })
                    }
                }
                Sec("Статус")
                Group {
                    ListRow {
                        Main {
                            Text(STATUS[s.status] ?: s.status)
                            if (s.status != "active") MetaText("${s.reason ?: ""}" + (s.since?.let { " · с ${MG[it.substring(5, 7).toInt() - 1]}" } ?: ""))
                        }
                        if (App.caps.client && !s.deleted) Btn("Изменить", "mini ghost") { mode = "status" }
                    }
                }
                s.balance?.let { b ->
                    Sec("Баланс за всё время")
                    Group { ListRow { Main { Text(if (b < 0) "Долг" else if (b > 0) "Предоплата" else "Расчёты сведены") }; Num(rub(kotlin.math.abs(b)), if (b < 0) colors.bad else if (b > 0) colors.ok else colors.fg) } }
                }
                Sec("Контакты")
                Group {
                    listOf("Ученик" to d.phone, "Родитель" to d.parent).forEach { (t, ph) ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
                            Row { Text(t, Modifier.weight(1f)); Num(fmtPhone(ph)) }
                            if (!ph.isNullOrEmpty()) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Позвонить" to "tel:+$ph", "Telegram" to "https://t.me/+$ph", "WhatsApp" to "https://wa.me/$ph").forEachIndexed { i, (label, href) ->
                                    Btn(label, if (i > 0) "mini ghost" else "mini") { platform.openUrl(href) }
                                }
                            }
                        }
                    }
                    d.motherFio?.takeIf { it.isNotBlank() }?.let { SummaryRow("ФИО матери", it) }
                    SummaryRow("Источник", s.source ?: "—")
                }
                Sec("Заметки")
                Group {
                    if (d.notes.isEmpty()) ListRow { MetaText("Заметок пока нет") }
                    d.notes.forEach { n ->
                        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
                            Text(n.text, lineHeight = 20.sp)
                            Text(dateTime(n.at) + " · " + n.by, Modifier.padding(top = 3.dp), color = colors.hint, fontSize = 11.sp)
                        }
                    }
                    if ((App.caps.time || App.caps.client) && !App.caps.director) Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp)) {
                        Area(note, "Что прошли, домашнее задание, на что обратить внимание") { note = it }
                        Actions(6.dp) {
                            Btn("Добавить заметку", enabled = note.isNotBlank()) { App.act("Заметка сохранена") { Api.post<List<NoteDto>, TextInput>("/api/students/$id/notes", TextInput(note)); note = "" } }
                        }
                    }
                }
                Sec("История")
                Group {
                    SummaryRow("Занятий", d.lessons.toString(), true)
                    d.paid?.let { SummaryRow("Оплачено всего", rub(it), true) }
                    d.recent.forEach { r -> ListRow { Main { MetaText(dayMonth(r.date) + " · " + App.subj(r.subject).name) }; if (r.group) Chip("группа", "grp") else Chip("индивид.") } }
                }
                if (s.deleted) {
                    Lock("Ученик удалён: не показывается в поиске и формах, прошлые занятия остаются в отчётах.")
                    if (App.caps.admin) Actions { Btn("Восстановить ученика", "primary") { App.act("Ученик восстановлен") { Api.postEmpty<StudentRowDto>("/api/students/$id/restore"); close() } } }
                } else {
                    Actions {
                        if (App.caps.client) Btn("Изменить данные") { close(); openStudentForm(d) }
                        if (App.caps.admin) Btn("Удалить ученика", "danger") {
                            App.act {
                                Api.postEmpty<StudentRowDto>("/api/students/$id/delete"); close()
                                Toasts.withUndo("Ученик удалён") { App.act("Удаление отменено") { Api.postEmpty<StudentRowDto>("/api/students/$id/restore") } }
                            }
                        }
                    }
                    if (!App.caps.admin) Lock(if (App.caps.client) "Удалить ученика может только админ." else "Изменять учеников могут те, у кого есть право ADD_CLIENT.")
                }
                HistoryButton("student", s.id.toString(), sFio(s))
            }
        }
    }
}

@Composable
private fun StatusEditor(s: StudentRowDto, done: () -> Unit) {
    var st by remember { mutableStateOf(s.status) }
    var reason by remember { mutableStateOf(s.reason ?: App.b.reasons.first()) }
    Title("Статус: ${sFio(s)}")
    Opts { STATUS.forEach { (k, t) -> Opt(t, st == k) { st = k } } }
    Gap(12.dp)
    FieldLabel("Причина (для паузы и ухода)")
    Dropdown(App.b.reasons.map { it to it }, reason) { reason = it }
    Lock("Ушедшие и на паузе не предлагаются в форме занятия. Причины ухода видны в статистике.")
    Actions {
        Btn("Отмена") { done() }
        Btn("Сохранить", "primary") { App.act("Статус: ${STATUS[st]!!.lowercase()}") { Api.post<StudentRowDto, StatusInput>("/api/students/${s.id}/status", StatusInput(st, reason)); done() } }
    }
}

fun openStudentForm(d: StudentDetailDto?) = Sheets.open { close ->
    val s = d?.row
    var last by remember { mutableStateOf(s?.last ?: "") }
    var first by remember { mutableStateOf(s?.first ?: "") }
    var middle by remember { mutableStateOf(s?.middle ?: "") }
    var cls by remember { mutableStateOf(s?.cls) }
    var phone by remember { mutableStateOf(d?.phone ?: "") }
    var parent by remember { mutableStateOf(d?.parent ?: "") }
    var source by remember { mutableStateOf(s?.source ?: "") }
    fun digits(raw: String): String { var x = raw.filter { it.isDigit() }; if (x.startsWith("8")) x = "7" + x.drop(1) else if (x.startsWith("9")) x = "7$x"; return x.take(11) }
    fun phoneErr(raw: String) = digits(raw).let { if (it.isEmpty() || it.length == 11) null else "Номер неполный: нужно 10 цифр после +7." }
    Title(if (s == null) "Новый ученик" else "Данные ученика")
    Fields {
        TextField(last, "Фамилия") { last = it }
        TextField(first, "Имя") { first = it }
        TextField(middle, "Отчество (необязательно)") { middle = it }
    }
    Sec("Класс")
    Opts { (1..11).forEach { i -> Opt(i.toString(), cls == i, modifier = Modifier.widthIn(min = 42.dp)) { cls = i } } }
    Sec("Телефоны (необязательно)")
    Fields {
        FieldLabel("Ученик"); TextField(phone, "+7 (913) 123-45-67", KeyboardType.Phone, num = true, isError = phoneErr(phone) != null) { phone = it }
        phoneErr(phone)?.let { MetaText(it, colors.bad) }
        FieldLabel("Родитель"); TextField(parent, "+7 (923) 123-45-67", KeyboardType.Phone, num = true, isError = phoneErr(parent) != null) { parent = it }
        phoneErr(parent)?.let { MetaText(it, colors.bad) }
    }
    Sec("Откуда узнали? (необязательно)")
    TextField(source, "Например: от знакомых, 2gis, вывеска") { source = it }
    val sugg = App.b.sources.filter { source.isBlank() || (it.lowercase().contains(source.lowercase()) && !it.equals(source, true)) }.take(6)
    Opts(6.dp) {
        sugg.forEach { v -> Opt(v, false) { source = v } }
        if (source.isNotBlank() && App.b.sources.none { it.equals(source.trim(), true) }) MetaText("Новый вариант — сохранится как есть")
    }
    val err = when {
        last.isBlank() || first.isBlank() -> "Введите фамилию и имя."
        cls == null -> "Выберите класс от 1 до 11."
        phoneErr(phone) != null || phoneErr(parent) != null -> "Проверьте телефоны."
        else -> null
    }
    Lock(err ?: "")
    Actions {
        Btn("Отмена") { close() }
        Btn(if (s == null) "Добавить" else "Сохранить", "primary", enabled = err == null) {
            val input = StudentInput(last, first, middle, cls, digits(phone).ifEmpty { null }, digits(parent).ifEmpty { null }, source.ifBlank { null })
            App.act {
                val r: StudentRowDto = if (s == null) Api.post("/api/students", input) else Api.post("/api/students/${s.id}", input, HttpMethod.Put)
                close(); Toasts.show(if (s == null) "Ученик добавлен, ID ${r.id}" else "Данные ученика сохранены")
            }
        }
    }
}

// ---------- Оплата ----------
fun openPayment(id: Int) = Sheets.open { close ->
    var edit by remember { mutableStateOf(false) }
    Show(remote(id, "payment") { Api.get<PaymentDto>("/api/payments/$id") }) { p ->
        val why = editDenied(p.date.substring(0, 7), p.tutor, App.caps.payment)
        SheetKicker("${dayMonth(p.date)} · №${p.id}")
        Title(p.student.fio)
        if (edit) {
            var amount by remember { mutableStateOf<Int?>(p.amount) }
            var subject by remember { mutableStateOf(p.subject) }
            var tutor by remember { mutableStateOf(p.tutor) }
            Fields {
                FieldLabel("Сумма, ₽"); NumberField(amount) { amount = it }
                FieldLabel("Предмет"); Dropdown(App.b.subjects.map { it.id to it.name }, subject) { subject = it }
                FieldLabel("Оплату принял"); Dropdown(App.teaching().map { it.id to it.name }, tutor, enabled = App.caps.admin) { tutor = it }
            }
            Lock(if ((amount ?: 0) <= 0) "Сумма должна быть больше нуля." else "")
            Actions {
                Btn("Отмена") { edit = false }
                Btn("Сохранить", "primary", enabled = (amount ?: 0) > 0) {
                    App.act("Оплата изменена") { Api.post<PaymentDto, PaymentPatch>("/api/payments/$id", PaymentPatch(amount, subject, tutor.takeIf { App.caps.admin }), HttpMethod.Patch); close() }
                }
            }
        } else {
            Group {
                if (App.money) SummaryRow("Сумма", rub(p.amount), true)
                SummaryRow("Предмет", App.subj(p.subject).name)
                SummaryRow("Оплату принял", App.tutorName(p.tutor))
                LabelRow("Чек") { if (p.hasPhoto) Chip("приложен", "ok") else Chip("нет", "half") }
            }
            if (p.hasPhoto) Thumb("payment", p.id.toString(), "Чек об оплате")
            if (why != null) Lock(why) else if (p.date.substring(0, 7) != App.currentMonth) Lock("Закрытый месяц: правка админа попадёт в журнал изменений.")
            Actions {
                Btn("Изменить", enabled = why == null && !p.deleted) { edit = true }
                Btn(if (p.deleted) "Восстановить" else "Удалить", if (p.deleted) "primary" else "danger", enabled = why == null) {
                    App.act(if (p.deleted) "Оплата восстановлена" else "Оплата удалена") { Api.post<PaymentDto, PaymentPatch>("/api/payments/$id", PaymentPatch(deleted = !p.deleted), HttpMethod.Patch); close() }
                }
            }
            HistoryButton("payment", p.id.toString(), p.student.fio + " · " + rub(p.amount))
        }
    }
}

fun openAddPayment() = Sheets.open { close ->
    var step by remember { mutableStateOf(0) }
    var tutor by remember { mutableStateOf(App.me) }
    var student by remember { mutableStateOf<Int?>(null) }
    var subject by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf(0) }
    var photo by remember { mutableStateOf<Photo?>(null) }
    var q by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    Steps(4, step)
    val students = remote("students") { loadStudents() }
    val fio = { id: Int? -> (students as? Remote.Ok)?.data?.find { it.id == id }?.let { sFio(it) } ?: "" }
    when (step) {
        0 -> {
            TutorPick(App.caps.paymentForOthers, tutor) { tutor = it }
            Title("Кто оплатил?")
            TextField(q, "Фильтр по ФИО", search = true) { q = it }
            Show(students) { all ->
                Gap(8.dp)
                Group {
                    all.filter { !it.deleted && (q.isBlank() || sFio(it).lowercase().contains(q.lowercase())) }.forEach { s ->
                        PickRow(s, student == s.id) { student = s.id }
                    }
                }
            }
        }
        1 -> {
            Title("За какой предмет?")
            Opts { App.b.subjects.forEach { s -> Opt(s.name, subject == s.id) { subject = s.id } } }
            Title("Сумма", Modifier.padding(top = 16.dp))
            NumberField(amount.takeIf { it > 0 }) { amount = it ?: 0 }
            Lock(if (amount <= 0) "Сумма должна быть больше нуля." else "")
        }
        2 -> {
            Title("Чек")
            PhotoBox(photo, "Нажмите, чтобы приложить скриншот перевода") { photo = it }
            Lock("Оплата без чека не попадает в сумму «оплачено» в отчёте.")
        }
        else -> {
            Title("Сохранить оплату?")
            Group {
                if (tutor != App.me) SummaryRow("Оплату принял", App.tutorName(tutor))
                SummaryRow("Ученик", fio(student))
                SummaryRow("Предмет", App.subj(subject!!).name)
                SummaryRow("Сумма", rub(amount), true)
                LabelRow("Чек") { Chip("приложен", "ok") }
            }
            Lock("После сохранения в лог-чат уйдёт сообщение #оплата, а при включённой интеграции оплата выгрузится в Firefly.")
        }
    }
    val canNext = when (step) { 0 -> student != null; 1 -> subject != null && amount > 0; 2 -> photo != null; else -> !saving }
    Actions {
        Btn(if (step > 0) "Назад" else "Отмена") { if (step > 0) step-- else close() }
        Btn(if (step == 3) (if (saving) "Сохраняем…" else "Сохранить") else "Далее", "primary", enabled = canNext) {
            if (step < 3) step++ else {
                saving = true
                App.scope.launch {
                    try {
                        Api.postForm<PaymentDto>("/api/payments", Api.json.encodeToString(PaymentInput.serializer(), PaymentInput(tutor, student!!, subject!!, amount)), photo!!.bytes)
                        App.month = App.currentMonth; close(); App.refresh(); Toasts.show("Оплата сохранена")
                    } catch (e: Throwable) { Toasts.show(e.message ?: "Ошибка") } finally { saving = false }
                }
            }
        }
    }
}

// ---------- Отчёты ----------
fun exportNow(input: ExportInput) = App.act {
    if (input.to == "chat") {
        val r: SentDto = Api.post("/api/report/export", input)
        Toasts.show("Бот прислал в чат файлов: ${r.sent}")
    } else download(Api.post("/api/report/export", input))
}

fun openExport(subject: Long?) = Sheets.open { close ->
    var kind by remember { mutableStateOf("month") }
    var month by remember { mutableStateOf(if (App.seesAll) App.month else App.currentMonth) }
    var year by remember { mutableStateOf(month.substring(0, 4).toInt()) }
    var subjects by remember { mutableStateOf(subject?.let { listOf(it) } ?: App.b.subjects.map { it.id }) }
    var tutor by remember { mutableStateOf("me") }
    var to by remember { mutableStateOf("chat") }
    Title("Выгрузка отчёта")
    Sec("Что выгрузить", top = 0.dp)
    Opts {
        Opt("Отчёт по оплате и посещаемости", kind == "month") { kind = "month" }
        if (App.money && App.caps.readPayments) Opt("Список оплат", kind == "payments") { kind = "payments" }
    }
    Sec("Месяц")
    if (App.seesAll) {
        val first = App.b.months.first().substring(0, 4).toInt(); val last = App.currentMonth.substring(0, 4).toInt()
        Stepper(year.toString(), year > first, year < last, true, { year-- }, { year++ })
        MS.chunked(4).forEachIndexed { r, row ->
            EqualRow(Modifier.padding(top = 6.dp), 6.dp) {
                row.forEachIndexed { j, n ->
                    val m = "$year-" + (r * 4 + j + 1).toString().padStart(2, '0')
                    Opt(n, month == m, enabled = m in App.b.months) { month = m }
                }
            }
        }
        Lock("Выбрано: ${monthName(month)}${if (month == App.currentMonth) " (текущий)" else ""}. Доступен любой месяц с начала учёта, как /reportmonth в боте.")
    } else Lock("${monthName(App.currentMonth)} (текущий). Прошлые месяцы выгружает администратор.")
    if (kind == "month") {
        Sec("Предметы")
        Opts { App.b.subjects.forEach { s -> Opt(s.name, s.id in subjects) { subjects = if (s.id in subjects) subjects - s.id else subjects + s.id } } }
        if (App.caps.admin) { Sec("Репетиторы"); Opts { Opt("Только я", tutor == "me") { tutor = "me" }; Opt("Все репетиторы", tutor == "all") { tutor = "all" } } }
    }
    Sec("Куда")
    Opts { Opt("Прислать в чат с ботом", to == "chat") { to = "chat" }; Opt("Скачать на устройство", to == "file") { to = "file" } }
    if (kind == "month" && subjects.isNotEmpty()) Actions { Btn("Предпросмотр") { openPreview(App.me, month, subjects) } }
    Lock((if (to == "chat") "Бот пришлёт файлы сообщением, как сейчас по /report — их удобно переслать." else "Несколько файлов скачаются одним архивом .zip.") +
        if (month == App.currentMonth) " Текущий месяц ещё не закрыт, цифры могут измениться." else "")
    Actions {
        Btn("Отмена") { close() }
        Btn("Выгрузить", "primary", enabled = kind != "month" || subjects.isNotEmpty()) { exportNow(ExportInput(kind, month, subjects, tutor, to)); close() }
    }
}

/** A spreadsheet-like table: fixed column widths, scrolled sideways. */
@Composable
private fun Sheet(widths: List<Int>, rows: List<Pair<String, List<String>>>) {
    val c = colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.bg).horizontalScroll(rememberScrollState())) {
        rows.forEach { (kind, cells) ->
            Row(Modifier.background(when (kind) { "th" -> c.bg2; "tot" -> c.accentBg; else -> c.bg })) {
                if (kind == "ttl") Text(cells[0], Modifier.width(widths.sum().dp).padding(horizontal = 8.dp, vertical = 3.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                else cells.forEachIndexed { i, t ->
                    Text(t, Modifier.width(widths[i].dp).padding(horizontal = 8.dp, vertical = 5.dp),
                        style = if (t.isNotEmpty() && t.all { it.isDigit() || it == '—' }) TextStyle(fontFamily = LocalMono.current, fontSize = 12.sp, textAlign = TextAlign.End) else TextStyle(fontSize = 12.sp),
                        fontWeight = if (kind == "th" || kind == "tot") FontWeight.Bold else FontWeight.Normal, color = c.fg)
                }
            }
        }
    }
}

fun openPreview(tutor: Long, month: String, subjects: List<Long>) = Sheets.open { close ->
    var si by remember { mutableStateOf(0) }
    var tab by remember { mutableStateOf("att") }
    val subject = subjects[si]
    Title("Предпросмотр")
    Show(remote(tutor, month, subject, "preview") { Api.get<PreviewDto>("/api/report/preview?tutor=$tutor&month=$month&subject=$subject") }) { x ->
        MetaText(x.name)
        if (subjects.size > 1) Opts(6.dp) { subjects.forEachIndexed { i, id -> Opt(App.subj(id).name, si == i) { si = i } } }
        Seg(listOf("att" to "отчет", "ls" to "журнал занятий") + if (x.pays != null) listOf("pay" to "журнал оплаты") else emptyList(), tab) { tab = it }
        when (tab) {
            "att" -> Sheet(listOf(36, 180, 56, 96, 96, 80, 70, 220),
                listOf(
                    "Отчет по оплате и посещаемости занятий по ${x.title}", "Преподаватель: ${x.tutorName}", x.monthTitle,
                ).map { "ttl" to listOf(it) } +
                    listOf("th" to listOf("№", "ФИО ученика", "Класс", "посетил индивидуально", "посетил групповые", "оплата", "Итого", "Даты посещений")) +
                    (if (x.att.isEmpty()) listOf("ttl" to listOf("нет занятий")) else emptyList()) +
                    x.att.map { r -> "td" to listOf(r.n.toString(), r.fio, r.cls?.toString() ?: "", r.ind.toString(), r.grp.toString(), r.paid?.toString() ?: "—", "", r.dates) } +
                    listOf("tot" to (listOf("", "Итого", "") + x.attTotals.map { it?.toString() ?: "—" } + listOf("", "")).take(8)))
            "ls" -> Sheet(listOf(110, 200, 80, 120, 100),
                listOf("th" to listOf("Дата", "ученик", "Сумма", "Принудительно групповое", "Полтора часа")) +
                    x.log.map { r -> "td" to listOf(r.at, r.fio, r.price.toString(), r.group, r.half) } +
                    listOf("tot" to listOf("итого", "", x.logTotal.toString(), "", "")))
            else -> Sheet(listOf(110, 200, 90),
                listOf("th" to listOf("Дата", "ученик", "Сумма")) +
                    (if (x.pays.isNullOrEmpty()) listOf("ttl" to listOf("нет оплат")) else emptyList()) +
                    (x.pays ?: emptyList()).map { r -> "td" to listOf(r.at, r.fio, r.amount.toString()) } +
                    listOf("tot" to listOf("итого", "", x.payTotal?.toString() ?: "")))
        }
        Lock("Листы и колонки как в файле, который строит /report. Таблицу можно прокрутить вбок." + if (x.pays == null) " Колонка «оплата» и лист оплат скрыты: у вас нет доступа к оплатам." else "")
        val who = if (tutor == App.me) "me" else tutor.toString()
        Actions {
            Btn("Закрыть") { close() }
            if (!App.caps.director) Btn("В чат") { exportNow(ExportInput("month", month, listOf(subject), who, "chat")) }
            Btn("Скачать .xlsx", "primary") { exportNow(ExportInput("month", month, listOf(subject), who, "file")) }
        }
    }
}

fun openSubmit(rep: ReportDto) = Sheets.open { close ->
    var note by remember { mutableStateOf("") }
    Title("Сдать отчёт директору?")
    Group {
        SummaryRow("Месяц", monthName(rep.month))
        SummaryRow("Предметы", rep.subjects.joinToString(", ") { App.subj(it.subject).name }.ifEmpty { "—" })
        SummaryRow("Занятий", rep.subjects.sumOf { it.lessons }.toString(), true)
        if (App.money) SummaryRow("Начислено", rub(rep.subjects.sumOf { it.billed }), true)
    }
    Sec("Комментарий (необязательно)")
    Area(note, "Например: 2 занятия перенесены на следующий месяц") { note = it }
    Lock("Директор сразу получит уведомление в Telegram и доступ к занятиям, оплатам и фотоотчётам за этот месяц." + if (rep.month == App.currentMonth) " Месяц ещё не закончился: после сдачи изменения будут видны директору как правки." else "")
    Actions {
        Btn("Отмена") { close() }
        Btn("Сдать", "primary") { App.act("Отчёт сдан директору") { Api.post<SubmissionDto, SubmitInput>("/api/report/submit", SubmitInput(rep.month, note)); close() } }
    }
}

fun openSubmission(r: InboxRowDto, month: String) = Sheets.open { close ->
    var returning by remember { mutableStateOf(false) }
    var comment by remember { mutableStateOf("") }
    val s = r.submission; val t = r.totals
    fun act(action: String, msg: String, body: ReturnInput? = null) = App.act(msg) {
        if (body != null) Api.post<SubmissionDto, ReturnInput>("/api/report/${r.tutor}/$month/$action", body) else Api.postEmpty<SubmissionDto>("/api/report/${r.tutor}/$month/$action")
        close()
    }
    Title(App.tutorName(r.tutor))
    MetaText(monthName(month) + " · " + when (s?.status) {
        null -> "Репетитор ещё не сдал отчёт. Данные ниже — текущие."
        "submitted" -> "Сдан ${s.submittedAt?.let { dateTime(it) }}."
        "accepted" -> "Принят ${s.decidedAt?.let { dateTime(it) }}${if (s.selfAccepted) " директором без сдачи" else ""}."
        else -> "Возвращён ${s.decidedAt?.let { dateTime(it) }}: «${s.comment}»"
    })
    Grid2 {
        Kpi("Занятий", t.lessons.toString())
        Kpi("Начислено", rub(t.billed), true)
        Kpi("Получено", rub(t.received), true)
        Kpi("Разница", rub(t.billed - t.received), true, if (t.billed - t.received > 0) colors.bad else colors.ok)
    }
    s?.note?.let { Sec("Комментарий репетитора"); Card(Modifier.padding(top = 0.dp)) { Text(it) } }
    Sec("Данные по предметам")
    Group {
        if (t.subjects.isEmpty()) Empty("В этом месяце занятий нет.")
        t.subjects.forEach { id -> ListRow({ openPreview(r.tutor, month, listOf(id)) }) { Main { Name(App.subj(id).name); MetaText("посещаемость, занятия, оплаты") }; Chip("открыть", "grp") } }
    }
    Actions { Btn("Скачать .xlsx") { exportNow(ExportInput("month", month, t.subjects, r.tutor.toString(), "file")) } }
    when {
        returning -> {
            Sec("Что исправить")
            Area(comment, "Например: нет фото оплаты Петрова за 7 число") { comment = it }
            Actions { Btn("Назад") { returning = false }; Btn("Вернуть", "danger", enabled = comment.isNotBlank()) { act("return", "Отчёт возвращён на доработку", ReturnInput(comment)) } }
        }
        s?.status == "accepted" -> Actions { Btn("Снять отметку «принят»") { act("reopen", "Отметка «принят» снята") }; Btn("Готово", "primary") { close() } }
        else -> Actions {
            if (s?.status == "submitted") Btn("Вернуть на доработку", "danger") { returning = true }
            Btn(if (s?.status == "submitted") "Принять" else "Отметить как принятый", "primary") { act("accept", "Отчёт принят") }
        }
    }
}

// ---------- Команда ----------
fun openTutor(orig: TutorDto?) = Sheets.open { close ->
    var name by remember { mutableStateOf(orig?.name ?: "") }
    var tgId by remember { mutableStateOf(orig?.id?.toString() ?: "") }
    var subjects by remember { mutableStateOf(orig?.subjects ?: emptyList()) }
    var perms by remember { mutableStateOf(orig?.perms ?: TUTOR_PRESET) }
    var rate by remember { mutableStateOf(orig?.rate) }
    var confirmOff by remember { mutableStateOf(false) }
    if (confirmOff && orig != null) {
        Title("Отключить: ${orig.name}?")
        Text("Все права будут сняты: бот и веб-интерфейс перестанут отвечать этому человеку. Занятия и оплаты останутся в отчётах.", color = colors.hint, lineHeight = 21.sp)
        Actions {
            Btn("Отмена") { confirmOff = false }
            Btn("Отключить", "danger") { App.act("Доступ отключён, история занятий сохранена") { Api.postEmpty<TutorDto>("/api/tutors/${orig.id}/disable"); close() } }
        }
        return@open
    }
    val adm = "ADMIN" in perms
    Title(orig?.name ?: "Новый преподаватель")
    if (orig != null && orig.perms.isEmpty()) {
        MetaText("Доступ отключён.")
        Actions { Btn("Вернуть доступ", "primary") { App.act("Доступ возвращён с правами репетитора") { Api.postEmpty<TutorDto>("/api/tutors/${orig.id}/enable"); close() } } }
        Gap(10.dp)
    }
    Fields {
        FieldLabel("Имя"); TextField(name, "Фамилия Имя") { name = it }
        FieldLabel("Telegram ID"); TextField(tgId, "428100231", KeyboardType.Number, num = true, readOnly = orig != null) { tgId = it }
        FieldLabel("Ставка, % от начисленного"); NumberField(rate) { rate = it }
    }
    Sec("Предметы")
    Opts { App.b.subjects.forEach { s -> Opt(s.name, s.id in subjects) { subjects = if (s.id in subjects) subjects - s.id else subjects + s.id } } }
    Sec("Быстрый набор прав")
    Opts { PRESETS.forEach { (t, l) -> Opt(t, l.size == perms.size && l.all { it in perms }) { perms = l } } }
    Sec("Права")
    Group {
        PERMS.forEach { p ->
            val implied = adm && p.k in ADMIN_IMPLIES
            val on = p.k in perms || implied
            ListRow(if (implied) null else ({ perms = if (on) perms - p.k else perms + p.k }), dim = implied, top = true) {
                Checkbox(on, if (implied) null else ({ perms = if (it) perms + p.k else perms - p.k }), enabled = !implied)
                Main {
                    NameLine { Name(p.t); if (p.isNew) PermChip("новое", true) }
                    MetaText(p.d + if (implied) " · входит в ADMIN" else "")
                    Gap(2.dp)
                    PermChip(p.k)
                }
            }
        }
    }
    val err = when {
        name.isBlank() -> "Введите имя преподавателя."
        !Regex("^\\d{5,15}$").matches(tgId.trim()) -> "Telegram ID — это число, например 428100231. Его можно узнать у @userinfobot."
        !("DIRECTOR" in perms || adm) && perms.any { it.startsWith("ADD_TIME") } && subjects.isEmpty() -> "Выберите хотя бы один предмет: без него нельзя внести занятие."
        (rate ?: 0) !in 0..100 -> "Ставка от 0 до 100%."
        else -> null
    }
    Lock(err ?: "")
    Actions {
        Btn("Отмена") { close() }
        Btn(if (orig != null) "Сохранить" else "Добавить", "primary", enabled = err == null) {
            val input = TutorInput(tgId.trim().toLong(), name.trim(), perms, subjects, rate)
            App.act(if (orig != null) "Права сохранены" else "Преподаватель добавлен. Пусть откроет бота и нажмёт /start") {
                if (orig != null) Api.post<TutorDto, TutorInput>("/api/tutors/${orig.id}", input, HttpMethod.Put) else Api.post<TutorDto, TutorInput>("/api/tutors", input)
                close()
            }
        }
    }
    if (orig != null && orig.perms.isNotEmpty() && orig.id != App.me) Actions { Btn("Отключить доступ", "danger") { confirmOff = true } }
    if (orig != null) HistoryButton("tutor", orig.id.toString(), orig.name)
}

// ---------- Журнал ----------
fun openAudit(e: AuditDto) = Sheets.open { close ->
    val ch = auditChanges(e)
    SheetKicker(dateTime(e.at) + " · " + if (e.source == "BOT") "через бота" else "через веб")
    Title("${e.action}: ${(ETYPES[e.type] ?: e.type).lowercase()}")
    Group {
        LabelRow("Кто") { UserLink(e.actor, e.actorName) }
        SummaryRow("Запись", e.title)
        SummaryRow("ID", "${e.type} #${e.id}", true)
    }
    if (ch.isNotEmpty()) {
        if (e.type == "session") { Sec("Подробности"); Group { ch.forEach { SummaryRow(it.first, it.third) } } }
        else {
            Sec("Что изменилось")
            Group {
                ListRow { Text("Поле", Modifier.weight(1f), fontWeight = FontWeight.Bold); Text("Было", Modifier.weight(1f), fontWeight = FontWeight.Bold); Text("Стало", Modifier.weight(1f), fontWeight = FontWeight.Bold) }
                ch.forEach { (k, a, b) ->
                    ListRow(top = true) { Text(k, Modifier.weight(1f), color = colors.hint, fontSize = 13.sp); Text(a, Modifier.weight(1f), color = colors.bad, fontSize = 13.sp); Text(b, Modifier.weight(1f), color = colors.ok, fontSize = 13.sp) }
                }
            }
        }
    }
    Actions { Btn("Вся история записи") { AuditView.showRecord(e.type, e.id, e.title) } }
}

fun openAlerts() = Sheets.open { close ->
    Show(remote("alerts") { Api.get<AlertsDto>("/api/alerts") }) { a0 ->
        var a by remember { mutableStateOf(a0) }
        Title("Оповещения администратору")
        Group {
            Toggle("Массовое удаление", "5 и больше удалений за 10 минут одним человеком", a.mass) { a = a.copy(mass = it) }
            Toggle("Правки ночью", "изменения с 00:00 до 07:00", a.night) { a = a.copy(night = it) }
            Toggle("Правки закрытого месяца", "любое изменение после того, как месяц закрыт", a.closed) { a = a.copy(closed = it) }
        }
        Lock("Приходит в личку администраторам, кнопка открывает журнал.")
        Actions { Btn("Готово", "primary") { App.act("Оповещения сохранены") { Api.post<AlertsDto, AlertsDto>("/api/alerts", a, HttpMethod.Put); close() } } }
    }
}

// ---------- Поиск ----------
fun openSearch() = Sheets.open { close ->
    var q by remember { mutableStateOf("") }
    var res by remember { mutableStateOf<SearchDto?>(null) }
    LaunchedEffect(q) {
        if (q.trim().length < 2) { res = null; return@LaunchedEffect }
        kotlinx.coroutines.delay(250)
        res = runCatching { Api.get<SearchDto>("/api/search?q=" + q.trim().encodeURLParameter()) }.getOrNull()
    }
    Title("Поиск")
    TextField(q, "Ученик, предмет, сумма", search = true) { q = it }
    val r = res
    if (r == null) Lock("Ищет сразу по ученикам, занятиям" + (if (App.caps.readPayments) " и оплатам" else "") + ". Например: «соколова», «физика», «4800».")
    else if (r.students.isEmpty() && r.lessons.isEmpty() && r.payments.isEmpty()) Empty("Ничего не нашлось.")
    else {
        if (r.students.isNotEmpty()) { Sec("Ученики"); Group { r.students.forEach { s -> ListRow({ close(); openStudent(s.id) }) { Avatar(s.id, sFio(s)); Main { Name(sFio(s)); MetaText("${s.cls ?: "—"} класс · ${STATUS[s.status]?.lowercase()}") } } } } }
        if (r.lessons.isNotEmpty()) { Sec("Занятия"); Group { r.lessons.forEach { l -> ListRow({ close(); openLesson(l.id) }) { TimeCol(shortDay(l.date), 54.dp, small = true); Main { Name(names(l)); MetaText(App.subj(l.subject).name + " · " + l.time) } } } } }
        if (r.payments.isNotEmpty()) { Sec("Оплаты"); Group { r.payments.forEach { p -> ListRow({ close(); openPayment(p.id) }) { TimeCol(shortDay(p.date), 54.dp, small = true); Main { Name(p.student.fio); MetaText(App.subj(p.subject).name) }; if (App.money) Num(rub(p.amount)) } } } }
    }
}
