package me.centralhardware.znatoki.telegram.statistic.firefly

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import io.minio.GetObjectArgs
import io.minio.MinioClient
import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.entity.Payment
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import java.time.format.DateTimeFormatter

object FireflyService {

    private val minioClient = MinioClient.builder()
        .endpoint(Config.Minio.url)
        .credentials(Config.Minio.accessKey, Config.Minio.secretKey)
        .build()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val accountCache = mutableMapOf<String, String>()
    private val categoryCache = mutableMapOf<String, String>()
    private val exportedPaymentsCache = mutableSetOf<String>()

    /**
     * Export a single payment to Firefly III
     * Creates accounts for tutor (source) and student (destination)
     * Creates category for subject
     * Attaches payment screenshot if available
     */
    suspend fun exportPayment(payment: Payment): Result<String> = runCatching {
        if (!FireflyConfig.enabled) {
            throw IllegalStateException("Firefly integration is not enabled. Set FIREFLY_ENABLED=true")
        }

        val paymentId = payment.id?.id?.toString() ?: throw IllegalStateException("Payment has no ID")

        // Check if already exported (use cache or search in Firefly)
        if (exportedPaymentsCache.contains(paymentId)) {
            KSLog.info { "Payment $paymentId already in cache, skipping" }
            return@runCatching "cached_$paymentId"
        }

        KSLog.info { "Exporting payment ${payment.id?.id} to Firefly" }

        // Get tutor, student, and subject details
        val tutor = TutorMapper.findByIdOrNull(payment.tutorId)
            ?: throw IllegalStateException("Tutor not found: ${payment.tutorId.id}")
        val studentFio = StudentMapper.getFioById(payment.studentId)
        val studentAccountName = "${payment.studentId.id} $studentFio"
        val subjectName = SubjectMapper.getNameById(payment.subjectId)

        // Create or get accounts
        // For deposit: source = revenue (ученик), destination = asset (преподаватель)
        getOrCreateAccount(studentAccountName, "revenue", null)
        getOrCreateAccount(tutor.name, "asset", "defaultAsset")

        // Create transaction
        val transaction = TransactionRequest(
            errorIfDuplicateHash = true,
            transactions = listOf(
                TransactionSplit(
                    type = "deposit",
                    date = payment.dateTime.format(dateFormatter),
                    amount = payment.amount.amount.toString(),
                    description = "${tutor.name} $subjectName $studentFio",
                    sourceName = studentAccountName,
                    destinationName = tutor.name,
                    currencyCode = "RUB",
                    categoryName = subjectName,
                    externalId = payment.id?.id?.toString()
                )
            )
        )

        val response = try {
            FireflyApiClient.createTransaction(transaction)
        } catch (e: Exception) {
            // If error is about duplicate, it's ok - transaction already exists
            if (e.message?.contains("duplicate", ignoreCase = true) == true ||
                e.message?.contains("Duplicate of transaction", ignoreCase = true) == true) {
                KSLog.info { "Payment ${payment.id?.id} already exists in Firefly (duplicate), skipping" }
                exportedPaymentsCache.add(paymentId)
                return@runCatching "existing_duplicate_$paymentId"
            }
            throw e
        }

        val transactionJournalId = response.data.attributes.transactions.firstOrNull()?.transactionJournalId
            ?: throw IllegalStateException("No transaction journal ID in response")

        KSLog.info { "Created transaction in Firefly: ${response.data.id}, journal ID: $transactionJournalId" }

        // Add to cache
        exportedPaymentsCache.add(paymentId)

        // Attach screenshot if available
        if (payment.photoReport != null) {
            attachScreenshot(transactionJournalId, payment.photoReport, payment.id?.id ?: 0)
        }

        response.data.id
    }.onFailure { error ->
        KSLog.error { "Failed to export payment ${payment.id?.id}: ${error.message}" }
        KSLog.error(error)
    }

    /**
     * Export all payments to Firefly III
     */
    suspend fun exportAllPayments(payments: List<Payment>): Result<List<String>> = runCatching {
        if (!FireflyConfig.enabled) {
            throw IllegalStateException("Firefly integration is not enabled. Set FIREFLY_ENABLED=true")
        }

        KSLog.info { "Exporting ${payments.size} payments to Firefly" }
        val results = mutableListOf<String>()
        var successCount = 0
        var failureCount = 0

        payments.forEachIndexed { index, payment ->
            val progress = ((index + 1) * 100) / payments.size
            KSLog.info { "[$progress%] Processing payment ${index + 1}/${payments.size}: ID=${payment.id?.id}" }

            exportPayment(payment).onSuccess { transactionId ->
                results.add(transactionId)
                successCount++
                KSLog.info { "✓ Successfully exported payment ${payment.id?.id} (Success: $successCount, Failed: $failureCount)" }
            }.onFailure { error ->
                failureCount++
                KSLog.error { "✗ Failed to export payment ${payment.id?.id}: ${error.message} (Success: $successCount, Failed: $failureCount)" }
            }
        }

        KSLog.info { "Export completed: ${results.size}/${payments.size} payments exported successfully" }
        KSLog.info { "Summary - Success: $successCount, Failed: $failureCount" }
        results
    }

    private suspend fun getOrCreateAccount(name: String, type: String, accountRole: String?): String {
        val cacheKey = "$type:$name"

        // Check cache first
        accountCache[cacheKey]?.let { return it }

        // Not in cache, create/get from Firefly
        val accountId = FireflyApiClient.getOrCreateAccount(name, type, accountRole)
        accountCache[cacheKey] = accountId
        return accountId
    }

    private suspend fun getOrCreateCategory(name: String): String {
        // Check cache first
        categoryCache[name]?.let { return it }

        // Not in cache, create/get from Firefly
        val categoryId = FireflyApiClient.getOrCreateCategory(name)
        categoryCache[name] = categoryId
        return categoryId
    }

    private suspend fun attachScreenshot(transactionJournalId: String, photoPath: String, paymentId: Int) {
        try {
            KSLog.info { "Attaching screenshot for payment $paymentId from MinIO: $photoPath" }

            val fileBytes = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(Config.Minio.bucket)
                    .`object`(photoPath)
                    .build()
            ).readAllBytes()

            val filename = photoPath.substringAfterLast('/')

            FireflyApiClient.createAndUploadAttachment(
                transactionJournalId = transactionJournalId,
                filename = filename,
                title = "Скриншот оплаты #$paymentId",
                fileBytes = fileBytes,
                notes = "Автоматически загружен из MinIO"
            )

            KSLog.info { "Successfully attached screenshot for payment $paymentId" }
        } catch (e: Exception) {
            KSLog.error { "Failed to attach screenshot for payment $paymentId: ${e.message}" }
            KSLog.error(e)
        }
    }

    fun clearCache() {
        accountCache.clear()
        categoryCache.clear()
        exportedPaymentsCache.clear()
    }

    suspend fun loadExistingTransactions() {
        KSLog.info { "Loading existing transactions from Firefly to prevent duplicates..." }
        try {
            val existing = FireflyApiClient.getAllTransactionExternalIds()
            exportedPaymentsCache.addAll(existing)
            KSLog.info { "Loaded ${existing.size} existing transaction external IDs" }
        } catch (e: Exception) {
            KSLog.error { "Failed to load existing transactions: ${e.message}" }
        }
    }
}
