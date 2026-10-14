package me.centralhardware.znatoki.telegram.statistic.web

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.send.media.sendDocument
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.extensions.utils.types.buttons.webAppButton
import dev.inmo.tgbotapi.requests.abstracts.InputFile
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.service.AuditEntry
import me.centralhardware.znatoki.telegram.statistic.service.AuditListener
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/** Telegram messages the web app sends: access requests, report reviews, admin alerts, files. */
class WebNotifier(private val bot: TelegramBot) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fire-and-forget: a failed Telegram call must not fail the web request that caused it. */
    fun send(chatId: Long, text: String, appButton: Pair<String, String>? = null) {
        scope.launch {
            runCatching {
                val url = appButton?.let { (_, path) -> WebConfig.appUrl?.let { it + path } }
                sendTextMessage(chatId, text, url?.let { u -> inlineKeyboard { row { webAppButton(appButton.first, u) } } })
            }.onFailure { KSLog.warning("WebNotifier: failed to message $chatId", it) }
        }
    }

    private suspend fun sendTextMessage(chatId: Long, text: String, markup: dev.inmo.tgbotapi.types.buttons.InlineKeyboardMarkup?) {
        bot.sendTextMessage(chatId.toChatId(), text, replyMarkup = markup)
    }

    suspend fun sendFile(chatId: Long, file: File) {
        bot.sendDocument(chatId.toChatId(), InputFile(file))
    }

    fun toPermission(p: Permissions, text: String, appButton: Pair<String, String>? = null, except: Long? = null) {
        TutorMapper.getAll()
            .filter { p in it.permissions && it.id.id != except }
            .forEach { send(it.id.id, text, appButton) }
    }

    fun launch(block: suspend TelegramBot.() -> Unit) {
        scope.launch { runCatching { bot.block() }.onFailure { KSLog.warning("WebNotifier: background task failed", it) } }
    }
}

/**
 * Watches the audit log and messages admins about the patterns they asked for in "Оповещения":
 * mass deletion, night-time edits, edits of a closed month and sign-ins from a new device.
 */
class AuditAlerts(private val notifier: WebNotifier) : AuditListener {

    private val deletions = ConcurrentHashMap<Long, MutableList<LocalDateTime>>()
    private val lastAlert = ConcurrentHashMap<String, LocalDateTime>()
    private val fmt = DateTimeFormatter.ofPattern("d.MM, HH:mm")

    fun settings(): AlertsDto = WebRepo.setting(KEY)?.let { runCatching { Json.decodeFromString<AlertsDto>(it) }.getOrNull() } ?: AlertsDto()

    fun save(a: AlertsDto) = WebRepo.saveSetting(KEY, Json.encodeToString(AlertsDto.serializer(), a))

    override fun onEntry(entry: AuditEntry) {
        if (entry.entityType == "session") return
        val s = settings()
        val who = TutorMapper.findByIdOrNull(me.centralhardware.znatoki.telegram.statistic.entity.TutorId(entry.userId))?.name ?: entry.userId.toString()
        val via = if (entry.source == DataSource.BOT) "в боте" else "в веб-интерфейсе"

        if (s.mass && entry.action == "Удалено") {
            val window = entry.at.minusMinutes(10)
            val list = deletions.computeIfAbsent(entry.userId) { mutableListOf() }
            val count = synchronized(list) {
                list.removeIf { it.isBefore(window) }
                list.add(entry.at)
                list.size
            }
            if (count >= 5 && throttle("mass-${entry.userId}", 10)) {
                alert(entry.userId, "⚠️ $who удалил(а) $count записей за 10 минут $via (${entry.at.format(fmt)}).")
            }
        }
        if (s.night && entry.at.hour < 7 && throttle("night-${entry.userId}", 60)) {
            alert(entry.userId, "🌙 Правка ночью: $who — «${entry.action}», ${entry.title} $via (${entry.at.format(fmt)}).")
        }
    }

    fun closedMonthEdit(actorId: Long, actorName: String, what: String, month: String) {
        if (!settings().closed) return
        alert(actorId, "🔒 $actorName изменил(а) запись закрытого месяца ($month): $what.")
    }

    fun newDevice(actorId: Long, actorName: String, device: String, ip: String) {
        if (!settings().newdev) return
        alert(actorId, "📱 $actorName вошёл(ла) в веб-интерфейс с нового устройства: $device, $ip.")
    }

    private fun alert(actorId: Long, text: String) =
        notifier.toPermission(Permissions.ADMIN, text, "Открыть журнал" to "/#tab=audit", except = actorId)

    private fun throttle(key: String, minutes: Long): Boolean {
        val now = LocalDateTime.now()
        val prev = lastAlert[key]
        if (prev != null && prev.isAfter(now.minusMinutes(minutes))) return false
        lastAlert[key] = now
        return true
    }

    companion object {
        private const val KEY = "alerts"
    }
}
