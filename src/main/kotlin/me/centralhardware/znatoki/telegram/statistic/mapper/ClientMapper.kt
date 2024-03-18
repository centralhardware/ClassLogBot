package me.centralhardware.znatoki.telegram.statistic.mapper

import kotliquery.Session
import kotliquery.queryOf
import org.springframework.stereotype.Component

@Component
class ClientMapper(private val session: Session) {

    fun existsByFio(fio: String) = session.run(
        queryOf(
            """
            SELECT EXISTS(
            SELECT *
            FROM client
            WHERE lower(trim(concat(id, ' ', name, ' ', second_name, ' ', last_name))) = lower(:fio)
            ORDER BY create_date DESC
            ) as e
        """, mapOf("fio" to fio)
        ).map { row -> row.boolean("e") }.asSingle
    )?: false

}