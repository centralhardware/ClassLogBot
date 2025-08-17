package me.centralhardware.znatoki.telegram.statistic.telegram

import me.centralhardware.telegram.ktgbotapi.access.checker.UserAccessChecker
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper

class UserExistChecker: UserAccessChecker {
    override fun checkAccess(userId: Long?): Boolean {
        return UserMapper.findById(userId!!) != null
    }
}