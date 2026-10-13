package me.centralhardware.znatoki.telegram.statistic.extensions

import dev.inmo.tgbotapi.extensions.utils.asFromUser
import dev.inmo.tgbotapi.types.update.abstracts.Update
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId

/** null for updates that have no sender (channel posts, some service updates). */
fun Update.userId(): Long? = data.asFromUser()?.from?.id?.chatId?.long
fun Update.tutorId(): TutorId? = userId()?.let { TutorId(it) }
