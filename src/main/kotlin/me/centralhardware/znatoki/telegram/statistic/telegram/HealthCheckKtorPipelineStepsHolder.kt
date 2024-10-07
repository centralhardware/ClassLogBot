package me.centralhardware.znatoki.telegram.statistic.telegram

import dev.inmo.tgbotapi.bot.ktor.KtorCallFactory
import dev.inmo.tgbotapi.bot.ktor.KtorPipelineStepsHolder
import dev.inmo.tgbotapi.requests.GetUpdates
import dev.inmo.tgbotapi.requests.abstracts.Request
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.flow.MutableStateFlow

class HealthCheckKtorPipelineStepsHolder : KtorPipelineStepsHolder {

    val health: MutableStateFlow<Boolean> = MutableStateFlow(false);

    override suspend fun <T : Any> onRequestException(request: Request<T>, t: Throwable): T? {
        if (t is HttpRequestTimeoutException &&
            t.message!!.startsWith("Request timeout has expired [url=https://api.telegram.org/bot")
        ) {
            health.value = true
        } else {
            health.value = false
        }

        return super.onRequestException(request, t)
    }

    override suspend fun <T : Any> onRequestReturnResult(
        result: Result<T>,
        request: Request<T>,
        callsFactories: List<KtorCallFactory>
    ): T {
        if (request is GetUpdates && result.isSuccess) {
            health.value = true
        }
        return super.onRequestReturnResult(result, request, callsFactories)
    }

}