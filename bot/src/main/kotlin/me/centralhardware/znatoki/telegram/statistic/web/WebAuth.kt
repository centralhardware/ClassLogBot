package me.centralhardware.znatoki.telegram.statistic.web

import kotliquery.Session
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.Permissions
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle
import me.centralhardware.znatoki.telegram.statistic.extensions.update
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object WebConfig {
    val port: Int by lazy { System.getenv("WEB_PORT")?.toIntOrNull() ?: 8080 }

    val enabled: Boolean by lazy { System.getenv("WEB_ENABLED")?.lowercase() != "false" }

    val botToken: String by lazy { System.getenv("BOT_TOKEN") ?: error("BOT_TOKEN is not set") }

    /** How old Telegram's initData may be; the app is reopened from the bot after that. */
    const val INIT_DATA_MAX_AGE_SECONDS = 24 * 60 * 60L

    /** How long a sign-in outside Telegram (bot or Login Widget) lasts before the login screen comes back. */
    const val LOGIN_MAX_AGE_SECONDS = 30 * 24 * 60 * 60L

    /** The bot's @username for the sign-in screen: BOT_USERNAME, else what getMe returned at startup. */
    @Volatile var botUsername: String? = System.getenv("BOT_USERNAME")?.removePrefix("@")?.takeIf { it.isNotBlank() }

    /** The Android app serves the bundled frontend from this origin (WebViewAssetLoader) and calls the API cross-origin. */
    const val ANDROID_APP_HOST = "appassets.androidplatform.net"
}

/** The Telegram user that signed a Mini App launch, as read from validated initData. */
data class TelegramUser(val id: Long, val firstName: String, val lastName: String?, val username: String?) {
    val displayName get() = listOfNotNull(firstName, lastName).joinToString(" ")
}

data class ValidatedInitData(val user: TelegramUser, val authDate: Long, val hash: String)

/**
 * Checks Telegram Mini App initData as described in
 * https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app:
 * the hash must be HMAC-SHA256 of the sorted fields, keyed with HMAC-SHA256("WebAppData", bot token).
 */
object InitDataValidator {

    fun validate(initData: String, botToken: String = WebConfig.botToken, now: Long = Instant.now().epochSecond): ValidatedInitData? {
        val fields = parseQuery(initData)
        val hash = fields["hash"] ?: return null
        val checkString = fields.filterKeys { it != "hash" }.toSortedMap().entries.joinToString("\n") { "${it.key}=${it.value}" }

        val secret = hmac("WebAppData".toByteArray(), botToken.toByteArray())
        val expected = hmac(secret, checkString.toByteArray()).toHex()
        if (!MessageDigest.isEqual(expected.toByteArray(), hash.lowercase().toByteArray())) return null

        val authDate = fields["auth_date"]?.toLongOrNull() ?: return null
        if (now - authDate > WebConfig.INIT_DATA_MAX_AGE_SECONDS) return null

        val user = runCatching { Json.parseToJsonElement(fields["user"] ?: return null).jsonObject }.getOrNull() ?: return null
        val id = user["id"]?.jsonPrimitive?.longOrNull ?: return null
        return ValidatedInitData(
            TelegramUser(
                id,
                user["first_name"]?.jsonPrimitive?.content ?: "",
                user["last_name"]?.jsonPrimitive?.content,
                user["username"]?.jsonPrimitive?.content,
            ),
            authDate,
            hash.lowercase(),
        )
    }

    internal fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(data)

    internal fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    internal fun parseQuery(query: String): Map<String, String> = query.split("&").mapNotNull { pair ->
        val i = pair.indexOf('=')
        if (i <= 0) null
        else URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8) to
            URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8)
    }.toMap()
}

/**
 * Checks Telegram Login Widget data, the credential of browsers signed in before the bot sign-in replaced the widget,
 * as described in https://core.telegram.org/widgets/login#checking-authorization:
 * the hash must be HMAC-SHA256 of the sorted fields, keyed with SHA256(bot token).
 * The client keeps the widget's fields as a query string and sends them like initData.
 */
object LoginWidgetValidator {

    fun validate(data: String, botToken: String = WebConfig.botToken, now: Long = Instant.now().epochSecond): TelegramUser? {
        val fields = InitDataValidator.parseQuery(data)
        val hash = fields["hash"] ?: return null
        val checkString = fields.filterKeys { it != "hash" }.toSortedMap().entries.joinToString("\n") { "${it.key}=${it.value}" }

        val secret = MessageDigest.getInstance("SHA-256").digest(botToken.toByteArray())
        val expected = with(InitDataValidator) { hmac(secret, checkString.toByteArray()).toHex() }
        if (!MessageDigest.isEqual(expected.toByteArray(), hash.lowercase().toByteArray())) return null

        val authDate = fields["auth_date"]?.toLongOrNull() ?: return null
        if (now - authDate > WebConfig.LOGIN_MAX_AGE_SECONDS) return null

        val id = fields["id"]?.toLongOrNull() ?: return null
        return TelegramUser(id, fields["first_name"] ?: "", fields["last_name"], fields["username"])
    }
}

/**
 * Sign-in through the bot, for the Android app and browsers: the app asks for a one-time token, opens
 * `t.me/<bot>?start=login_<token>`, and polls until the bot sees /start with it. The credential it then gets is
 * `tgb <id>.<auth_date>.<hmac>`, signed with a key derived from the bot token and valid for LOGIN_MAX_AGE_SECONDS.
 */
