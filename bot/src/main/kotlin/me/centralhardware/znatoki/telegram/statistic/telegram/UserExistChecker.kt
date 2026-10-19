package me.centralhardware.znatoki.telegram.statistic.telegram

import UserAccessChecker
import me.centralhardware.znatoki.telegram.statistic.extensions.db
import me.centralhardware.znatoki.telegram.statistic.entity.TutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper

class UserExistChecker: UserAccessChecker {
    override fun checkAccess(userId: Long?): Boolean {
        return userId != null && db { TutorMapper.findByIdOrNull(TutorId(userId)) } != null
    }
}