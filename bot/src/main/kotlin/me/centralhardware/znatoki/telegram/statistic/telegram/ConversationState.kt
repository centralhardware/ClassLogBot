package me.centralhardware.znatoki.telegram.statistic.telegram

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.d
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages active conversation states to prevent concurrent conversations
 * and enable cancellation.
 */
object ConversationState {
    private val activeConversations = ConcurrentHashMap<Long, ConversationInfo>()
    
    data class ConversationInfo(
        val type: ConversationType,
        val job: Job
    )
    
    enum class ConversationType {
        LESSON,
        PAYMENT,
        STUDENT
    }
    
    /**
     * Check if user has an active conversation
     */
    fun hasActiveConversation(userId: Long): Boolean {
        return activeConversations.containsKey(userId)
    }


    fun startConversation(userId: Long, type: ConversationType, job: Job): ConversationType? {
        if (hasActiveConversation(userId)) {
            return null
        }
        activeConversations[userId] = ConversationInfo(type, job)
        KSLog.d("User $userId started conversation: $type")
        return type
    }

    fun endConversation(userId: Long) {
        val removed = activeConversations.remove(userId)
        if (removed != null) {
            KSLog.d("User $userId finished conversation: ${removed.type}")
        }
    }

    fun cancelConversation(userId: Long): Boolean {
        val info = activeConversations.remove(userId)
        if (info != null) {
            KSLog.d("User $userId cancelled conversation: ${info.type}")
            info.job.cancel()
            return true
        }
        return false
    }
}
