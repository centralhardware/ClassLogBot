package me.centralhardware.znatoki.telegram.statistic.webapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.ktor.http.HttpMethod
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Table
import org.jetbrains.compose.web.dom.Td
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Th
import org.jetbrains.compose.web.dom.Tr
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.get

/** A picked photo, already shrunk to a JPEG: the bytes to upload and a preview URL. */
class Photo(val bytes: ByteArray, val url: String)

fun pickPhoto(done: (Photo) -> Unit) {
    val inp = document.createElement("input") as HTMLInputElement
    inp.type = "file"; inp.accept = "image/*"
    inp.onchange = {
        inp.files?.get(0)?.let { f ->
            val src = URL.createObjectURL(f)
            val img = org.w3c.dom.Image()
            img.onload = {
                val k = minOf(1.0, 2000.0 / maxOf(img.width, img.height))
                val c = document.createElement("canvas") as HTMLCanvasElement
                c.width = (img.width * k).toInt(); c.height = (img.height * k).toInt()
                c.getContext("2d").asDynamic().drawImage(img, 0, 0, c.width, c.height)
                c.toBlob({ b: Blob? -> URL.revokeObjectURL(src); b?.let { blob -> blobToBytes(blob) { bytes -> done(Photo(bytes, URL.createObjectURL(blob))) } } }, "image/jpeg", 0.85)
            }
            img.onerror = { _, _, _, _, _ -> Toasts.show("Не удалось открыть фото"); null }
            img.src = src
        }
    }
    inp.click()
}

@Composable
fun PhotoBox(photo: Photo?, empty: String, onPick: (Photo) -> Unit) =
    Box("photo" + if (photo != null) " done" else "", onClick = { pickPhoto(onPick) }) {
        if (photo != null) { Img(photo.url, "фото"); Text("Фото приложено · нажмите, чтобы заменить") } else Text(empty)
    }

/** Who the record is for, when the caller may enter lessons or payments for other tutors. */
@Composable
fun TutorPick(forOthers: Boolean, value: Long, onPick: (Long) -> Unit) {
    if (!forOthers) return
    Box("sec", "margin-top:0") { Text("Репетитор") }
    Box("tutorpick") { App.teaching().forEach { t -> Opt(t.name + if (t.id == App.me) " (вы)" else "", t.id == value) { onPick(t.id) } } }
}

