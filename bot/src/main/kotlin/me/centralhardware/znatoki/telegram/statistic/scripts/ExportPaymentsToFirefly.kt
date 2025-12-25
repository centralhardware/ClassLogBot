package me.centralhardware.znatoki.telegram.statistic.scripts

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.setDefaultKSLog
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.entity.parsePayment
import me.centralhardware.znatoki.telegram.statistic.extensions.runList
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyService

/**
 * Script to export all payments to Firefly III
 *
 * Usage:
 * ./gradlew :bot:run --args="export-payments"
 *
 * Or compile and run directly:
 * ./gradlew :bot:installDist
 * ./bot/build/install/bot/bin/bot export-payments
 *
 * Environment variables required:
 * - FIREFLY_BASE_URL: URL to Firefly III instance (default: https://firefly.centralhardware.me)
 * - FIREFLY_TOKEN: API token for Firefly III
 * - FIREFLY_USER_EMAIL: Email of the Firefly user (default: ies1081971@bk.ru)
 * - FIREFLY_ENABLED: Set to "true" to enable Firefly integration
 * - FIREFLY_CURRENCY: Currency code (default: RUB)
 *
 * Database connection:
 * - DATASOURCE_URL
 * - DATASOURCE_USERNAME
 * - DATASOURCE_PASSWORD
 *
 * MinIO (for attachments):
 * - MINIO_URL
 * - MINIO_BUCKET
 * - MINIO_ACCESS_KEY
 * - MINIO_SECRET_KEY
 */
fun main(args: Array<String>) = runBlocking {
    KSLog.info { "=== Firefly Payment Export Script ===" }
    KSLog.info { "Starting export of all payments to Firefly III" }

    try {
        // Load existing transactions to prevent duplicates
        FireflyService.loadExistingTransactions()

        // Get all payments from database
        val payments = getAllPayments()
        KSLog.info { "Found ${payments.size} payments to export" }

        if (payments.isEmpty()) {
            KSLog.info { "No payments found. Exiting." }
            return@runBlocking
        }

        // Export to Firefly
        val result = FireflyService.exportAllPayments(payments)

        result.onSuccess { transactionIds ->
            KSLog.info { "=== Export Complete ===" }
            KSLog.info { "Successfully exported ${transactionIds.size}/${payments.size} payments" }
            KSLog.info { "Created ${transactionIds.size} transactions in Firefly" }
        }.onFailure { error ->
            KSLog.info { "=== Export Failed ===" }
            KSLog.info { "Error: ${error.message}" }
            throw error
        }
    } catch (e: Exception) {
        KSLog.info { "Fatal error during export: ${e.message}" }
        e.printStackTrace()
        throw e
    }
}

private fun getAllPayments(): List<Payment> {
    return runList(
        queryOf(
            """
            SELECT p.id,
                   p.date_time,
                   p.tutor_id,
                   p.student_id,
                   p.amount,
                   p.subject_id,
                   p.is_deleted,
                   p.photo_report,
                   p.added_by_tutor_id,
                   p.data_source
            FROM payment p
            WHERE p.is_deleted = false
            ORDER BY p.date_time ASC
            """
        )
    ) { it.parsePayment() }
}
