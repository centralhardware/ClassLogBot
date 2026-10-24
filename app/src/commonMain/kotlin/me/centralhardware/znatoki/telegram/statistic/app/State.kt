package me.centralhardware.znatoki.telegram.statistic.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlin.math.roundToLong

// ---------- formatting ----------
val MN = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
val MG = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
val MS = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
val WD = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
val WDF = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")
val STATUS = mapOf("active" to "Занимается", "paused" to "Пауза", "left" to "Ушёл")

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
fun dow(iso: String) = LocalDate.parse(iso).dayOfWeek.isoDayNumber - 1
fun daysBetween(a: String, b: String) = LocalDate.parse(a).daysUntil(LocalDate.parse(b))
fun daysInMonth(m: String) = YearMonth.parse(m).numberOfDays
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
                tg?.haptic(true)
            } catch (e: Throwable) {
                Toasts.show(e.message ?: "Ошибка"); tg?.haptic(false)
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
        App.scope.launch { delay(2200); items.removeAll { it.first == id } }
    }
    fun withUndo(msg: String, onUndo: () -> Unit) {
        val u0 = Triple(msg, 10, onUndo)
        undo = u0
        App.scope.launch {
            while (true) {
                delay(1000)
                val u = undo ?: return@launch
                if (u.third !== onUndo) return@launch
                if (u.second <= 1) { undo = null; return@launch }
                undo = u.copy(second = u.second - 1)
            }
        }
    }
}

/** Bottom sheets, newest last; Telegram's BackButton (or the system back) closes the top one. */
object Sheets {
    val stack = mutableStateListOf<@Composable (close: () -> Unit) -> Unit>()
    fun open(content: @Composable (close: () -> Unit) -> Unit) { stack.add(content); sync() }
    fun close(content: @Composable (close: () -> Unit) -> Unit) { stack.remove(content); sync() }
    fun closeTop() { if (stack.isNotEmpty()) stack.removeAt(stack.size - 1); sync() }
    fun closeAll() { stack.clear(); sync() }
    fun sync() { tg?.backButton(stack.isNotEmpty()) }
}

/** A photo opened full screen: the loader and its caption. */
var viewImage by mutableStateOf<Pair<Pair<String, String>, String>?>(null)

/** The user's interface size: a density multiplier on the whole app, remembered on this device. */
object UiScale {
    private const val KEY = "ui-scale"
    val options = listOf(0.85f to "85%", 1f to "100%", 1.15f to "115%", 1.3f to "130%", 1.5f to "150%")
    var value by mutableStateOf(platform.load(KEY)?.toFloatOrNull()?.takeIf { it in 0.5f..2f } ?: 1f)
        private set

    fun set(v: Float) {
        value = v
        platform.save(KEY, v.toString())
    }
}
