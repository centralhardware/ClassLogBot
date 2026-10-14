package me.centralhardware.znatoki.telegram.statistic.web.service

import kotliquery.Row
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.extensions.runSingle
import me.centralhardware.znatoki.telegram.statistic.web.AuditDto
import me.centralhardware.znatoki.telegram.statistic.web.AuditPageDto
import me.centralhardware.znatoki.telegram.statistic.web.WebServer
import java.io.File
import java.nio.file.Files

/** Which journal entries to show: one entity type and a text search, or the history of one record. */
data class AuditFilter(val type: String? = null, val query: String? = null, val recordType: String? = null, val recordId: String? = null) {

    fun where(): Pair<String, Map<String, Any?>> {
        // Entries from before the web app have no source and render badly, so they are left out.
        val clauses = mutableListOf("a.source IS NOT NULL")
        val params = mutableMapOf<String, Any?>()
        if (recordType != null && recordId != null) {
            clauses += "a.entity_type = :rtype AND a.entity_id = :rid"; params["rtype"] = recordType; params["rid"] = recordId
        } else if (type != null && type != "all") {
            clauses += "a.entity_type = :etype"; params["etype"] = type
        }
        if (!query.isNullOrBlank()) {
            clauses += "(coalesce(a.title, '') || ' ' || coalesce(t.name, '') || ' ' || a.action) ILIKE :search"; params["search"] = "%${query.trim()}%"
        }
        return clauses.joinToString(" AND ") to params
    }
}

/** The audit journal: paging through it and exporting it as CSV. */
object AuditLogService {

    private const val FROM = "FROM audit_log a LEFT JOIN tutors t ON t.id = a.user_id"

    fun page(filter: AuditFilter, limit: Int, offset: Int): AuditPageDto {
        val (where, params) = filter.where()
        val total = runSingle(queryOf("SELECT count(*) AS n $FROM WHERE $where", params)) { it.int("n") } ?: 0
        val items = runList(
            queryOf("SELECT a.*, t.name AS actor $FROM WHERE $where ORDER BY a.id DESC LIMIT ${limit.coerceIn(1, 200)} OFFSET ${offset.coerceAtLeast(0)}", params)
        ) { it.toAudit() }
        return AuditPageDto(total, items)
    }

    fun csv(filter: AuditFilter): File {
        val (where, params) = filter.where()
        val rows = runList(queryOf("SELECT a.*, t.name AS actor $FROM WHERE $where ORDER BY a.id DESC", params)) { it.toAudit() }
        val esc = { s: String -> "\"" + s.replace("\"", "\"\"") + "\"" }
        val file = Files.createTempDirectory("classlog").resolve("audit_log.csv").toFile()
        file.bufferedWriter().use { w ->
            w.write("﻿id;время;кто;источник;действие;тип;запись;название;изменения\n")
            rows.forEach { a ->
                w.write(listOf(a.n.toString(), a.at.replace('T', ' '), a.actorName, a.source, a.action, a.type, a.id, a.title, a.changes?.toString() ?: "")
                    .joinToString(";") { esc(it) } + "\n")
            }
        }
        return file
    }

    private fun Row.toAudit() = AuditDto(
        n = int("id"),
        at = localDateTime("timestamp").toString(),
        actor = long("user_id"),
        actorName = stringOrNull("actor") ?: long("user_id").toString(),
        source = string("source"),
        action = string("action"),
        type = stringOrNull("entity_type") ?: "",
        id = stringOrNull("entity_id") ?: "",
        title = stringOrNull("title") ?: stringOrNull("details") ?: "",
        changes = stringOrNull("changes")?.let { WebServer.json.parseToJsonElement(it) },
    )
}
