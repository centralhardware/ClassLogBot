package me.centralhardware.znatoki.telegram.statistic.service

import kotliquery.Session
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.DataSource
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle
import java.time.LocalDateTime

/** One changed field of an audited record: its readable name, the value before and after. */
data class Change(val field: String, val old: Any?, val new: Any?)

/** Listens to every audit entry as it is written, e.g. to alert admins about suspicious edits. */
fun interface AuditListener {
    fun onEntry(entry: AuditEntry)
}

data class AuditEntry(
    val id: Int,
    val userId: Long,
    val action: String,
    val entityType: String,
    val entityId: String,
    val title: String,
    val changes: List<Change>,
    val source: DataSource,
    val at: LocalDateTime,
)

/**
 * Writes the audit_log that the web app's journal reads. Both the bot and the web app call it, and
 * the entry is tagged with where the change came from. A failed write is logged, never thrown:
 * losing an audit line must not undo the change it describes.
 */
object AuditService {

    @Volatile
    var listener: AuditListener? = null

    fun log(
        userId: Long,
        action: String,
        entityType: String,
        entityId: Any,
        title: String,
        changes: List<Change> = emptyList(),
        source: DataSource,
        studentId: Int? = null,
        subjectId: Long? = null,
        tx: Session? = null,
    ) {
        runCatching {
            val now = LocalDateTime.now()
            val id = runSingle(
                queryOf(
                    """
                    INSERT INTO audit_log (user_id, action, entity_type, entity_id, details, timestamp,
                                           student_id, subject_id, source, title, changes)
                    VALUES (:user_id, :action, :entity_type, :entity_id, :details, :timestamp,
                            :student_id, :subject_id, :source, :title, CAST(:changes AS jsonb))
                    RETURNING id
                    """,
                    mapOf(
                        "user_id" to userId,
                        "action" to action,
                        "entity_type" to entityType,
                        "entity_id" to entityId.toString(),
                        "details" to changes.joinToString("; ") { "${it.field}: ${it.old ?: "—"} → ${it.new ?: "—"}" },
                        "timestamp" to now,
                        "student_id" to studentId,
                        "subject_id" to subjectId?.toInt(),
                        "source" to source.value,
                        "title" to title,
                        "changes" to changesJson(changes).toString(),
                    ),
                ),
                tx,
            ) { it.int("id") }!!
            listener?.onEntry(AuditEntry(id, userId, action, entityType, entityId.toString(), title, changes, source, now))
        }.onFailure { KSLog.error("AuditService: failed to write '$action' for $entityType $entityId", it) }
    }

    fun changesJson(changes: List<Change>): JsonArray = buildJsonArray {
        changes.forEach { c ->
            add(buildJsonArray {
                add(JsonPrimitive(c.field))
                add(c.old.toJson())
                add(c.new.toJson())
            })
        }
    }

    private fun Any?.toJson() = when (this) {
        null -> JsonNull
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        else -> JsonPrimitive(toString())
    }
}
