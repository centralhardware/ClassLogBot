package me.centralhardware.znatoki.telegram.statistic.firefly

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import me.centralhardware.znatoki.telegram.statistic.firefly.FireflyApiClient.handleResponse

object FireflyApiClient {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        install(DefaultRequest) {
            header("Authorization", "Bearer ${FireflyConfig.fireflyToken}")
            header("Accept", "application/vnd.api+json")
            contentType(ContentType.Application.Json)
        }

        defaultRequest {
            url(FireflyConfig.fireflyBaseUrl)
        }
    }

    private suspend inline fun <reified T> HttpResponse.handleResponse(operationName: String): T {
        if (!status.isSuccess()) {
            val errorBody = bodyAsText()
            throw RuntimeException("Failed to $operationName: ${status}. Response: $errorBody")
        }
        return body()
    }

    private suspend fun HttpResponse.handleResponseUnit(operationName: String) {
        if (!status.isSuccess()) {
            val errorBody = bodyAsText()
            throw RuntimeException("Failed to $operationName: ${status}. Response: $errorBody")
        }
    }

    suspend fun createTransaction(transaction: TransactionRequest): TransactionResponse {
        return client.post("/api/v1/transactions") {
            setBody(transaction)
        }.handleResponse("create transaction in Firefly")
    }

    suspend fun createAndUploadAttachment(
        transactionJournalId: String,
        filename: String,
        title: String,
        fileBytes: ByteArray,
        notes: String? = null
    ) {
        val attachmentRequest = AttachmentRequest(
            filename = filename,
            attachableType = "TransactionJournal",
            attachableId = transactionJournalId,
            title = title,
            notes = notes
        )

        val attachmentResponse = client.post("/api/v1/attachments") {
            setBody(attachmentRequest)
        }.handleResponse<AttachmentResponse>("create attachment in Firefly")

        val uploadUrl = attachmentResponse.data.attributes.uploadUrl
        if (uploadUrl != null) {
            client.post(uploadUrl) {
                setBody(fileBytes)
                contentType(ContentType.Application.OctetStream)
            }.handleResponseUnit("upload attachment to Firefly")
        }
    }

    suspend fun getOrCreateAccount(name: String, type: String, accountRole: String? = null): String {
        // Try to create directly, Firefly will return existing if duplicate
        val accountRequest = AccountRequest(
            name = name,
            type = type,
            accountRole = accountRole,
            currencyCode = "RUB"
        )

        return try {
            val response = client.post("/api/v1/accounts") {
                setBody(accountRequest)
            }.handleResponse<AccountResponse>("create account in Firefly")
            response.data.id
        } catch (e: Exception) {
            // If creation failed, try to find existing account
            val accounts = client.get("/api/v1/accounts") {
                parameter("type", type)
            }.handleResponse<AccountListResponse>("get accounts from Firefly")

            accounts.data.find { it.attributes.name == name }?.id
                ?: throw IllegalStateException("Failed to create or find account: $name")
        }
    }

    suspend fun getOrCreateCategory(name: String): String {
        // Try to create directly, Firefly will return existing if duplicate
        val categoryRequest = CategoryRequest(name = name)

        return try {
            val response = client.post("/api/v1/categories") {
                setBody(categoryRequest)
            }.handleResponse<CategoryResponse>("create category in Firefly")
            response.data.id
        } catch (e: Exception) {
            // If creation failed, try to find existing category
            val categories = client.get("/api/v1/categories")
                .handleResponse<CategoryListResponse>("get categories from Firefly")

            categories.data.find { it.attributes.name == name }?.id
                ?: throw IllegalStateException("Failed to create or find category: $name")
        }
    }

    suspend fun getAllTransactionExternalIds(): Set<String> {
        val externalIds = mutableSetOf<String>()
        var page = 1
        var hasMorePages = true

        try {
            while (hasMorePages) {
                val response = client.get("/api/v1/transactions") {
                    parameter("page", page)
                    parameter("limit", 100)
                }

                if (!response.status.isSuccess()) {
                    break
                }

                val transactions = response.body<TransactionListResponse>()

                transactions.data.forEach { transaction ->
                    transaction.attributes.transactions.forEach { split ->
                        split.externalId?.let { externalIds.add(it) }
                    }
                }

                val totalPages = transactions.meta?.pagination?.totalPages ?: 1
                hasMorePages = page < totalPages
                page++
            }
        } catch (e: Exception) {
            // Ignore errors, return what we have
        }

        return externalIds
    }

    fun close() {
        client.close()
    }
}
