package me.centralhardware.znatoki.telegram.statistic.telegram.conversation

import arrow.core.left
import arrow.core.right
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import kotlinx.coroutines.runBlocking
import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.entity.ClientBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.PhoneNumber
import me.centralhardware.znatoki.telegram.statistic.entity.SchoolClass
import me.centralhardware.znatoki.telegram.statistic.entity.SourceOption
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.extensions.tutorId
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.AuditLogMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.SKIP
import me.centralhardware.znatoki.telegram.statistic.telegram.*

/**
 * Creates a new student using wait-based conversation flow
 */
suspend fun BehaviourContext.createStudent(message: CommonMessage<MessageContent>) {
    val chatId = message.chat.id
    val userId = message.userId()
    val builder = ClientBuilder()
    
    try {
        sendTextMessage(chatId, "Начало создания ученика. Используйте $CANCEL для отмены в любой момент.")
        
        // Step 1: Enter FIO
        val fio = waitValidatedFio(
            chatId = chatId,
            userId = userId,
            prompt = "Ведите ФИО в формате: фамилия имя [отчество].",
            duplicateCheck = {
                StudentMapper.findAllByFio(it.first, it.second, it.third).isEmpty()
            }
        )
        builder.name = fio!!.first
        builder.secondName = fio.second
        builder.lastName = fio.third
        
        // Step 2: Enter school class (optional)
        val schoolClass = waitValidatedInt(
            chatId = chatId,
            userId = userId,
            prompt = "Введите класс $SKIP для пропуска.",
            allowSkip = true,
            validator = {
                if (SchoolClass.validate(it)) {
                    Unit.right()
                } else {
                    "Введите класс".left()
                }
            }
        )
        schoolClass?.let { builder.schoolClass = SchoolClass(it) }
        
        // Step 3: Enter record date
        val recordDate = waitValidatedDate(
            chatId = chatId,
            userId = userId,
            prompt = "Введите дату записи дд ММ гггг"
        )
        builder.recordDate = recordDate
        
        // Step 4: Enter birth date
        val birthDate = waitValidatedDate(
            chatId = chatId,
            userId = userId,
            prompt = "Введите дату рождения дд ММ гггг"
        )
        builder.birthDate = birthDate
        
        // Step 5: Select source (how they learned about us)
        val source = waitEnum(
            chatId = chatId,
            userId = userId,
            prompt = "Введите как узнал",
            options = SourceOption.options(),
            allowSkip = true
        )
        source?.let { builder.source = SourceOption.fromTitle(it) }
        
        // Step 6: Enter phone (optional)
        val phone = waitValidatedPhone(
            chatId = chatId,
            userId = userId,
            prompt = "Введите телефон $SKIP для пропуска.",
            allowSkip = true
        )
        phone?.let { builder.phone = PhoneNumber(it) }
        
        // Step 7: Enter responsible person's phone (optional)
        val responsiblePhone = waitValidatedPhone(
            chatId = chatId,
            userId = userId,
            prompt = "Введите телефон ответственного $SKIP для пропуска.",
            allowSkip = true
        )
        responsiblePhone?.let { builder.responsiblePhone = PhoneNumber(it) }
        
        // Step 8: Enter mother's FIO (optional)
        val motherFio = waitValidatedText(
            chatId = chatId,
            userId = userId,
            prompt = "Введите ФИО матери $SKIP для пропуска.",
            allowSkip = true
        )
        motherFio?.let { builder.motherFio = it }
        
        // Step 9: Save
        builder.createdBy = message.tutorId()
        
        val client = builder.build()
        client.id = StudentMapper.save(client)
        
        sendTextMessage(
            message.chat,
            client.getInfo(
                LessonMapper.getSubjectIdsForStudent(client.id!!).map {
                    SubjectMapper.getNameById(it)
                }
            ),
            parseMode = MarkdownParseMode,
            replyMarkup = ReplyKeyboardRemove()
        )
        
        sendTextMessage(
            Config.logChat(),
            """
            #ученик
            ${
                client.getInfo(
                    LessonMapper.getSubjectIdsForStudent(client.id!!)
                        .map { SubjectMapper.getNameById(it) }
                )
            }
            """.trimIndent(),
            parseMode = MarkdownParseMode,
        )
        
        AuditLogMapper.log(
            userId = message.tutorId().id,
            action = "CREATE_STUDENT",
            entityType = "student",
            entityId = client.id.id.toString(),
            studentId = client.id.id,
            subjectId = null,
            null,
            client
        )
        
        sendTextMessage(message.chat, "Создание ученика закончено")
        
    } catch (e: ConversationCancelledException) {
        sendTextMessage(chatId, "Создание ученика отменено", replyMarkup = ReplyKeyboardRemove())
    } catch (e: Exception) {
        sendTextMessage(chatId, "Операция отменена: ${e.message}", replyMarkup = ReplyKeyboardRemove())
    }
}
