package me.centralhardware.znatoki.telegram.statistic.firefly

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionRequest(
    @SerialName("error_if_duplicate_hash") val errorIfDuplicateHash: Boolean = true,
    @SerialName("apply_rules") val applyRules: Boolean = true,
    @SerialName("fire_webhooks") val fireWebhooks: Boolean = true,
    @SerialName("group_title") val groupTitle: String? = null,
    val transactions: List<TransactionSplit>
)

@Serializable
data class TransactionSplit(
    val type: String, // "deposit", "withdrawal", "transfer"
    val date: String, // YYYY-MM-DD format
    val amount: String,
    val description: String,
    @SerialName("source_name") val sourceName: String? = null,
    @SerialName("destination_name") val destinationName: String? = null,
    @SerialName("currency_code") val currencyCode: String? = "RUB",
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("external_id") val externalId: String? = null,
    val notes: String? = null,
    val tags: List<String>? = null,
    @SerialName("transaction_journal_id") val transactionJournalId: String? = null
)

@Serializable
data class TransactionResponse(
    val data: TransactionData
)

@Serializable
data class TransactionData(
    val type: String,
    val id: String,
    val attributes: TransactionAttributes
)

@Serializable
data class TransactionAttributes(
    val transactions: List<TransactionSplitResponse>
)

@Serializable
data class TransactionSplitResponse(
    @SerialName("transaction_journal_id") val transactionJournalId: String,
    val type: String,
    val date: String,
    val amount: String,
    val description: String,
    @SerialName("external_id") val externalId: String? = null
)

@Serializable
data class AttachmentRequest(
    val filename: String,
    @SerialName("attachable_type") val attachableType: String,
    @SerialName("attachable_id") val attachableId: String,
    val title: String? = null,
    val notes: String? = null
)

@Serializable
data class AttachmentResponse(
    val data: AttachmentData
)

@Serializable
data class AttachmentData(
    val type: String,
    val id: String,
    val attributes: AttachmentAttributes
)

@Serializable
data class AttachmentAttributes(
    @SerialName("attachable_id") val attachableId: String,
    val filename: String,
    @SerialName("upload_url") val uploadUrl: String? = null
)

@Serializable
data class AccountRequest(
    val name: String,
    val type: String, // "asset", "expense", "revenue"
    @SerialName("account_role") val accountRole: String? = null,
    @SerialName("currency_code") val currencyCode: String? = "RUB",
    val notes: String? = null,
    val active: Boolean = true
)

@Serializable
data class AccountResponse(
    val data: AccountData
)

@Serializable
data class AccountData(
    val type: String,
    val id: String,
    val attributes: AccountAttributes
)

@Serializable
data class AccountAttributes(
    val name: String,
    val type: String,
    @SerialName("account_role") val accountRole: String? = null
)

@Serializable
data class AccountListResponse(
    val data: List<AccountData>,
    val meta: PaginationMeta? = null
)

@Serializable
data class CategoryRequest(
    val name: String,
    val notes: String? = null
)

@Serializable
data class CategoryResponse(
    val data: CategoryData
)

@Serializable
data class CategoryData(
    val type: String,
    val id: String,
    val attributes: CategoryAttributes
)

@Serializable
data class CategoryAttributes(
    val name: String,
    val notes: String? = null
)

@Serializable
data class CategoryListResponse(
    val data: List<CategoryData>,
    val meta: PaginationMeta? = null
)

@Serializable
data class TransactionListResponse(
    val data: List<TransactionData>,
    val meta: PaginationMeta? = null,
    val links: PaginationLinks? = null
)

@Serializable
data class PaginationMeta(
    val pagination: PaginationInfo
)

@Serializable
data class PaginationInfo(
    val total: Int,
    val count: Int,
    @SerialName("per_page") val perPage: Int,
    @SerialName("current_page") val currentPage: Int,
    @SerialName("total_pages") val totalPages: Int
)

@Serializable
data class PaginationLinks(
    val self: String,
    val first: String? = null,
    val next: String? = null,
    val prev: String? = null,
    val last: String? = null
)
