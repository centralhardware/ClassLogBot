package me.centralhardware.znatoki.telegram.statistic.web

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

    /** Public https address of the Mini App; without it the bot shows no app buttons. */
    val appUrl: String? by lazy { System.getenv("WEB_APP_URL")?.trimEnd('/')?.takeIf { it.isNotBlank() } }

    val enabled: Boolean by lazy { System.getenv("WEB_ENABLED")?.lowercase() != "false" }

    val botToken: String by lazy { System.getenv("BOT_TOKEN") ?: error("BOT_TOKEN is not set") }

    /** How old Telegram's initData may be when a new session is opened from it. */
    const val INIT_DATA_MAX_AGE_SECONDS = 24 * 60 * 60L
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
        val fields = initData.split("&").mapNotNull { pair ->
            val i = pair.indexOf('=')
            if (i <= 0) null
            else URLDecoder.decode(pair.substring(0, i), StandardCharsets.UTF_8) to
                URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8)
        }.toMap()
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

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(data)

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}

data class WebSession(
    val id: UUID,
    val tutorId: Long,
    val device: String?,
    val ip: String?,
    val createdAt: LocalDateTime,
    val lastSeenAt: LocalDateTime,
    val revoked: Boolean,
)

object SessionStore {

    private fun kotliquery.Row.toSession() = WebSession(
        uuid("id"), long("tutor_id"), stringOrNull("device"), stringOrNull("ip"),
        localDateTime("created_at"), localDateTime("last_seen_at"), boolean("revoked"),
    )

    fun findByInitData(hash: String): WebSession? = runSingle(
        queryOf("SELECT * FROM web_sessions WHERE init_data_hash = :h ORDER BY created_at DESC LIMIT 1", mapOf("h" to hash))
    ) { it.toSession() }

    fun create(tutorId: Long, initDataHash: String, device: String, ip: String): WebSession {
        val id = UUID.randomUUID()
        update(
            queryOf(
                """
                INSERT INTO web_sessions (id, tutor_id, init_data_hash, device, ip)
                VALUES (:id, :tutor, :hash, :device, :ip)
                """,
                mapOf("id" to id, "tutor" to tutorId, "hash" to initDataHash, "device" to device, "ip" to ip),
            )
        )
        return find(id)!!
    }

    fun find(id: UUID): WebSession? =
        runSingle(queryOf("SELECT * FROM web_sessions WHERE id = :id", mapOf("id" to id))) { it.toSession() }

    fun touch(id: UUID) = update(queryOf("UPDATE web_sessions SET last_seen_at = now() WHERE id = :id", mapOf("id" to id)))

    fun active(tutorId: Long): List<WebSession> = runList(
        queryOf(
            """
            SELECT * FROM web_sessions
            WHERE tutor_id = :t AND NOT revoked AND last_seen_at > now() - interval '7 days'
            ORDER BY last_seen_at DESC
            """,
            mapOf("t" to tutorId),
        )
    ) { it.toSession() }

    fun knownDevice(tutorId: Long, device: String): Boolean = runSingle(
        queryOf("SELECT EXISTS(SELECT 1 FROM web_sessions WHERE tutor_id = :t AND device = :d) AS e", mapOf("t" to tutorId, "d" to device))
    ) { it.boolean("e") } ?: false

    fun lastLogin(tutorId: Long): WebSession? = runSingle(
        queryOf("SELECT * FROM web_sessions WHERE tutor_id = :t ORDER BY created_at DESC LIMIT 1", mapOf("t" to tutorId))
    ) { it.toSession() }

    fun revoke(id: UUID) = update(queryOf("UPDATE web_sessions SET revoked = true WHERE id = :id", mapOf("id" to id)))

    fun revokeAll(tutorId: Long): Int =
        update(queryOf("UPDATE web_sessions SET revoked = true WHERE tutor_id = :t AND NOT revoked", mapOf("t" to tutorId)))

    /** Sessions are valid for a week of inactivity; the app then opens a new one from fresh initData. */
    fun isUsable(s: WebSession) = !s.revoked && s.lastSeenAt.isAfter(LocalDateTime.now().minusDays(7))
}

/**
 * The signed-in tutor of a request, with the screen-level capabilities derived from tutors.permissions,
 * mirroring UserExt.kt (hasTimePermission() = ADD_TIME or ADMIN, and so on).
 */
class Caller(val tutor: Tutor, val session: WebSession) {
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
        fun load(session: WebSession): Caller? =
            TutorMapper.findByIdOrNull(TutorId(session.tutorId))
                ?.takeIf { Permissions.WEB_INTERFACE in it.permissions }
                ?.let { Caller(it, session) }
    }
}
