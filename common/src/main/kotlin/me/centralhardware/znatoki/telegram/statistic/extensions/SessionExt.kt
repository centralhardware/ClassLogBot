package me.centralhardware.znatoki.telegram.statistic.extensions

import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import kotliquery.sessionOf
import me.centralhardware.znatoki.telegram.statistic.dataSource

/*
 * Every DB function takes the session as a context parameter. Entry points (a route, a bot handler,
 * a job) open it with [db], and everything called inside runs on it without passing it around;
 * [transaction] groups some of those calls into one commit.
 *
 * A session must not outlive its block: a long wait (a conversation step) or a background launch opens
 * its own instead of capturing the caller's.
 */

inline fun <T> db(block: context(Session) () -> T): T =
    sessionOf(dataSource).use { block(it) }

/**
 * Runs [block] as one transaction on the caller's session. Its connection is switched to a transaction
 * for the duration, so every DB call inside (on the same session) commits or rolls back together.
 */
context(session: Session)
inline fun <T> transaction(crossinline block: () -> T): T =
    session.transaction { block() }

context(session: Session)
inline fun <T> runSingle(q: Query, crossinline map: (Row) -> T): T? =
    session.run(q.map { row -> map(row) }.asSingle)

context(session: Session)
inline fun <T> runList(q: Query, crossinline map: (Row) -> T): List<T> =
    session.run(q.map { row -> map(row) }.asList)

context(session: Session)
fun execute(q: Query): Boolean = session.execute(q)

context(session: Session)
fun update(q: Query): Int = session.update(q)
