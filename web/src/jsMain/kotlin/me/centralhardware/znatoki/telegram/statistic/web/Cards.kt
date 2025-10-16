package me.centralhardware.znatoki.telegram.statistic.web

import androidx.compose.runtime.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import me.centralhardware.znatoki.telegram.statistic.dto.*

@Composable
fun LessonCard(lesson: LessonDto, onClick: () -> Unit) {
    Div({
        classes(AppStyles.card)
        style {
            marginBottom(12.px)
            padding(16.px)
            cursor("pointer")
        }
        onClick { onClick() }
    }) {
        Div({
            style {
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.SpaceBetween)
                alignItems(AlignItems.Start)
                marginBottom(12.px)
            }
        }) {
            Div {
                Div({
                    style {
                        fontSize(18.px)
                        fontWeight(600)
                        color(Color("#2d3748"))
                        marginBottom(4.px)
                    }
                }) {
                    Text(lesson.subject.name)
                }
                Div({ style { color(Color("#718096")); fontSize(14.px) } }) {
                    Text(lesson.students.joinToString(", ") { "${it.name} ${it.secondName}" })
                }
            }
            Div({
                style {
                    fontSize(20.px)
                    fontWeight(700)
                    color(Color("#667eea"))
                }
            }) {
                Text("${lesson.amount} ₽")
            }
        }

        Div({ style { marginBottom(12.px) } }) {
            if (lesson.isGroup) {
                Badge("Групповое", BadgeType.GROUP)
            }
            if (lesson.isExtra) {
                Badge("Доп.", BadgeType.EXTRA)
            }
        }

        Div({ style { display(DisplayStyle.Flex); property("gap", "8px") } }) {
            PrimaryButton("Изменить") { onClick() }
        }
    }
}

@Composable
fun PaymentCard(payment: PaymentDto, onClick: () -> Unit) {
    Div({
        classes(AppStyles.card)
        style {
            marginBottom(12.px)
            padding(16.px)
            cursor("pointer")
        }
        onClick { onClick() }
    }) {
        Div({
            style {
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.SpaceBetween)
                alignItems(AlignItems.Start)
                marginBottom(12.px)
            }
        }) {
            Div {
                Div({
                    style {
                        fontSize(18.px)
                        fontWeight(600)
                        color(Color("#2d3748"))
                        marginBottom(4.px)
                    }
                }) {
                    Text("Оплата")
                }
                Div({ style { color(Color("#718096")); fontSize(14.px) } }) {
                    Text("${payment.student.name} ${payment.student.secondName}")
                }
            }
            Div({
                style {
                    fontSize(20.px)
                    fontWeight(700)
                    color(Color("#48bb78"))
                }
            }) {
                Text("${payment.amount} ₽")
            }
        }

        Div({ style { display(DisplayStyle.Flex); property("gap", "8px") } }) {
            PrimaryButton("Изменить") { onClick() }
        }
    }
}

