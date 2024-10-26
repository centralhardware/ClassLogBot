package me.centralhardware.znatoki.telegram.statistic.mapper

import dev.inmo.tgbotapi.types.toChatId
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.configuration.session
import me.centralhardware.znatoki.telegram.statistic.parseStringList
import me.centralhardware.znatoki.telegram.statistic.toCustomProperties

object ConfigMapper {

    fun get(key: String) =
        session.run(
            queryOf(
                    """
                SELECT value 
                FROM config
                WHERE key = :key
            """,
                    mapOf("key" to key)
                )
                .map { it.string("value") }
                .asSingle
        )!!

    fun logChat() = get("logChatId").toLong().toChatId()
    fun paymentProperties() = get("payment_properties").toCustomProperties()
    fun clientProperties() = get("client_properties").toCustomProperties()
    fun serviceProperties() = get("service_properties").toCustomProperties()
    fun includeInInline() = get("include_in_inlines").parseStringList()
    fun includeInReport() = get("include_in_report").parseStringList()
    fun grafanaUsername() = get("grafana_username")
    fun grafanaPassword() = get("grafana_password")
    fun grafanaUrl() = get("grafana_url")
    fun clientName() = get("client_name")
    fun name() = get("name")
}
