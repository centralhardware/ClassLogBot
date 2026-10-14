package me.centralhardware.znatoki.telegram.statistic.webapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.js.Date
import kotlin.math.roundToLong

// ---------- formatting ----------
val MN = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
val MG = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
val MS = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
val WD = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
val WDF = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")
val STATUS = mapOf("active" to "Занимается", "paused" to "Пауза", "left" to "Ушёл")
val PALETTE = listOf("#2a8bd8", "#2f9e62", "#c45ab3", "#d4772c", "#5b6fd6", "#2a9d9d", "#b8913a", "#d64545")

fun rub(n: Number): String {
    val v = n.toDouble().roundToLong()
    val s = kotlin.math.abs(v).toString().reversed().chunked(3).joinToString(" ").reversed()
    return (if (v < 0) "-" else "") + s + " ₽"
}
fun monthName(m: String) = MN[m.substring(5, 7).toInt() - 1] + " " + m.substring(0, 4)
fun dayMonth(iso: String) = "${iso.substring(8, 10).toInt()} ${MG[iso.substring(5, 7).toInt() - 1]}"
fun shortDay(iso: String) = "${iso.substring(8, 10).toInt()} ${MS[iso.substring(5, 7).toInt() - 1]}"
fun dateTime(iso: String) = dayMonth(iso.substring(0, 10)) + ", " + iso.substring(11, 16)
/** 0 = Monday. */
fun dow(iso: String) = (Date(iso + "T12:00:00").getDay() + 6) % 7
fun daysBetween(a: String, b: String) = ((Date(b + "T12:00:00").getTime() - Date(a + "T12:00:00").getTime()) / 864e5).roundToLong().toInt()
fun daysInMonth(m: String) = Date(m.substring(0, 4).toInt(), m.substring(5, 7).toInt(), 0).getDate()
fun fmtPhone(p: String?) = if (p.isNullOrEmpty() || p.length < 11) "—" else "+7 ${p.substring(1, 4)} ${p.substring(4, 7)}-${p.substring(7, 9)}-${p.substring(9)}"
fun k1(v: Double) = ((v / 100).roundToLong() / 10.0).toString().replace(".", ",") + "к"
fun inMonth(m: String) = MG[m.substring(5, 7).toInt() - 1].let { it.dropLast(1) + if (it.endsWith("та")) "те" else "е" }

// ---------- app state ----------
object App {
    var boot by mutableStateOf<BootDto?>(null)
    var tab by mutableStateOf("today")
    var month by mutableStateOf("")
    var who by mutableStateOf("me")
    /** Bumped after every change so that every loaded list refetches. */
    var version by mutableStateOf(0)
    val scope: CoroutineScope = MainScope()

    val b get() = boot!!
    val caps get() = b.caps
    val me get() = b.me.id
    val today get() = b.today
    val currentMonth get() = b.today.substring(0, 7)
    val seesAll get() = caps.seesAll
    val money get() = caps.seesAll

    fun subj(id: Long) = b.subjects.find { it.id == id } ?: SubjectDto(id, "#$id", false)
    fun tutorName(id: Long) = b.tutors.find { it.id == id }?.name ?: "#$id"
    fun tutorShort(id: Long) = tutorName(id).split(" ").let { it.getOrNull(1) ?: it[0] }
    fun teaching() = b.tutors.filter { it.teaching && it.active }

    fun refresh() { version++ }

    /** Runs a change: errors become a toast, success refreshes the data. */
    fun act(done: String? = null, block: suspend () -> Unit) {
        scope.launch {
            try {
                block(); refresh(); done?.let { Toasts.show(it) }
                tg?.HapticFeedback?.notificationOccurred("success")
            } catch (e: Throwable) {
                Toasts.show(e.message ?: "Ошибка"); tg?.HapticFeedback?.notificationOccurred("error")
            }
        }
    }
}

fun price(l: LessonDto) = l.amount * if (l.half) 1.5 else 1.0
fun isGroup(l: LessonDto) = l.students.size > 1 || l.forceGroup
fun names(l: LessonDto) = l.students.joinToString(", ") { it.fio }
fun sFio(s: StudentRowDto) = listOf(s.last, s.first).filter { it.isNotBlank() }.joinToString(" ")

sealed interface Remote<out T> {
    data object Loading : Remote<Nothing>
    data class Ok<T>(val data: T) : Remote<T>
    data class Fail(val error: Throwable) : Remote<Nothing>
}

/** Loads [load] and reloads it whenever [keys] change or data is refreshed after a change. */
@Composable
fun <T> remote(vararg keys: Any?, load: suspend () -> T): Remote<T> {
    val r = produceState<Remote<T>>(Remote.Loading, *keys, App.version) {
        value = try { Remote.Ok(load()) } catch (e: Throwable) { Remote.Fail(e) }
    }
    return r.value
}

object Toasts {
    val items = mutableStateListOf<Pair<Int, String>>()
    var undo by mutableStateOf<Triple<String, Int, () -> Unit>?>(null)
    private var seq = 0
    fun show(msg: String) {
        val id = ++seq; items.add(id to msg)
        window.setTimeout({ items.removeAll { it.first == id } }, 2200)
    }
    fun withUndo(msg: String, onUndo: () -> Unit) {
        undo = Triple(msg, 10, onUndo)
        fun tick() {
            val u = undo ?: return
            if (u.second <= 1) { undo = null; return }
            undo = u.copy(second = u.second - 1); window.setTimeout({ tick() }, 1000)
        }
        window.setTimeout({ tick() }, 1000)
    }
}

/** Bottom sheets, newest last; Telegram's BackButton closes the top one. */
object Sheets {
    val stack = mutableStateListOf<@Composable (close: () -> Unit) -> Unit>()
    fun open(content: @Composable (close: () -> Unit) -> Unit) { stack.add(content); sync() }
    fun closeTop() { if (stack.isNotEmpty()) stack.removeAt(stack.size - 1); sync() }
    fun closeAll() { stack.clear(); sync() }
    fun sync() { tg?.BackButton?.let { if (stack.isEmpty()) it.hide() else it.show() } }
}

var viewImage by mutableStateOf<Pair<String, String>?>(null)
