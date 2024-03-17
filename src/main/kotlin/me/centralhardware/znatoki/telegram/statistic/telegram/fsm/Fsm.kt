package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import me.centralhardware.znatoki.telegram.statistic.eav.PropertiesBuilder
import me.centralhardware.znatoki.telegram.statistic.eav.Property
import me.centralhardware.znatoki.telegram.statistic.entity.ClientBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.PaymentBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.ServiceBuilder
import me.centralhardware.znatoki.telegram.statistic.mapper.OrganizationMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.bulider.replyKeyboard
import me.centralhardware.znatoki.telegram.statistic.userId
import me.centralhardware.znatoki.telegram.statistic.utils.organizationMapper
import me.centralhardware.znatoki.telegram.statistic.utils.sender
import me.centralhardware.znatoki.telegram.statistic.utils.userMapper
import org.telegram.telegrambots.meta.api.objects.Update
import ru.nsk.kstatemachine.Event
import ru.nsk.kstatemachine.TransitionParams

sealed interface UpdateEvent {
    object UpdateEvent : Event
}

fun getLogUser(userId: Long): Long? =
    userMapper().getById(userId)?.organizationId?.let { user ->
        organizationMapper().getById(user)?.logChatId
    }

fun processCustomProperties(
    update: Update,
    builder: PropertiesBuilder,
    onFinish: (List<Property>) -> Unit
) {
    val chatId = update.userId()

    builder.validate(update)
        .mapLeft { error ->
            sender().sendText(error, chatId)
        }
        .map {
            builder.setProperty(update)

            builder.next()?.let { next ->
                if (next.second.isNotEmpty()) {
                    sender().send(replyKeyboard {
                        text(next.first)
                        chatId(chatId)
                        next.second.forEach {
                            row { btn(it) }
                        }
                    }.build())
                } else {
                    sender().sendText(next.first, chatId)
                }
            } ?: onFinish(builder.properties)
        }
}

fun TransitionParams<*>.argTime(): Pair<Update, ServiceBuilder> = this.argument as Pair<Update, ServiceBuilder>
fun TransitionParams<*>.argClient(): Pair<Update, ClientBuilder> = this.argument as Pair<Update, ClientBuilder>
fun TransitionParams<*>.argPayment(): Pair<Update, PaymentBuilder> = this.argument as Pair<Update, PaymentBuilder>