@Composable
fun HistoryButton(type: String, id: String, title: String) {
    if (!App.caps.admin) return
    Actions { Btn("История изменений") { AuditView.showRecord(type, id, title) } }
}

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
        Box("sec", "margin-top:0") { Text("${dayMonth(l.date)} · ${l.time}") }
        Title(names(l))
        if (edit) LessonEdit(l, onCancel = { edit = false }) { p -> patch(p, "Занятие изменено"); edit = false }
        else {
            Box("summary") {
                SummaryRow("Репетитор", App.tutorName(l.tutor))
                SummaryRow("Предмет", App.subj(l.subject).name)
                SummaryRow("Тип", if (isGroup(l)) "Групповое" else "Индивидуальное")
                if (App.money) SummaryRow("Цена за ученика", rub(price(l)) + if (l.half) " (×1,5)" else "", true)
                else if (l.half) SummaryRow("Длительность", "полтора часа")
                if (l.flags.isNotEmpty()) SummaryRow("Проверить", l.flags.joinToString(", "))
            }
            if (l.hasPhoto) Thumb(photoUrl("lesson", l.id), "Фотоотчёт")
            if (why != null) Lock(why) else if (l.date.substring(0, 7) != App.currentMonth) Lock("Закрытый месяц: вы правите как админ.")
            val single = l.students.size == 1
            if (App.caps.extraHalfHour || (single && App.caps.forceGroup)) Box("group", "margin-top:12px") {
                if (App.caps.extraHalfHour) Toggle("Полтора часа", "стоимость ×1,5", l.half, why == null && !l.deleted) { patch(LessonPatch(half = it), if (it) "Теперь полтора часа" else "Снова один час") }
                if (single && App.caps.forceGroup) Toggle("Групповое", "один ученик, но занятие групповое", l.forceGroup, why == null && !l.deleted) { patch(LessonPatch(forceGroup = it), if (it) "Отмечено как групповое" else "Отмечено как индивидуальное") }
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
    Box("", "display:grid;gap:8px") {
        FieldLabel("Стоимость за ученика, ₽"); NumberField(amount) { amount = it }
        FieldLabel("Предмет"); Dropdown(App.b.subjects.map { it.id to it.name }, subject) { subject = it }
        FieldLabel("Дата"); TextField(date, type = InputType.Date, cls = "field num") { date = it }
        FieldLabel("Время"); TextField(time, type = InputType.Time, cls = "field num") { time = it }
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
    Box("lock", "padding-top:0;margin-bottom:8px") { Text("Занятий: ${ls.size}. Каждое изменение попадёт в журнал" + if (m != App.currentMonth) " и в отчёт за ${monthName(m).lowercase()}." else ".") }
    Box("", "display:grid;gap:8px") {
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

@Composable
private fun AddLesson(pre: LessonPrefill?, close: () -> Unit) {
    val draft = remember {
        if (pre == null) runCatching { localStorage.getItem(DRAFT_KEY)?.let { Api.json.decodeFromString<LessonPrefill>(it) } }.getOrNull() else null
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
        runCatching { localStorage.setItem(DRAFT_KEY, Api.json.encodeToString(LessonPrefill.serializer(), LessonPrefill(subject, studentsSel, amount, tutor, from = from))) }
    }
    fun clearDraft() = runCatching { localStorage.removeItem(DRAFT_KEY) }

    val tutorObj = App.b.tutors.find { it.id == tutor }
    val subjects = App.b.subjects.filter { App.caps.admin || tutorObj == null || tutorObj.subjects.isEmpty() || it.id in tutorObj.subjects }
    Steps(total, step)
    if (restored) Box("card", "margin-bottom:10px;font-size:13px;display:flex;justify-content:space-between;align-items:center;gap:8px") {
        Span { Text("Черновик восстановлен: продолжите с того места, где остановились.") }
        Btn("Заново", "mini ghost") { clearDraft(); restored = false; subject = null; studentsSel = emptyList(); amount = 1000; from = null; step = 0 }
    }
    if (pre?.slot != null) Box("lock", "padding-top:0;margin-bottom:8px") { Text("По расписанию: ${WDF[dow(App.today)].lowercase()} ${pre.time}. Всё заполнено из слота — проверьте и нажимайте «Далее».") }
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
            Box("opts") { subjects.forEach { s -> Opt(s.name, subject == s.id) { if (subject != s.id) studentsSel = emptyList(); subject = s.id } } }
            val groupNames = subjects.filter { it.group }.joinToString(" и ") { it.name.lowercase() }
            if (groupNames.isNotEmpty()) Lock("В предметах $groupNames можно отметить нескольких учеников на одном занятии.")
        }
        1 -> {
            val sb = App.subj(subject!!)
            Title(if (sb.group) "Кто был на занятии?" else "Какой ученик?")
            TextField(q, "Фильтр по ФИО") { q = it }
            Show(remote("students") { loadStudents() }) { all ->
                // A restored draft or a repeated lesson can carry a student who is no longer active: drop them, or they
                // count towards "Выбрано" without ever showing as ticked.
                val active = all.filter { !it.deleted && it.status == "active" }
                val kept = studentsSel.distinct().filter { id -> active.any { it.id == id } }
                if (kept != studentsSel) studentsSel = kept
                val list = active.filter { q.isBlank() || sFio(it).lowercase().contains(q.lowercase()) }
                Box("group", "margin-top:8px") {
                    if (list.isEmpty()) Empty("Никого не нашлось.")
                    list.forEach { s ->
                        Box("row", onClick = {
                            studentsSel = if (sb.group) (if (s.id in studentsSel) studentsSel - s.id else studentsSel + s.id) else listOf(s.id)
                        }) {
                            Avatar(s.id, sFio(s)); Box("main") { Box("name") { Text(sFio(s)) }; Box("meta") { Text("${s.cls ?: "—"} класс") } }
                            Div { if (s.id in studentsSel) Chip("✓", "grp") }
                        }
                    }
                }
            }
            Lock(if (sb.group) "Выбрано: ${studentsSel.size}. Двое и больше — групповое занятие." else "${sb.name} только индивидуально.")
        }
        2 -> {
            Title("Стоимость за ученика")
            NumberField(amount) { amount = it ?: 0 }
            Box("opts", "margin-top:8px") { listOf(800, 900, 1000, 1200, 1400).forEach { v -> Opt(rub(v), amount == v) { amount = v } } }
            Lock(if (amount <= 0) "Сумма должна быть больше нуля." else from?.let { "Как на прошлом занятии $it." } ?: "")
        }
        3 -> {
            if (pre?.from != null) Box("lock", "padding-top:0;margin-bottom:8px") { Text("Повтор занятия от ${pre.from}. Можно вернуться и поменять.") }
            Title("Фотоотчёт")
            PhotoBox(photo, "Нажмите, чтобы приложить фото отчётности") { photo = it }
            Lock("Откроется камера или галерея.")
        }
        else -> {
            val grp = studentsSel.size > 1
            Title("Сохранить занятие?")
            Show(remote("students") { loadStudents() }) { all ->
                Box("summary") {
                    if (tutor != App.me) Box("row") { Box("main") { Text("Репетитор") }; Div { Text(App.tutorName(tutor) + " "); Chip("внесено вами") } }
                    SummaryRow("Предмет", App.subj(subject!!).name)
                    Box("row") { Box("main") { Text("Ученики") }; Box("right", "max-width:60%") { Text(studentsSel.mapNotNull { id -> all.find { it.id == id }?.let { sFio(it) } }.joinToString(", ")) } }
                    SummaryRow("Тип", if (grp) "Групповое" else "Индивидуальное")
                    SummaryRow("Стоимость", rub(amount) + if (grp) " × ${studentsSel.size}" else "", true)
                    Box("row") { Box("main") { Text("Фото") }; Div { Chip("приложено", "ok") } }
                }
            }
            if (App.caps.admin && pre?.slot == null) {
                Box("", "display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:10px") {
                    Div { FieldLabel("Дата"); TextField(date, type = InputType.Date, cls = "field num") { date = it } }
                    Div { FieldLabel("Время"); TextField(time, type = InputType.Time, cls = "field num") { time = it } }
                }
                Lock("Админ может внести занятие задним числом, в том числе в закрытый месяц.")
            }
            Lock("После сохранения в лог-чат уйдёт обычное сообщение #занятие с фото.")
        }
    }
    if (subject != null && step < 4) saveDraft()
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
    Box("", "display:grid;gap:8px;margin-top:10px") {
        if (weekly) { FieldLabel("Каждую неделю, день"); Dropdown(WDF.indices.map { it to WDF[it] }, weekday) { weekday = it } }
        else { FieldLabel("Дата"); TextField(date, type = InputType.Date, cls = "field num") { date = it } }
        FieldLabel("Время"); TextField(time, type = InputType.Time, cls = "field num") { time = it }
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
    Box("group") {
        Toggle("Неотмеченные занятия", "Вечером, если по расписанию что-то не отмечено", evening) { evening = it }
        Box("row tg") { Box("main") { Box("name") { Text("Время") } }; Dropdown(listOf("20:00", "21:00", "22:00").map { it to it }, at, css = "width:auto") { at = it } }
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
                Box("", "display:flex;gap:12px;align-items:center") {
                    Avatar(s.id, sFio(s))
                    Div {
                        org.jetbrains.compose.web.dom.H2({ style("margin:0") }) { Text(listOf(s.last, s.first, s.middle).filter { it.isNotBlank() }.joinToString(" ")) }
                        Box("meta", "color:var(--hint);font-size:12px") { Text("ID ${s.id} · ${s.cls ?: "—"} класс · " + s.subjects.joinToString(", ") { App.subj(it).name }.ifEmpty { "занятий ещё не было" }) }
                    }
                }
                Sec("Статус")
                Box("summary") {
                    Box("row") {
                        Box("main") { Text(STATUS[s.status] ?: s.status); if (s.status != "active") T(" · ${s.reason ?: ""}" + (s.since?.let { " · с ${MG[it.substring(5, 7).toInt() - 1]}" } ?: ""), "meta", "color:var(--hint)") }
                        if (App.caps.client && !s.deleted) Btn("Изменить", "mini ghost") { mode = "status" }
                    }
                }
                s.balance?.let { b ->
                    Sec("Баланс за всё время")
                    Box("summary") { Box("row") { Box("main") { Text(if (b < 0) "Долг" else if (b > 0) "Предоплата" else "Расчёты сведены") }; Box("num", "color:${if (b < 0) "var(--bad)" else if (b > 0) "var(--ok)" else "inherit"}") { Text(rub(kotlin.math.abs(b))) } } }
                }
                Sec("Контакты")
                Box("summary") {
                    listOf("Ученик" to d.phone, "Родитель" to d.parent).forEach { (t, ph) ->
                        Box("row", "display:block") {
                            Div({ style("display:flex;justify-content:space-between") }) { Span { Text(t) }; Span({ classes("num") }) { Text(fmtPhone(ph)) } }
                            if (!ph.isNullOrEmpty()) Box("slotacts", "margin-top:8px") {
                                listOf("Позвонить" to "tel:+$ph", "Telegram" to "https://t.me/+$ph", "WhatsApp" to "https://wa.me/$ph").forEachIndexed { i, (label, href) ->
                                    org.jetbrains.compose.web.dom.A(href, { classes("mini"); if (i > 0) classes("ghost"); attr("target", "_blank"); attr("rel", "noopener") }) { Text(label) }
                                }
                            }
                        }
                    }
                    d.motherFio?.takeIf { it.isNotBlank() }?.let { SummaryRow("ФИО матери", it) }
                    SummaryRow("Источник", s.source ?: "—")
                }
                Sec("Заметки")
                Box("summary") {
                    if (d.notes.isEmpty()) Box("row") { Box("main meta", "color:var(--hint)") { Text("Заметок пока нет") } }
                    d.notes.forEach { n -> Box("row", "display:block") { Div({ style("line-height:1.45") }) { Text(n.text) }; Box("meta", "color:var(--hint);font-size:11px;margin-top:3px") { Text(dateTime(n.at) + " · " + n.by) } } }
                    if ((App.caps.time || App.caps.client) && !App.caps.director) Box("row", "display:block") {
                        Area(note, "Что прошли, домашнее задание, на что обратить внимание") { note = it }
                        Box("actions", "margin-top:6px") {
                            Btn("Добавить заметку", enabled = note.isNotBlank()) { App.act("Заметка сохранена") { Api.post<List<NoteDto>, TextInput>("/api/students/$id/notes", TextInput(note)); note = "" } }
                        }
                    }
                }
                Sec("История")
                Box("summary") {
                    SummaryRow("Занятий", d.lessons.toString(), true)
                    d.paid?.let { SummaryRow("Оплачено всего", rub(it), true) }
                    d.recent.forEach { r -> Box("row") { Box("main") { Box("meta", "margin:0") { Text(dayMonth(r.date) + " · " + App.subj(r.subject).name) } }; Div { if (r.group) Chip("группа", "grp") else Chip("индивид.") } } }
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
    Box("opts") { STATUS.forEach { (k, t) -> Opt(t, st == k) { st = k } } }
    Label("Причина (для паузы и ухода)")
    Dropdown(App.b.reasons.map { it to it }, reason) { reason = it }
    Lock("Ушедшие и на паузе не предлагаются в форме занятия. Причины ухода видны в статистике.")
    Actions {
        Btn("Отмена") { done() }
        Btn("Сохранить", "primary") { App.act("Статус: ${STATUS[st]!!.lowercase()}") { Api.post<StudentRowDto, StatusInput>("/api/students/${s.id}/status", StatusInput(st, reason)); done() } }
    }
}

@Composable private fun Label(text: String) = Box("", "color:var(--hint);font-size:12px;display:block;margin-top:12px") { Text(text) }

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
    Box("", "display:grid;gap:8px") {
        TextField(last, "Фамилия") { last = it }
        TextField(first, "Имя") { first = it }
        TextField(middle, "Отчество (необязательно)") { middle = it }
    }
    Sec("Класс")
    Box("opts") { (1..11).forEach { i -> Opt(i.toString(), cls == i, css = "min-width:38px") { cls = i } } }
    Sec("Телефоны (необязательно)")
    Box("", "display:grid;gap:8px") {
        FieldLabel("Ученик"); TextField(phone, "+7 (913) 123-45-67", InputType.Tel, "field num" + if (phoneErr(phone) != null) " bad" else "") { phone = it }
        phoneErr(phone)?.let { Box("ferr") { Text(it) } }
        FieldLabel("Родитель"); TextField(parent, "+7 (923) 123-45-67", InputType.Tel, "field num" + if (phoneErr(parent) != null) " bad" else "") { parent = it }
        phoneErr(parent)?.let { Box("ferr") { Text(it) } }
    }
    Sec("Откуда узнали? (необязательно)")
    TextField(source, "Например: от знакомых, 2gis, вывеска") { source = it }
    val sugg = App.b.sources.filter { source.isBlank() || (it.lowercase().contains(source.lowercase()) && !it.equals(source, true)) }.take(6)
    Box("opts", "margin-top:6px") {
        sugg.forEach { v -> Opt(v, false, css = "padding:6px 10px;font-size:13px") { source = v } }
        if (source.isNotBlank() && App.b.sources.none { it.equals(source.trim(), true) }) Box("lock", "padding:6px 2px") { Text("Новый вариант — сохранится как есть") }
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
        Box("sec", "margin-top:0") { Text("${dayMonth(p.date)} · №${p.id}") }
        Title(p.student.fio)
        if (edit) {
            var amount by remember { mutableStateOf<Int?>(p.amount) }
            var subject by remember { mutableStateOf(p.subject) }
            var tutor by remember { mutableStateOf(p.tutor) }
            Box("", "display:grid;gap:8px") {
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
            Box("summary") {
                if (App.money) SummaryRow("Сумма", rub(p.amount), true)
                SummaryRow("Предмет", App.subj(p.subject).name)
                SummaryRow("Оплату принял", App.tutorName(p.tutor))
                Box("row") { Box("main") { Text("Чек") }; Div { if (p.hasPhoto) Chip("приложен", "ok") else Chip("нет", "half") } }
            }
            if (p.hasPhoto) Thumb(photoUrl("payment", p.id.toString()), "Чек об оплате")
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
            TextField(q, "Фильтр по ФИО") { q = it }
            Show(students) { all ->
                Box("group", "margin-top:8px") {
                    all.filter { !it.deleted && (q.isBlank() || sFio(it).lowercase().contains(q.lowercase())) }.forEach { s ->
                        Box("row", onClick = { student = s.id }) {
                            Avatar(s.id, sFio(s)); Box("main") { Box("name") { Text(sFio(s)) }; Box("meta") { Text("${s.cls ?: "—"} класс") } }
                            Div { if (student == s.id) Chip("✓", "grp") }
                        }
                    }
                }
            }
        }
        1 -> {
            Title("За какой предмет?")
            Box("opts") { App.b.subjects.forEach { s -> Opt(s.name, subject == s.id) { subject = s.id } } }
            org.jetbrains.compose.web.dom.H2({ style("margin-top:16px") }) { Text("Сумма") }
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
            Box("summary") {
                if (tutor != App.me) SummaryRow("Оплату принял", App.tutorName(tutor))
                SummaryRow("Ученик", fio(student))
                SummaryRow("Предмет", App.subj(subject!!).name)
                SummaryRow("Сумма", rub(amount), true)
                Box("row") { Box("main") { Text("Чек") }; Div { Chip("приложен", "ok") } }
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
    Sec("Что выгрузить")
    Box("opts") {
        Opt("Отчёт по оплате и посещаемости", kind == "month") { kind = "month" }
        if (App.money && App.caps.readPayments) Opt("Список оплат", kind == "payments") { kind = "payments" }
    }
    Sec("Месяц")
    if (App.seesAll) {
        val first = App.b.months.first().substring(0, 4).toInt(); val last = App.currentMonth.substring(0, 4).toInt()
        Box("monthbar", "margin-bottom:6px") {
            Btn("‹", "iconbtn", enabled = year > first) { year-- }; B { Text(year.toString()) }; Btn("›", "iconbtn", enabled = year < last) { year++ }
        }
        Box("", "display:grid;grid-template-columns:repeat(4,1fr);gap:6px") {
            MS.forEachIndexed { i, n ->
                val m = "$year-" + (i + 1).toString().padStart(2, '0')
                Opt(n, month == m, enabled = m in App.b.months, css = "padding:8px 4px") { month = m }
            }
        }
        Box("lock", "padding-top:6px") { Text("Выбрано: ${monthName(month)}${if (month == App.currentMonth) " (текущий)" else ""}. Доступен любой месяц с начала учёта, как /reportmonth в боте.") }
    } else Box("lock", "padding-top:0") { Text("${monthName(App.currentMonth)} (текущий). Прошлые месяцы выгружает администратор.") }
    if (kind == "month") {
        Sec("Предметы")
        Box("opts") { App.b.subjects.forEach { s -> Opt(s.name, s.id in subjects) { subjects = if (s.id in subjects) subjects - s.id else subjects + s.id } } }
        if (App.caps.admin) { Sec("Репетиторы"); Box("opts") { Opt("Только я", tutor == "me") { tutor = "me" }; Opt("Все репетиторы", tutor == "all") { tutor = "all" } } }
    }
    Sec("Куда")
    Box("opts") { Opt("Прислать в чат с ботом", to == "chat") { to = "chat" }; Opt("Скачать на устройство", to == "file") { to = "file" } }
    if (kind == "month" && subjects.isNotEmpty()) Actions { Btn("Предпросмотр") { openPreview(App.me, month, subjects) } }
    Lock((if (to == "chat") "Бот пришлёт файлы сообщением, как сейчас по /report — их удобно переслать." else "Несколько файлов скачаются одним архивом .zip.") +
        if (month == App.currentMonth) " Текущий месяц ещё не закрыт, цифры могут измениться." else "")
    Actions {
        Btn("Отмена") { close() }
        Btn("Выгрузить", "primary", enabled = kind != "month" || subjects.isNotEmpty()) { exportNow(ExportInput(kind, month, subjects, tutor, to)); close() }
    }
}

fun openPreview(tutor: Long, month: String, subjects: List<Long>) = Sheets.open { close ->
    var si by remember { mutableStateOf(0) }
    var tab by remember { mutableStateOf("att") }
    val subject = subjects[si]
    Title("Предпросмотр")
    Show(remote(tutor, month, subject, "preview") { Api.get<PreviewDto>("/api/report/preview?tutor=$tutor&month=$month&subject=$subject") }) { x ->
        Box("meta", "color:var(--hint);font-size:12px;margin:-4px 2px 8px") { Text(x.name) }
        if (subjects.size > 1) Box("opts", "margin-bottom:6px") { subjects.forEachIndexed { i, id -> Opt(App.subj(id).name, si == i) { si = i } } }
        Box("xltabs") {
            (listOf("att" to "отчет", "ls" to "журнал занятий") + if (x.pays != null) listOf("pay" to "журнал оплаты") else emptyList()).forEach { (v, t) ->
                org.jetbrains.compose.web.dom.Button({ attr("aria-pressed", (tab == v).toString()); onClick { tab = v } }) { Text(t) }
            }
        }
        Box("xl") {
            Table {
                when (tab) {
                    "att" -> {
                        listOf("Отчет по оплате и посещаемости занятий по ${x.title}", "Преподаватель: ${x.tutorName}", x.monthTitle).forEach { Tr({ classes("ttl") }) { Td({ attr("colspan", "8") }) { Text(it) } } }
                        Tr { listOf("№", "ФИО ученика", "Класс", "посетил индивидуально", "посетил групповые", "оплата", "Итого", "Даты посещений").forEach { Th { Text(it) } } }
                        if (x.att.isEmpty()) Tr { Td({ attr("colspan", "8"); classes("c") }) { Text("нет занятий") } }
                        x.att.forEach { r -> Tr { Td({ classes("c") }) { Text(r.n.toString()) }; Td { Text(r.fio) }; Td({ classes("c") }) { Text(r.cls?.toString() ?: "") }; Td({ classes("n") }) { Text(r.ind.toString()) }; Td({ classes("n") }) { Text(r.grp.toString()) }; Td({ classes("n") }) { Text(r.paid?.toString() ?: "—") }; Td {}; Td { Text(r.dates) } } }
                        Tr({ classes("tot") }) { Td {}; Td { Text("Итого") }; Td {}; x.attTotals.forEach { Td({ classes("n") }) { Text(it?.toString() ?: "—") } }; Td {}; Td {} }
                    }
                    "ls" -> {
                        Tr { listOf("Дата", "ученик", "Сумма", "Принудительно групповое", "Полтора часа").forEach { Th { Text(it) } } }
                        x.log.forEach { r -> Tr { Td({ classes("n") }) { Text(r.at) }; Td({ style("white-space:pre") }) { Text(r.fio) }; Td({ classes("n") }) { Text(r.price.toString()) }; Td({ classes("c") }) { Text(r.group) }; Td({ classes("c") }) { Text(r.half) } } }
                        Tr({ classes("tot") }) { Td { Text("итого") }; Td {}; Td({ classes("n") }) { Text(x.logTotal.toString()) }; Td {}; Td {} }
                    }
                    else -> {
                        Tr { listOf("Дата", "ученик", "Сумма").forEach { Th { Text(it) } } }
                        if (x.pays.isNullOrEmpty()) Tr { Td({ attr("colspan", "3"); classes("c") }) { Text("нет оплат") } }
                        x.pays?.forEach { r -> Tr { Td({ classes("n") }) { Text(r.at) }; Td { Text(r.fio) }; Td({ classes("n") }) { Text(r.amount.toString()) } } }
                        Tr({ classes("tot") }) { Td { Text("итого") }; Td {}; Td({ classes("n") }) { Text(x.payTotal?.toString() ?: "") } }
                    }
                }
            }
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
    Box("summary") {
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
    Box("meta", "color:var(--hint);font-size:12px;margin:-4px 2px 8px") {
        Text(monthName(month) + " · " + when (s?.status) {
            null -> "Репетитор ещё не сдал отчёт. Данные ниже — текущие."
            "submitted" -> "Сдан ${s.submittedAt?.let { dateTime(it) }}."
            "accepted" -> "Принят ${s.decidedAt?.let { dateTime(it) }}${if (s.selfAccepted) " директором без сдачи" else ""}."
            else -> "Возвращён ${s.decidedAt?.let { dateTime(it) }}: «${s.comment}»"
        })
    }
    Box("kpis") {
        Box("kpi") { Span { Text("Занятий") }; B { Text(t.lessons.toString()) } }
        Box("kpi") { Span { Text("Начислено") }; B({ classes("num") }) { Text(rub(t.billed)) } }
        Box("kpi") { Span { Text("Получено") }; B({ classes("num") }) { Text(rub(t.received)) } }
        Box("kpi") { Span { Text("Разница") }; B({ classes("num"); style("color:${if (t.billed - t.received > 0) "var(--bad)" else "var(--ok)"}") }) { Text(rub(t.billed - t.received)) } }
    }
    s?.note?.let { Sec("Комментарий репетитора"); Box("status", "margin:0") { Text(it) } }
    Sec("Данные по предметам")
    Box("group") {
        if (t.subjects.isEmpty()) Empty("В этом месяце занятий нет.")
        t.subjects.forEach { id -> Box("row", onClick = { openPreview(r.tutor, month, listOf(id)) }) { Box("main") { Box("name") { Text(App.subj(id).name) }; Box("meta") { Text("посещаемость, занятия, оплаты") } }; Chip("открыть", "grp") } }
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
        org.jetbrains.compose.web.dom.P({ style("color:var(--hint);line-height:1.5") }) { Text("Все права будут сняты: бот и веб-интерфейс перестанут отвечать этому человеку. Занятия и оплаты останутся в отчётах.") }
        Actions {
            Btn("Отмена") { confirmOff = false }
            Btn("Отключить", "danger") { App.act("Доступ отключён, история занятий сохранена") { Api.postEmpty<TutorDto>("/api/tutors/${orig.id}/disable"); close() } }
        }
        return@open
    }
    val adm = "ADMIN" in perms
    Title(orig?.name ?: "Новый преподаватель")
    if (orig != null && orig.perms.isEmpty()) {
        Box("lock", "padding-top:0") { Text("Доступ отключён.") }
        Actions { Btn("Вернуть доступ", "primary") { App.act("Доступ возвращён с правами репетитора") { Api.postEmpty<TutorDto>("/api/tutors/${orig.id}/enable"); close() } } }
    }
    Box("", "display:grid;gap:8px") {
        FieldLabel("Имя"); TextField(name, "Фамилия Имя") { name = it }
        FieldLabel("Telegram ID"); TextField(tgId, "428100231", cls = "field num", readOnly = orig != null) { tgId = it }
        FieldLabel("Ставка, % от начисленного"); NumberField(rate) { rate = it }
    }
    Sec("Предметы")
    Box("opts") { App.b.subjects.forEach { s -> Opt(s.name, s.id in subjects) { subjects = if (s.id in subjects) subjects - s.id else subjects + s.id } } }
    Sec("Быстрый набор прав")
    Box("opts") { PRESETS.forEach { (t, l) -> Opt(t, l.size == perms.size && l.all { it in perms }) { perms = l } } }
    Sec("Права")
    Box("group") {
        PERMS.forEach { p ->
            val implied = adm && p.k in ADMIN_IMPLIES
            org.jetbrains.compose.web.dom.Label(attrs = { classes("perm"); if (implied) classes("implied") }) {
                org.jetbrains.compose.web.dom.Input(InputType.Checkbox) {
                    checked(p.k in perms || implied); if (implied) attr("disabled", "")
                    onChange { perms = if (it.value) perms + p.k else perms - p.k }
                }
                Span({ classes("main") }) {
                    Span({ classes("name"); style("display:block;font-weight:600") }) { Text(p.t); if (p.isNew) { Text(" "); Span({ classes("pc", "new") }) { Text("новое") } } }
                    Span({ classes("meta"); style("display:block") }) { Text(p.d + if (implied) " · входит в ADMIN" else "") }
                    org.jetbrains.compose.web.dom.Code { Text(p.k) }
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
    Box("sec", "margin-top:0") { Text(dateTime(e.at) + " · " + if (e.source == "BOT") "через бота" else "через веб") }
    Title("${e.action}: ${(ETYPES[e.type] ?: e.type).lowercase()}")
    Box("summary") {
        Box("row") { Box("main") { Text("Кто") }; Div { UserLink(e.actor, e.actorName) } }
        Box("row") { Box("main") { Text("Запись") }; Box("right", "max-width:62%") { Text(e.title) } }
        SummaryRow("ID", "${e.type} #${e.id}", true)
    }
    if (ch.isNotEmpty()) {
        if (e.type == "session") { Sec("Подробности"); Box("summary") { ch.forEach { SummaryRow(it.first, it.third) } } }
        else {
            Sec("Что изменилось")
            Box("summary", "overflow-x:auto") {
                Table({ classes("diff") }) {
                    Tr { Th { Text("Поле") }; Th { Text("Было") }; Th { Text("Стало") } }
                    ch.forEach { (k, a, b) -> Tr { Td { Text(k) }; Td({ classes("was") }) { Text(a) }; Td({ classes("now") }) { Text(b) } } }
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
        Box("group") {
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
        res = runCatching { Api.get<SearchDto>("/api/search?q=" + js("encodeURIComponent")(q.trim()) as String) }.getOrNull()
    }
    Title("Поиск")
    TextField(q, "Ученик, предмет, сумма", InputType.Search, cls = "search") { q = it }
    val r = res
    if (r == null) Lock("Ищет сразу по ученикам, занятиям" + (if (App.caps.readPayments) " и оплатам" else "") + ". Например: «соколова», «физика», «4800».")
    else if (r.students.isEmpty() && r.lessons.isEmpty() && r.payments.isEmpty()) Empty("Ничего не нашлось.")
    else {
        if (r.students.isNotEmpty()) { Sec("Ученики"); Box("group") { r.students.forEach { s -> Box("row", onClick = { close(); openStudent(s.id) }) { Avatar(s.id, sFio(s)); Box("main") { Box("name") { Text(sFio(s)) }; Box("meta") { Text("${s.cls ?: "—"} класс · ${STATUS[s.status]?.lowercase()}") } } } } } }
        if (r.lessons.isNotEmpty()) { Sec("Занятия"); Box("group") { r.lessons.forEach { l -> Box("row", onClick = { close(); openLesson(l.id) }) { Box("num", "width:54px;color:var(--hint);font-size:12px") { Text(shortDay(l.date)) }; Box("main") { Box("name") { Text(names(l)) }; Box("meta") { Text(App.subj(l.subject).name + " · " + l.time) } } } } } }
        if (r.payments.isNotEmpty()) { Sec("Оплаты"); Box("group") { r.payments.forEach { p -> Box("row", onClick = { close(); openPayment(p.id) }) { Box("num", "width:54px;color:var(--hint);font-size:12px") { Text(shortDay(p.date)) }; Box("main") { Box("name") { Text(p.student.fio) }; Box("meta") { Text(App.subj(p.subject).name) } }; if (App.money) Box("num") { Text(rub(p.amount)) } } } } }
    }
}