object BotLogin {
    const val PREFIX = "login_"
    private const val TOKEN_TTL_SECONDS = 10 * 60L

    private class Pending(val created: Long, @Volatile var user: TelegramUser? = null)

    private val pending = java.util.concurrent.ConcurrentHashMap<String, Pending>()

    fun start(now: Long = Instant.now().epochSecond): String {
        pending.entries.removeIf { now - it.value.created > TOKEN_TTL_SECONDS }
        return UUID.randomUUID().toString().replace("-", "").also { pending[it] = Pending(now) }
    }

    /** The bot saw /start login_<token> from [user]; false when the token is unknown or expired. */
    fun confirm(token: String, user: TelegramUser, now: Long = Instant.now().epochSecond): Boolean {
        val p = pending[token]?.takeIf { now - it.created <= TOKEN_TTL_SECONDS } ?: return false
        p.user = user
        return true
    }

    /** The credential once the bot confirmed [token] (handed out once), null while waiting. */
    fun poll(token: String, now: Long = Instant.now().epochSecond, botToken: String = WebConfig.botToken): String? {
        val user = pending[token]?.user ?: return null
        pending.remove(token)
        return "tgb " + sign(user.id, now, botToken)
    }

    fun sign(userId: Long, authDate: Long, botToken: String = WebConfig.botToken): String {
        val body = "$userId.$authDate"
        return "$body." + with(InitDataValidator) { hmac(key(botToken), body.toByteArray()).toHex() }
    }

    fun validate(data: String, botToken: String = WebConfig.botToken, now: Long = Instant.now().epochSecond): TelegramUser? {
        val parts = data.split(".")
        if (parts.size != 3) return null
        val id = parts[0].toLongOrNull() ?: return null
        val authDate = parts[1].toLongOrNull() ?: return null
        if (!MessageDigest.isEqual(sign(id, authDate, botToken).toByteArray(), data.lowercase().toByteArray())) return null
        if (now - authDate > WebConfig.LOGIN_MAX_AGE_SECONDS) return null
        return TelegramUser(id, "", null, null)
    }

    private fun key(botToken: String) = InitDataValidator.hmac("ClassLogBotLogin".toByteArray(), botToken.toByteArray())
}

/** The user behind a credential: `tma <initData>` inside Telegram, `tlw <login widget data>` or `tgb <bot login>` outside it. */
object TelegramAuth {
    fun verify(credential: String): TelegramUser? = when {
        credential.startsWith("tma ") -> InitDataValidator.validate(credential.removePrefix("tma "))?.user
        credential.startsWith("tlw ") -> LoginWidgetValidator.validate(credential.removePrefix("tlw "))
        credential.startsWith("tgb ") -> BotLogin.validate(credential.removePrefix("tgb "))
        else -> null
    }
}

/**
 * The signed-in tutor of a request, with the screen-level capabilities derived from tutors.permissions,
 * mirroring UserExt.kt (hasTimePermission() = ADD_TIME or ADMIN, and so on).
 */
class Caller(val tutor: Tutor) {
    val id: Long get() = tutor.id.id
    val tutorId: TutorId get() = tutor.id
    private val perms = tutor.permissions.toSet()

    val isAdmin = Permissions.ADMIN in perms
    /** The director reads everything and reviews reports, but changes no records. */
    val isDirector = Permissions.DIRECTOR in perms && !isAdmin
    val seesAll = isAdmin || isDirector

    val canTime = !isDirector && (Permissions.ADD_TIME in perms || isAdmin)
    val canPayment = !isDirector && (Permissions.ADD_PAYMENT in perms || isAdmin)
    val readsPayments = canPayment || isDirector
    val canClient = !isDirector && (Permissions.ADD_CLIENT in perms || isAdmin)
    val canTimeForOthers = canTime && (Permissions.ADD_TIME_FOR_OTHERS in perms || isAdmin)
    val canPaymentForOthers = canPayment && (Permissions.ADD_PAYMENT_FOR_OTHERS in perms || isAdmin)
    val canForceGroup = !isDirector && (Permissions.FORCE_GROUP in perms || isAdmin)
    val canExtraHalfHour = !isDirector && (Permissions.EXTRA_HALF_HOUR in perms || isAdmin)
    val canManageTutors = Permissions.MANAGE_TUTORS in perms || isAdmin
    val seesStudents = canClient || isDirector || canTime || canPayment

    /** Why the caller may not change a record of [recordTutor] dated in [recordMonth], or null when they may. */
    fun editDenied(recordMonth: java.time.YearMonth, recordTutor: Long, hasPerm: Boolean): String? = when {
        isAdmin -> null
        isDirector -> "Директор только просматривает данные."
        recordMonth != java.time.YearMonth.now() -> "Месяц закрыт, запись изменить нельзя. Это может сделать админ."
        !(hasPerm && recordTutor == id) -> "Изменить запись может только её репетитор или админ."
        else -> null
    }

    companion object {
        /** The tutor behind validated initData, or null without WEB_INTERFACE. */
        context(_: Session)
        fun load(userId: Long): Caller? =
            TutorMapper.findByIdOrNull(TutorId(userId))
                ?.takeIf { Permissions.WEB_INTERFACE in it.permissions }
                ?.let { Caller(it) }
    }
}
