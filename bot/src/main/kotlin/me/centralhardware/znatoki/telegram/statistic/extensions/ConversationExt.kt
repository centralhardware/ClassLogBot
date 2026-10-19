package me.centralhardware.znatoki.telegram.statistic.extensions

import me.centralhardware.telegram.conversation.ConversationCancelledException

/**
 * The answer to a step that can't be skipped. The wait* helpers return null only for a skipped
 * optional step, so null here means the step was skipped anyway, and the conversation ends like a cancel.
 */
fun <T : Any> T?.required(): T = this ?: throw ConversationCancelledException("A required step was skipped")
