package me.centralhardware.znatoki.telegram.statistic.extensions

import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import me.centralhardware.znatoki.telegram.statistic.dataSource

/*
 * Every query helper runs on the transaction opened by [inTransaction] on this thread, if there is one,
 * and otherwise on a fresh auto-commit session. So a service can wrap several mapper calls (insert,
 * schedule mark, audit row) in one transaction without the mappers knowing about it.
 */

@PublishedApi
internal val current = ThreadLocal<TransactionalSession?>()

@PublishedApi
internal inline fun <T> withSession(block: (Session) -> T): T =
    current.get()?.let(block) ?: sessionOf(dataSource).use(block)

inline fun <T> runSingle(q: Query, crossinline map: (Row) -> T): T? =
    withSession { it.run(q.map { row -> map(row) }.asSingle) }

inline fun <T> runList(q: Query, crossinline map: (Row) -> T): List<T> =
    withSession { it.run(q.map { row -> map(row) }.asList) }

fun execute(q: Query): Boolean = withSession { it.execute(q) }

fun update(q: Query): Int = withSession { it.update(q) }

/** Runs [block] in one transaction; nested calls join the outer one. Must not suspend inside. */
fun <T> inTransaction(block: () -> T): T {
    if (current.get() != null) return block()
    return sessionOf(dataSource).use { session ->
        session.transaction { tx ->
            current.set(tx)
            try { block() } finally { current.remove() }
        }
    }
}

fun <T> transaction(block: (TransactionalSession) -> T): T =
    current.get()?.let(block) ?: sessionOf(dataSource).use { session -> session.transaction(block) }
