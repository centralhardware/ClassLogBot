package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.AuditLog
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle
import me.centralhardware.znatoki.telegram.statistic.extensions.update
import java.time.Instant

object AuditLogMapper {

    fun log(
        userId: Long,
        action: String,
        entityType: String? = null,
        entityId: Int? = null,
        details: String? = null,
        studentId: Int? = null,
        subjectId: Int? = null
    ) {
        update(
            queryOf(
                """
                INSERT INTO audit_log (user_id, action, entity_type, entity_id, details, timestamp, student_id, subject_id)
                VALUES (:userId, :action, :entityType, :entityId, :details, :timestamp, :studentId, :subjectId)
                """,
                mapOf(
                    "userId" to userId,
                    "action" to action,
                    "entityType" to entityType,
                    "entityId" to entityId,
                    "details" to details,
                    "timestamp" to Instant.now(),
                    "studentId" to studentId,
                    "subjectId" to subjectId
                )
            )
        )
    }

    fun getAll(limit: Int = 100, offset: Long = 0, tutorId: Long? = null, subjectId: Int? = null, action: String? = null): List<AuditLog> {
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any?>("limit" to limit, "offset" to offset)
        
        tutorId?.let {
            conditions.add("user_id = :tutorId")
            params["tutorId"] = it
        }
        
        subjectId?.let {
            conditions.add("subject_id = :subjectId")
            params["subjectId"] = it
        }
        
        action?.let {
            conditions.add("action LIKE :action")
            params["action"] = "$it%"
        }
        
        val whereClause = if (conditions.isNotEmpty()) "WHERE ${conditions.joinToString(" AND ")}" else ""
        
        return runList(
            queryOf(
                """
                SELECT id, user_id, action, entity_type, entity_id, details, timestamp, student_id, subject_id
                FROM audit_log
                $whereClause
                ORDER BY timestamp DESC
                LIMIT :limit OFFSET :offset
                """,
                params
            )
        ) { row ->
            AuditLog(
                id = row.int("id"),
                userId = row.long("user_id"),
                action = row.string("action"),
                entityType = row.stringOrNull("entity_type"),
                entityId = row.intOrNull("entity_id"),
                details = row.stringOrNull("details"),
                timestamp = row.instant("timestamp"),
                studentId = row.intOrNull("student_id"),
                subjectId = row.intOrNull("subject_id")
            )
        }
    }

    fun getByUserId(userId: Long, limit: Int = 100, offset: Long = 0, subjectId: Int? = null, action: String? = null): List<AuditLog> {
        val conditions = mutableListOf("user_id = :userId")
        val params = mutableMapOf<String, Any?>("userId" to userId, "limit" to limit, "offset" to offset)
        
        subjectId?.let {
            conditions.add("subject_id = :subjectId")
            params["subjectId"] = it
        }
        
        action?.let {
            conditions.add("action LIKE :action")
            params["action"] = "$it%"
        }
        
        return runList(
            queryOf(
                """
                SELECT id, user_id, action, entity_type, entity_id, details, timestamp, student_id, subject_id
                FROM audit_log
                WHERE ${conditions.joinToString(" AND ")}
                ORDER BY timestamp DESC
                LIMIT :limit OFFSET :offset
                """,
                params
            )
        ) { row ->
            AuditLog(
                id = row.int("id"),
                userId = row.long("user_id"),
                action = row.string("action"),
                entityType = row.stringOrNull("entity_type"),
                entityId = row.intOrNull("entity_id"),
                details = row.stringOrNull("details"),
                timestamp = row.instant("timestamp"),
                studentId = row.intOrNull("student_id"),
                subjectId = row.intOrNull("subject_id")
            )
        }
    }

    fun count(tutorId: Long? = null, subjectId: Int? = null, action: String? = null): Long {
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any?>()
        
        tutorId?.let {
            conditions.add("user_id = :tutorId")
            params["tutorId"] = it
        }
        
        subjectId?.let {
            conditions.add("subject_id = :subjectId")
            params["subjectId"] = it
        }
        
        action?.let {
            conditions.add("action LIKE :action")
            params["action"] = "$it%"
        }
        
        val whereClause = if (conditions.isNotEmpty()) "WHERE ${conditions.joinToString(" AND ")}" else ""
        
        return runSingle(
            queryOf("SELECT COUNT(*) as count FROM audit_log $whereClause", params)
        ) { row -> row.long("count") } ?: 0
    }

    fun countByUserId(userId: Long, subjectId: Int? = null, action: String? = null): Long {
        val conditions = mutableListOf("user_id = :userId")
        val params = mutableMapOf<String, Any?>("userId" to userId)
        
        subjectId?.let {
            conditions.add("subject_id = :subjectId")
            params["subjectId"] = it
        }
        
        action?.let {
            conditions.add("action LIKE :action")
            params["action"] = "$it%"
        }
        
        return runSingle(
            queryOf(
                "SELECT COUNT(*) as count FROM audit_log WHERE ${conditions.joinToString(" AND ")}",
                params
            )
        ) { row -> row.long("count") } ?: 0
    }
}
