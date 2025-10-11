package me.centralhardware.znatoki.telegram.statistic.extensions

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextData
import dev.inmo.tgbotapi.extensions.behaviour_builder.createSubContextAndDoAsynchronouslyWithUpdatesFilter
import kotlinx.coroutines.flow.filter
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper

var BehaviourContextData.user: Tutor
    get() = get("user") as Tutor
    set(value) = set("user", value)

suspend fun BehaviourContext.initContext(filter: (Tutor?) -> Boolean, block: BehaviourContext.() -> Unit) = createSubContextAndDoAsynchronouslyWithUpdatesFilter(
    updatesUpstreamFlow = allUpdatesFlow.filter {
        runCatching {
            filter.invoke(TutorMapper.findByIdOrNull(it.tutorId()))
        }.getOrDefault(false)
    }
) { block() }
