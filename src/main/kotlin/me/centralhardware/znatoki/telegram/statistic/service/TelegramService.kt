package me.centralhardware.znatoki.telegram.statistic.service

import me.centralhardware.znatoki.telegram.statistic.entity.Role
import me.centralhardware.znatoki.telegram.statistic.mapper.UserMapper
import org.springframework.stereotype.Service

@Service
class TelegramService(private val userMapper: UserMapper) {

    fun hasWriteRight(chatId: Long): Boolean {
        val role = getRole(chatId)
        return role == Role.READ_WRITE ||
                role == Role.ADMIN
    }

    fun hasReadRight(chatId: Long): Boolean {
        val role = getRole(chatId)
        return role == Role.READ ||
                role == Role.READ_WRITE ||
                role == Role.ADMIN
    }

    fun isAdmin(chatId: Long): Boolean {
        return getRole(chatId) == Role.ADMIN
    }

    private fun getRole(chatId: Long): Role? {
        return userMapper.getById(chatId)?.role
    }

}