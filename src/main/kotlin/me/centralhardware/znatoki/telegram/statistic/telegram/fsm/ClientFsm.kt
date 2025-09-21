package me.centralhardware.znatoki.telegram.statistic.telegram.fsm

import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendTextMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.types.message.MarkdownParseMode
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MessageContent
import me.centralhardware.znatoki.telegram.statistic.Config
import me.centralhardware.znatoki.telegram.statistic.entity.ClientBuilder
import me.centralhardware.znatoki.telegram.statistic.entity.getInfo
import me.centralhardware.znatoki.telegram.statistic.extensions.userId
import me.centralhardware.znatoki.telegram.statistic.mapper.ClientMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServiceMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.ServicesMapper


private val sourceOptions = listOf(
    "Вывеска",
    "С прошлых лет",
    "От знакомых",
    "2gis",
    "Реклама на подъезде",
    "Ходили старшие",
    "Интернет",
    "Листовка",
    "Аудио Реклама в магазине",
    "Реклама на ТВ",
    "инстаграм"
)

suspend fun BehaviourContext.startClientFsm(message: CommonMessage<MessageContent>) =
    startTelegramFsm(
        "clientFsm",
        ClientBuilder(),
        message
    ) {

        fio(
            "Введите ФИО. В формате: имя фамилия отчество.",
            false,
            {
                ClientMapper.findAllByFio(it.name, it.secondName, it.lastName)
                    .isEmpty()
            }
        ) { builder, fio ->
            builder.name = fio.name
            builder.secondName = fio.secondName
            builder.lastName = fio.lastName
        }

        int(
            "Введите класс /skip для пропуска."
        ) { builder, klass ->
            builder.klass = klass
        }

        date(
            prompt = "Введите дату записи"
        ) { builder, date ->
            builder.recordDate = date
        }

        date(
            prompt = "Введите дату рождения"
        ) { b, date ->
            b.birthDate = date
        }

        enum(
            prompt = "Введите как узнал",
            options = sourceOptions,
            true
        ) { b, value ->
            b.source = value
        }

        phone(
            prompt = "Введите телефон /skip для пропуска.",
            optionalSkip = true
        ) { b, value ->
            b.phone = value
        }

        phone(
            prompt = "Введите телефон ответственного /skip для пропуска.",
            optionalSkip = true
        ) { b, value ->
            b.responsiblePhone = value
        }

        fio(
            prompt = "Введите ФИО матери /skip для пропуска.",
            optionalSkip = true
        ) { b, value ->
            b.motherFio = "${value.name} ${value.secondName} ${value.lastName}"
        }

        onFinish { message, b ->
            b.createdBy = message.userId()

            val client = b.build()
            client.id = ClientMapper.save(client)

            sendTextMessage(
                message.chat,
                client.getInfo(
                    ServiceMapper.getServicesForClient(client.id!!).mapNotNull {
                        ServicesMapper.getNameById(it)
                    }
                ),
            )

            // лог
            send(
                Config.logChat(),
                text = """
                    #ученик
                    ${
                    client.getInfo(
                        ServiceMapper.getServicesForClient(client.id!!)
                            .mapNotNull { ServicesMapper.getNameById(it) }
                    )
                }
                """.trimIndent(),
                parseMode = MarkdownParseMode,
            )

            sendTextMessage(message.chat, "Создание ученика закончено")
        }
    }

