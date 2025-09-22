package me.centralhardware.znatoki.telegram.statistic.extensions

import kotliquery.Query
import kotliquery.Row
import kotliquery.Session

inline fun <T> Session.runSingle(q: Query, crossinline map: (Row) -> T): T? =
    this.run(q.map { map(it) }.asSingle)

inline fun <T> Session.runList(q: Query, crossinline map: (Row) -> T): List<T> =
    this.run(q.map { map(it) }.asList)

inline fun Session.update(q: Query) =
    this.update(q)