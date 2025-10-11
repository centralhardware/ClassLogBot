@file:JvmName("BehaviourContextExtCompat")
package me.centralhardware.znatoki.telegram.statistic

// Compatibility layer - re-export extensions to old package for backward compatibility
import me.centralhardware.znatoki.telegram.statistic.extensions.user as _user
import me.centralhardware.znatoki.telegram.statistic.extensions.initContext as _initContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextData
import me.centralhardware.znatoki.telegram.statistic.entity.Tutor

var BehaviourContextData.user: Tutor
    get() = this._user
    set(value) { this._user = value }

suspend fun BehaviourContext.initContext(filter: (Tutor?) -> Boolean, block: BehaviourContext.() -> Unit) =
    this._initContext(filter, block)
