package me.centralhardware.znatoki.telegram.statistic.extensions

import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import kotliquery.sessionOf
import me.centralhardware.znatoki.telegram.statistic.dataSource

/*
 * Each helper runs on [session] when given (an open transaction, so the call joins it) and otherwise
 * on its own auto-commit session.
 */

inline fun <T> withSession(session: Session?, block: (Session) -> T): T =
    session?.let(block) ?: sessionOf(dataSource).use(block)

inline fun <T> runSingle(q: Query, session: Session? = null, crossinline map: (Row) -> T): T? =
    withSession(session) { it.run(q.map { row -> map(row) }.asSingle) }

inline fun <T> runList(q: Query, session: Session? = null, crossinline map: (Row) -> T): List<T> =
    withSession(session) { it.run(q.map { row -> map(row) }.asList) }

fun execute(q: Query, session: Session? = null): Boolean = withSession(session) { it.execute(q) }

fun update(q: Query, session: Session? = null): Int = withSession(session) { it.update(q) }

fun <T> transaction(block: (kotliquery.TransactionalSession) -> T): T =
    sessionOf(dataSource).use { session ->
        session.transaction(block)
    }
