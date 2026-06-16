package me.centralhardware.znatoki.telegram.statistic.telegram.conversation

import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardRemove
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.message.abstracts.ChatContentMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import me.centralhardware.telegram.conversation.CANCEL
import me.centralhardware.telegram.conversation.ConversationCancelledException
import me.centralhardware.telegram.conversation.SKIP
import me.centralhardware.telegram.conversation.waitEnum
import me.centralhardware.telegram.conversation.waitInt
import me.centralhardware.telegram.conversation.waitParsed
import me.centralhardware.telegram.conversation.waitValidatedText
import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.entity.ClientBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.PhoneNumber
import me.centralhardware.znatoki.telegram.statistic.entity.SchoolClass
import me.centralhardware.znatoki.telegram.statistic.entity.SourceOption
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.extensions.tutorId
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.telegram.parseDateInput
import me.centralhardware.znatoki.telegram.statistic.telegram.parseFio
import me.centralhardware.znatoki.telegram.statistic.telegram.parsePhone

/**
 * Creates a new student using wait-based conversation flow
 */
suspend fun BehaviourContext.createStudent(message: ChatContentMessage<MessageContent>) {
    val chatId = message.chat.id
    val builder = ClientBuilder()

    try {
        sendTextMessage(chatId, "Начало создания ученика. Используйте $CANCEL для отмены в любой момент.")

        // Step 1: Enter FIO
        val fio = waitParsed(chatId, "Ведите ФИО в формате: фамилия имя [отчество].") { text ->
            parseFio(text) { StudentMapper.findAllByFio(it.first, it.second, it.third).isEmpty() }
        }
        builder.name = fio!!.first
        builder.secondName = fio.second
        builder.lastName = fio.third

        // Step 2: Enter school class (optional)
        val schoolClass = waitInt(
            chatId = chatId,
            prompt = "Введите класс $SKIP для пропуска.",
            allowSkip = true,
            validate = { if (SchoolClass.validate(it)) null else "Введите класс" }
        )
        schoolClass?.let { builder.schoolClass = SchoolClass(it) }

        // Step 3: Enter record date
        builder.recordDate = waitParsed(chatId, "Введите дату записи дд ММ гггг") { parseDateInput(it) }

        // Step 4: Enter birth date
        builder.birthDate = waitParsed(chatId, "Введите дату рождения дд ММ гггг") { parseDateInput(it) }

        // Step 5: Select source (how they learned about us)
        val source = waitEnum(
            chatId = chatId,
            prompt = "Введите как узнал",
            options = SourceOption.options(),
            allowSkip = true
        )
        source?.let { builder.source = SourceOption.fromTitle(it) }

        // Step 6: Enter phone (optional)
        val phone = waitParsed(chatId, "Введите телефон $SKIP для пропуска.", allowSkip = true) { parsePhone(it) }
        phone?.let { builder.phone = PhoneNumber(it) }

        // Step 7: Enter responsible person's phone (optional)
        val responsiblePhone = waitParsed(
            chatId,
            "Введите телефон ответственного $SKIP для пропуска.",
            allowSkip = true
        ) { parsePhone(it) }
        responsiblePhone?.let { builder.responsiblePhone = PhoneNumber(it) }

        // Step 8: Enter mother's FIO (optional)
        val motherFio = waitValidatedText(
            chatId = chatId,
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
                LessonMapper.getSubjectIdsForStudent(client.id).map {
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
                    LessonMapper.getSubjectIdsForStudent(client.id)
                        .map { SubjectMapper.getNameById(it) }
                )
            }
            """.trimIndent(),
            parseMode = MarkdownParseMode,
        )
        
        sendTextMessage(message.chat, "Создание ученика закончено")
        
    } catch (e: ConversationCancelledException) {
        sendTextMessage(chatId, "Создание ученика отменено", replyMarkup = ReplyKeyboardRemove())
    } catch (e: Exception) {
        sendTextMessage(chatId, "Операция отменена: ${e.message}", replyMarkup = ReplyKeyboardRemove())
    }
}
