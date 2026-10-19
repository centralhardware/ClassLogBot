package me.centralhardware.znatoki.telegram.statistic.web.service

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.krontab.buildSchedule
import dev.inmo.krontab.utils.asTzFlowWithDelays
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.chat.get.getDefaultChatMenuButton
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.webAppButton
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.MenuButton
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.AuditEntry
import me.centralhardware.znatoki.telegram.statistic.service.AuditListener
import me.centralhardware.znatoki.telegram.statistic.web.AlertsDto
import me.centralhardware.znatoki.telegram.statistic.web.WebRepo
import me.centralhardware.znatoki.telegram.statistic.web.monthTitle
import me.centralhardware.znatoki.telegram.statistic.web.subjectNames
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * Telegram messages from the web app to its users, each with a button that opens the app on the
 * section it is about ([link] is the app's hash route, e.g. "tab=report").
 */
object Notifications {

    lateinit var bot: TelegramBot
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fire-and-forget: a failed Telegram call must not fail the request that caused it. */
    fun send(tutorId: Long, text: String, link: String? = null) {
        scope.launch {
            runCatching {
                val url = link?.let { l -> AppUrl.get(bot)?.let { "$it/#$l" } }
                bot.sendTextMessage(tutorId.toChatId(), text, replyMarkup = url?.let { inlineKeyboard { row { webAppButton("Открыть в приложении", it) } } })
            }.onFailure { KSLog.warning("Notifications: failed to message $tutorId", it) }
        }
    }

    /** To everyone holding [permission], optionally except the person who caused it. */
    fun sendTo(permission: Permissions, text: String, link: String? = null, except: Long? = null) =
        TutorMapper.getAll()
            .filter { permission in it.permissions && it.id.id != except }
            .forEach { send(it.id.id, text, link) }

    /** Runs Telegram work (log-chat posts, Firefly export) after the response, off the request. */
    fun background(block: suspend TelegramBot.() -> Unit) {
        scope.launch { runCatching { bot.block() }.onFailure { KSLog.warning("Notifications: background task failed", it) } }
    }

    suspend fun sendFile(tutorId: Long, file: java.io.File) = bot.sendDocument(tutorId.toChatId(), InputFile(file))
}

/**
 * The Mini App's address, as set for the bot's menu button in BotFather; buttons that open the app
 * use it. Re-read hourly, so changing it in BotFather needs no restart.
 */
object AppUrl {
    private var cached: String? = null
    private var readAt = LocalDateTime.MIN

    suspend fun get(bot: TelegramBot): String? {
        if (readAt.isAfter(LocalDateTime.now().minusHours(1))) return cached
        cached = runCatching { (bot.getDefaultChatMenuButton() as? MenuButton.WebApp)?.webApp?.url?.trimEnd('/') }
            .onFailure { KSLog.warning("AppUrl: cannot read the menu button", it) }.getOrNull()
        readAt = LocalDateTime.now()
        return cached
    }
}

/**
 * Watches the audit log and notifies admins about what they asked for in "Оповещения":
 * mass deletion, night-time edits and edits of a closed month.
 */
object AuditAlerts : AuditListener {

    private const val KEY = "alerts"
    private val deletions = ConcurrentHashMap<Long, MutableList<LocalDateTime>>()
    private val lastAlert = ConcurrentHashMap<String, LocalDateTime>()
    private val fmt = DateTimeFormatter.ofPattern("d.MM, HH:mm")

    /** Read once and kept until [save]: every audit entry consults it, a bulk edit makes hundreds. */
    @Volatile private var cached: AlertsDto? = null

    fun settings(): AlertsDto = cached
        ?: (WebRepo.setting(KEY)?.let { runCatching { Json.decodeFromString<AlertsDto>(it) }.getOrNull() } ?: AlertsDto()).also { cached = it }

    fun save(a: AlertsDto) {
        WebRepo.saveSetting(KEY, Json.encodeToString(AlertsDto.serializer(), a))
        cached = a
    }

    override fun onEntry(entry: AuditEntry) {
        val s = settings()
        val who = TutorMapper.findByIdOrNull(TutorId(entry.userId))?.name ?: entry.userId.toString()
        val via = if (entry.source == DataSource.BOT) "в боте" else "в веб-интерфейсе"
        if (s.mass && entry.action == "Удалено") {
            val count = recentDeletions(entry.userId, entry.at)
            if (count >= 5 && once("mass-${entry.userId}", 10)) {
                alert(entry.userId, "⚠️ $who удалил(а) $count записей за 10 минут $via (${entry.at.format(fmt)}).")
            }
        }
        if (s.night && entry.at.hour < 7 && once("night-${entry.userId}", 60)) {
            alert(entry.userId, "🌙 Правка ночью: $who — «${entry.action}», ${entry.title} $via (${entry.at.format(fmt)}).")
        }
    }

    fun closedMonthEdit(actorId: Long, actorName: String, what: String, month: YearMonth) {
        if (settings().closed) alert(actorId, "🔒 $actorName изменил(а) запись закрытого месяца (${monthTitle(month).lowercase()}): $what.")
    }

    private fun recentDeletions(userId: Long, at: LocalDateTime): Int {
        val list = deletions.computeIfAbsent(userId) { mutableListOf() }
        val count = synchronized(list) {
            list.removeIf { it.isBefore(at.minusMinutes(10)) }
            list.add(at)
            list.size
        }
        prune(at)
        return count
    }

    /** Drops alert timestamps older than the longest window (60 min), so the maps don't grow forever. */
    private fun prune(now: LocalDateTime) {
        val cutoff = now.minusMinutes(60)
        lastAlert.entries.removeIf { it.value.isBefore(cutoff) }
        deletions.entries.removeIf { (_, list) -> synchronized(list) { list.all { it.isBefore(cutoff) } } }
    }

    private fun alert(actorId: Long, text: String) = Notifications.sendTo(Permissions.ADMIN, text, "tab=audit", except = actorId)

    /** True at most once per [minutes] for [key], so a burst of edits makes one alert. */
    private fun once(key: String, minutes: Long): Boolean {
        val now = LocalDateTime.now()
        if (lastAlert[key]?.isAfter(now.minusMinutes(minutes)) == true) return false
        lastAlert[key] = now
        return true
    }
}

/**
 * Evening reminders from "Напоминания": at the hour a tutor chose, the day's schedule slots that are
 * still unmarked, and two days before the month ends a nudge to check lessons and hand in the report.
 */
object Reminders {

    suspend fun run() {
        buildSchedule("0 0 20,21,22 * * *").asTzFlowWithDelays().collect {
            runCatching { send(LocalTime.now().hour) }.onFailure { KSLog.warning("Reminders failed", it) }
        }
    }

    private fun send(hour: Int) {
        val at = "%02d:00".format(hour)
        val today = LocalDate.now()
        val names = WebRepo.studentNames()
        val subjects = subjectNames()
        val slots = WebRepo.slots(null, names)
        val marked = WebRepo.marks(today, today, slots.map { it.id }).map { it.slot }.toSet()

        TutorMapper.getAll()
            .filter { Permissions.WEB_INTERFACE in it.permissions && (Permissions.ADD_TIME in it.permissions || Permissions.ADMIN in it.permissions) }
            .forEach { tutor ->
                val r = WebRepo.reminders(tutor.id.id)
                if (r.at != at) return@forEach
                if (r.evening) {
                    val pending = WebRepo.slotsOn(today, slots.filter { it.tutor == tutor.id.id }).filter { it.id !in marked }.sortedBy { it.time }
                    if (pending.isNotEmpty()) Notifications.send(
                        tutor.id.id,
                        "🕘 Сегодня по расписанию не отмечено: " + pending.joinToString("; ") {
                            "${it.time} ${subjects[it.subject]} — ${it.students.joinToString(", ") { s -> s.fio }}"
                        },
                        "tab=schedule",
                    )
                }
                val month = YearMonth.from(today)
                if (r.monthEnd && month.atEndOfMonth().minusDays(2) == today) Notifications.send(
                    tutor.id.id,
                    "📅 До конца месяца 2 дня. Проверьте занятия за ${monthTitle(month).lowercase()} и сдайте отчёт директору.",
                    "tab=report",
                )
            }
    }
}