@Composable
fun StudentCard(student: StudentDto) {
    val fullName = "${student.name} ${student.secondName} ${student.lastName}".trim()
    Div({
        style {
            backgroundColor(Color("#f7fafc"))
            padding(16.px)
            borderRadius(12.px)
            marginBottom(12.px)
            cursor("pointer")
            property("transition", "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)")
            border(2.px, LineStyle.Solid, Color.transparent)
        }
    }) {
        Div({
            style {
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.SpaceBetween)
                alignItems(AlignItems.Center)
                marginBottom(8.px)
            }
        }) {
            Div({
                style {
                    fontWeight(600)
                    color(Color("#2d3748"))
                    fontSize(16.px)
                }
            }) {
                Text(fullName)
            }
            Div({
                style {
                    color(Color("#718096"))
                    fontSize(13.px)
                }
            }) {
                Text("#${student.id}")
            }
        }
        Div({
            style {
                color(Color("#4a5568"))
                fontSize(14.px)
                display(DisplayStyle.Grid)
                property("grid-template-columns", "repeat(auto-fit, minmax(150px, 1fr))")
                property("gap", "8px")
            }
        }) {
            student.schoolClass?.let { schoolClass ->
                Div {
                    Div({
                        style {
                            fontSize(11.px)
                            color(Color("#718096"))
                            property("text-transform", "uppercase")
                            fontWeight(600)
                            property("letter-spacing", "0.5px")
                        }
                    }) {
                        Text("Класс")
                    }
                    Div({
                        style {
                            fontSize(14.px)
                            color(Color("#2d3748"))
                        }
                    }) {
                        Text(schoolClass.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherCard(teacher: TutorDto) {
    Div({
        style {
            backgroundColor(Color.white)
            borderRadius(12.px)
            padding(16.px)
            marginBottom(12.px)
            border(1.px, LineStyle.Solid, Color("#e2e8f0"))
            property("box-shadow", "0 1px 3px rgba(0, 0, 0, 0.1)")
            property("transition", "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)")
            property("animation", "cardFadeIn 0.4s ease-out")
        }
    }) {
        Div({
            style {
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.SpaceBetween)
                alignItems(AlignItems.Center)
                marginBottom(12.px)
            }
        }) {
            Div {
                Span({
                    style {
                        fontWeight(600)
                        fontSize(16.px)
                        color(Color("#2d3748"))
                    }
                }) {
                    Text(teacher.name)
                }
                Span({
                    style {
                        color(Color("#718096"))
                        fontSize(12.px)
                        marginLeft(8.px)
                    }
                }) {
                    Text("#${teacher.id}")
                }
            }
            Button({
                classes(AppStyles.button)
                style {
                    padding(6.px, 12.px)
                    fontSize(13.px)
                }
            }) {
                Text("Изменить")
            }
        }

        if (teacher.permissions.isNotEmpty()) {
            Div({ style { marginBottom(12.px) } }) {
                Div({
                    style {
                        fontSize(13.px)
                        fontWeight(600)
                        color(Color("#4a5568"))
                        marginBottom(6.px)
                    }
                }) {
                    Text("Разрешения")
                }
                Div {
                    teacher.permissions.forEach { permission ->
                        Span({
                            style {
                                display(DisplayStyle.InlineBlock)
                                padding(4.px, 10.px)
                                margin(2.px, 4.px, 2.px, 0.px)
                                borderRadius(6.px)
                                fontSize(12.px)
                                fontWeight(500)
                                backgroundColor(Color("#edf2f7"))
                                color(Color("#2d3748"))
                            }
                        }) {
                            Text(permission)
                        }
                    }
                }
            }
        }

        if (teacher.subjects.isNotEmpty()) {
            Div {
                Div({
                    style {
                        fontSize(13.px)
                        fontWeight(600)
                        color(Color("#4a5568"))
                        marginBottom(6.px)
                    }
                }) {
                    Text("Предметы")
                }
                Div {
                    teacher.subjects.forEach { subject ->
                        Span({
                            style {
                                display(DisplayStyle.InlineBlock)
                                padding(4.px, 10.px)
                                margin(2.px, 4.px, 2.px, 0.px)
                                borderRadius(6.px)
                                fontSize(12.px)
                                fontWeight(500)
                                backgroundColor(Color("#bee3f8"))
                                color(Color("#2c5282"))
                            }
                        }) {
                            Text(subject.name)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogCard(log: AuditLogEntryDto) {
    Div({
        classes(AppStyles.card)
        style {
            marginBottom(8.px)
            padding(12.px)
            cursor("pointer")
        }
    }) {
        Div({
            style {
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.SpaceBetween)
                alignItems(AlignItems.Start)
                marginBottom(6.px)
            }
        }) {
            Div({ style { flex("1"); property("min-width", "0") } }) {
                Div({
                    style {
                        fontSize(13.px)
                        color(Color("#718096"))
                        marginBottom(2.px)
                    }
                }) {
                    Text(log.timestamp)
                }
                Div({
                    style {
                        fontSize(14.px)
                        fontWeight(600)
                        color(Color("#2d3748"))
                    }
                }) {
                    Text(log.userName)
                }
                Div({
                    style {
                        fontSize(13.px)
                        color(Color("#4a5568"))
                        marginTop(2.px)
                    }
                }) {
                    Text(log.details)
                }
            }
            Span({
                style {
                    fontSize(11.px)
                    padding(4.px, 8.px)
                    property("white-space", "nowrap")
                    borderRadius(12.px)
                    fontWeight(600)
                    when {
                        log.action.contains("create", ignoreCase = true) -> {
                            backgroundColor(Color("#c6f6d5"))
                            color(Color("#22543d"))
                        }
                        log.action.contains("update", ignoreCase = true) -> {
                            backgroundColor(Color("#bee3f8"))
                            color(Color("#2c5282"))
                        }
                        log.action.contains("delete", ignoreCase = true) -> {
                            backgroundColor(Color("#fed7d7"))
                            color(Color("#c53030"))
                        }
                        else -> {
                            backgroundColor(Color("#feebc8"))
                            color(Color("#7c2d12"))
                        }
                    }
                }
            }) {
                Text(log.action)
            }
        }
    }
}

@Composable
fun ReportContent(report: ReportDto) {
    Div({ style { marginTop(20.px) } }) {
        H3({ style { marginBottom(16.px) } }) {
            Text("Итого")
        }

        // Summary stats
        Div({
            style {
                display(DisplayStyle.Grid)
                property("grid-template-columns", "repeat(auto-fit, minmax(150px, 1fr))")
                property("gap", "12px")
                marginBottom(20.px)
            }
        }) {
            SummaryCard("Занятий", report.lessons.size.toString())
            SummaryCard("Оплат", report.payments.size.toString())
            SummaryCard("Учеников", report.students.size.toString())
        }

        // Students list
        if (report.students.isNotEmpty()) {
            H3({ style { marginTop(20.px); marginBottom(16.px) } }) {
                Text("Ученики")
            }
            report.students.forEach { studentReport ->
                Div({
                    style {
                        backgroundColor(Color("#f7fafc"))
                        padding(12.px)
                        borderRadius(8.px)
                        marginBottom(8.px)
                    }
                }) {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            justifyContent(JustifyContent.SpaceBetween)
                            alignItems(AlignItems.Center)
                        }
                    }) {
                        Div {
                            Span({
                                style {
                                    fontWeight(600)
                                    color(Color("#2d3748"))
                                }
                            }) {
                                Text("${studentReport.student.name} ${studentReport.student.secondName} ${studentReport.student.lastName}".trim())
                            }
                            Span({
                                style {
                                    color(Color("#718096"))
                                    fontSize(14.px)
                                    marginLeft(8.px)
                                }
                            }) {
                                Text(studentReport.student.schoolClass?.toString() ?: "")
                            }
                        }
                        Div({ style { property("text-align", "right") } }) {
                            Div({
                                style {
                                    fontSize(14.px)
                                    color(Color("#4a5568"))
                                }
                            }) {
                                Text("${studentReport.lessonsCount} занятий")
                            }
                            Div({
                                style {
                                    fontWeight(600)
                                    color(Color("#667eea"))
                                }
                            }) {
                                Text("${studentReport.totalAmount} ₽")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsContent(stats: StatisticsDto) {
    Div({ style { marginTop(20.px) } }) {
        // Summary
        Div({
            style {
                display(DisplayStyle.Grid)
                property("grid-template-columns", "repeat(auto-fit, minmax(150px, 1fr))")
                property("gap", "12px")
                marginBottom(20.px)
            }
        }) {
            SummaryCard("Занятий", stats.totalLessons.toString())
            SummaryCard("Индивид.", stats.individualLessons.toString())
            SummaryCard("Групповых", stats.groupLessons.toString())
            SummaryCard("Доход", "${stats.totalIncome} ₽")
            SummaryCard("Оплат", "${stats.totalPayments} ₽")
            SummaryCard("Учеников", stats.uniqueStudents.toString())
        }

        // Subject breakdown
        if (stats.subjectBreakdown.isNotEmpty()) {
            H3({ style { marginTop(20.px); marginBottom(16.px) } }) {
                Text("По предметам")
            }
            stats.subjectBreakdown.forEach { subjectStats ->
                Div({
                    style {
                        backgroundColor(Color("#f7fafc"))
                        padding(12.px)
                        borderRadius(8.px)
                        marginBottom(12.px)
                    }
                }) {
                    Div({
                        style {
                            fontWeight(600)
                            color(Color("#2d3748"))
                            marginBottom(8.px)
                        }
                    }) {
                        Text(subjectStats.subject)
                    }
                    Div({
                        style {
                            display(DisplayStyle.Grid)
                            property("grid-template-columns", "repeat(auto-fit, minmax(100px, 1fr))")
                            property("gap", "8px")
                            fontSize(14.px)
                        }
                    }) {
                        Div {
                            Div({ style { color(Color("#718096")) } }) {
                                Text("Занятий")
                            }
                            Div({
                                style {
                                    fontWeight(600)
                                    color(Color("#4a5568"))
                                }
                            }) {
                                Text(subjectStats.lessonsCount.toString())
                            }
                        }
                        Div {
                            Div({ style { color(Color("#718096")) } }) {
                                Text("Инд/Групп")
                            }
                            Div({
                                style {
                                    fontWeight(600)
                                    color(Color("#4a5568"))
                                }
                            }) {
                                Text("${subjectStats.individualCount}/${subjectStats.groupCount}")
                            }
                        }
                        Div {
                            Div({ style { color(Color("#718096")) } }) {
                                Text("Доход")
                            }
                            Div({
                                style {
                                    fontWeight(600)
                                    color(Color("#667eea"))
                                }
                            }) {
                                Text("${subjectStats.totalIncome} ₽")
                            }
                        }
                        Div {
                            Div({ style { color(Color("#718096")) } }) {
                                Text("Учеников")
                            }
                            Div({
                                style {
                                    fontWeight(600)
                                    color(Color("#4a5568"))
                                }
                            }) {
                                Text(subjectStats.studentsCount.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}
