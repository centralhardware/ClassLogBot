package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import arrow.core.left
import arrow.core.right
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.entity.ClientBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.PhoneNumber
import me.centralhardware.znatoki.telegram.statistic.entity.SchoolClass
import me.centralhardware.znatoki.telegram.statistic.entity.SourceOption
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.extensions.tutorId
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.LessonMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper

suspend fun BehaviourContext.startClientFsm(message: CommonMessage<MessageContent>) =
    startTelegramFsm(
        "clientFsm",
        ClientBuilder(),
        message
    ) {

        fio(
            "Ведите ФИО в формате: фамилия имя [отчество].",
            false,
            {
                StudentMapper.findAllByFio(it.first, it.second, it.third)
                    .isEmpty()
            }
        ) { builder, fio ->
            builder.name = fio.first
            builder.secondName = fio.second
            builder.lastName = fio.third
        }

        int(
            "Введите класс /skip для пропуска.",
            validator = {
                if (SchoolClass.validate(it)) {
                    Unit.right()
                } else {
                    "Введите класс".left()
                }
            }
        ) { builder, schoolClass ->
            builder.schoolClass = SchoolClass(schoolClass)
        }

        date(
            prompt = "Введите дату записи дд ММ гггг"
        ) { builder, date ->
            builder.recordDate = date
        }

        date(
            prompt = "Введите дату рождения дд ММ гггг"
        ) { b, date ->
            b.birthDate = date
        }

        enum(
            prompt = "Введите как узнал",
            options = SourceOption.options(),
            true
        ) { b, value ->
            b.source = SourceOption.fromTitle(value)
        }

        phone(
            prompt = "Введите телефон /skip для пропуска.",
            optionalSkip = true
        ) { b, value ->
            b.phone = PhoneNumber(value)
        }

        phone(
            prompt = "Введите телефон ответственного /skip для пропуска.",
            optionalSkip = true
        ) { b, value ->
            b.responsiblePhone = PhoneNumber(value)
        }

        text(
            prompt = "Введите ФИО матери /skip для пропуска.",
            optionalSkip = true
        ) { b, value ->
            b.motherFio = value
        }

        onFinish { message, b ->
            b.createdBy = message.tutorId()

            val client = b.build()
            client.id = StudentMapper.save(client)

            sendTextMessage(
                message.chat,
                client.getInfo(
                    LessonMapper.getSubjectIdsForStudent(client.id!!).map {
                        SubjectMapper.getNameById(it)
                    }
                ),
                parseMode = MarkdownParseMode,
            )

            // лог
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

            sendTextMessage(message.chat, "Создание ученика закончено")
        }
    }

