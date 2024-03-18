package me.centralhardware.znatoki.telegram.statistic

import me.centralhardware.znatoki.telegram.statistic.mapper.*
import me.centralhardware.znatoki.telegram.statistic.service.ClientService
import me.centralhardware.znatoki.telegram.statistic.service.MinioService
import me.centralhardware.znatoki.telegram.statistic.telegram.TelegramSender
import me.centralhardware.znatoki.telegram.statistic.telegram.fsm.Storage
import me.centralhardware.znatoki.telegram.statistic.validate.AmountValidator
import me.centralhardware.znatoki.telegram.statistic.validate.FioValidator
import me.centralhardware.znatoki.telegram.statistic.validate.ServiceValidator
import org.jetbrains.annotations.NotNull
import org.springframework.stereotype.Component
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import java.util.ResourceBundle

@Component
class BeanUtils : ApplicationContextAware {

    companion object {
        var applicationContext: ApplicationContext? = null
        fun <T> getBean(clazz: Class<T>): T {
            return applicationContext?.getBean(clazz) ?: throw IllegalArgumentException()
        }
    }

    override fun setApplicationContext(@NotNull ac: ApplicationContext) {
        applicationContext = ac
    }

}


fun telegramClient() = BeanUtils.getBean(OkHttpTelegramClient::class.java)
fun resourceBundle() = BeanUtils.getBean(ResourceBundle::class.java)
fun sender() = BeanUtils.getBean(TelegramSender::class.java)

fun serviceValidator() = BeanUtils.getBean(ServiceValidator::class.java)
fun fioValidator() = BeanUtils.getBean(FioValidator::class.java)
fun amountValidator() = BeanUtils.getBean(AmountValidator::class.java)
fun paymentMapper() = BeanUtils.getBean(PaymentMapper::class.java)

fun servicesMapper() = BeanUtils.getBean(ServicesMapper::class.java)
fun serviceMapper() = BeanUtils.getBean(ServiceMapper::class.java)
fun userMapper() = BeanUtils.getBean(UserMapper::class.java)
fun organizationMapper() = BeanUtils.getBean(OrganizationMapper::class.java)
fun clientService() = BeanUtils.getBean(ClientService::class.java)
fun minioService() = BeanUtils.getBean(MinioService::class.java)

fun storage() = BeanUtils.getBean(Storage::class.java)