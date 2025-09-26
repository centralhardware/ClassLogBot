package me.centralhardware.znatoki.telegram.statistic.extensions

import kotliquery.Query
import kotliquery.Row
import kotliquery.Session
import kotliquery.sessionOf
import me.centralhardware.znatoki.telegram.statistic.dataSource

inline fun <T> runSingle(q: Query, crossinline map: (Row) -> T): T? =
    sessionOf(dataSource).use { session ->
        session.run(q.map { map(it) }.asSingle)
    }

inline fun <T> runList(q: Query, crossinline map: (Row) -> T): List<T> =
    sessionOf(dataSource).use { session ->
        session.run(q.map { map(it) }.asList)
    }

fun execute(q: Query): Boolean =
    sessionOf(dataSource).use { session ->
        session.execute(q)
    }

fun update(q: Query): Int =
    sessionOf(dataSource).use { session ->
        session.update(q)
    }

