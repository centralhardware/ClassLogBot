package me.centralhardware.znatoki.telegram.statistic.api

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import me.centralhardware.znatoki.telegram.statistic.entity.*
import me.centralhardware.znatoki.telegram.statistic.mapper.PaymentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.StudentMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.SubjectMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.TutorMapper
import me.centralhardware.znatoki.telegram.statistic.mapper.AuditLogMapper
import me.centralhardware.znatoki.telegram.statistic.service.DiffService
import me.centralhardware.znatoki.telegram.statistic.exception.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class PaymentDto(
    val id: Int,
    val dateTime: String,
    val tutorName: String,
    val subjectName: String,
    val studentName: String,
    val amount: Int,
    val photoReport: String?
)

@Serializable
data class UpdatePaymentRequest(
    val amount: Int
)

@Serializable
data class CreatePaymentRequest(
    val subjectId: Long,
    val studentId: Int,
    val amount: Int,
    val photoReport: String
)

fun Payment.toPaymentDto(): PaymentDto {
    val tutor = TutorMapper.findByIdOrNull(tutorId)
    val subject = SubjectMapper.getNameById(subjectId)
    val student = StudentMapper.findById(studentId)

    return PaymentDto(
        id = id!!.id,
        dateTime = dateTime.format(DateTimeFormatter.ofPattern("HH:mm")),
        tutorName = tutor?.name ?: "Unknown",
        subjectName = subject ?: "Unknown",
        studentName = student.fio(),
        amount = amount.amount,
        photoReport = photoReport?.let { "/api/image/$it" }
    )
}



fun Route.paymentApi() {
    route("/api/payments") {
        get {
            val tutorId = call.authenticatedTutorId
            val payments = PaymentMapper.getTodayPayments(tutorId)
                .map { it.toPaymentDto() }
                .sortedBy { it.dateTime }

            KSLog.info("PaymentsApi.GET: User ${tutorId.id} loaded today's payments")
            call.respond(payments)
        }

        post {
            requires(Permissions.ADD_PAYMENT)
            val tutorId = call.authenticatedTutorId
            val request = call.receive<CreatePaymentRequest>()

            if (request.amount <= 0) {
                throw ValidationException("Amount must be positive")
            }

            val studentId = request.studentId.toStudentId()
            val subjectId = SubjectId(request.subjectId)

            val payment = Payment(
                id = null,
                dateTime = LocalDateTime.now(),
                tutorId = tutorId,
                studentId = studentId,
                amount = request.amount.toAmount(),
                subjectId = subjectId,
                deleted = false,
                photoReport = request.photoReport,
                addedByTutorId = tutorId
            )

            val paymentId = PaymentMapper.insert(payment)
            val createdPayment = PaymentMapper.findById(paymentId)!!

            val student = StudentMapper.findById(studentId)
            val subject = SubjectMapper.getNameById(subjectId)

            AuditLogMapper.log(
                userId = tutorId.id,
                action = "CREATE_PAYMENT",
                entityType = "payment",
                entityId = paymentId.id,
                studentId = request.studentId,
                subjectId = request.subjectId.toInt(),
                null,
                createdPayment
            )

            KSLog.info("PaymentsApi.POST: User ${tutorId.id} created payment for student ${request.studentId}")
            call.respond(HttpStatusCode.Created, createdPayment.toPaymentDto())
        }

        put("/{id}") {
            requires(Permissions.ADD_PAYMENT)
            val paymentIdStr = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("Invalid payment ID")
            val tutorId = call.authenticatedTutorId
            val request = call.receive<UpdatePaymentRequest>()

            if (request.amount <= 0) {
                throw ValidationException("Amount must be positive")
            }

            val paymentId = PaymentId(paymentIdStr)
            val oldPayment = PaymentMapper.findById(paymentId) ?: throw NotFoundException("Payment not found")

            if (oldPayment.amount.amount == request.amount) {
                KSLog.info("PaymentsApi.PUT: User ${tutorId.id} attempted to update payment $paymentIdStr with no changes")
                call.respond(HttpStatusCode.OK, oldPayment.toPaymentDto())
                return@put
            }

            PaymentMapper.updateAmount(paymentId, request.amount.toAmount())

            val updatedPayment = PaymentMapper.findById(paymentId)!!

            AuditLogMapper.log(
                userId = tutorId.id,
                action = "UPDATE_PAYMENT",
                entityType = "payment",
                entityId = paymentIdStr,
                studentId = oldPayment.studentId.id,
                subjectId = oldPayment.subjectId.id.toInt(),
                oldPayment,
                updatedPayment
            )

            KSLog.info("PaymentsApi.PUT: User ${tutorId.id} updated payment $paymentIdStr")
            call.respond(HttpStatusCode.OK, updatedPayment.toPaymentDto())
        }

        delete("/{id}") {
            requires(Permissions.ADD_PAYMENT)
            val paymentIdStr = call.parameters["id"]?.toIntOrNull() ?: throw BadRequestException("Invalid payment ID")
            val tutorId = call.authenticatedTutorId

            val paymentId = PaymentId(paymentIdStr)
            val payment = PaymentMapper.findById(paymentId) ?: throw NotFoundException("Payment not found")

            PaymentMapper.setDelete(paymentId, true)

            AuditLogMapper.log(
                userId = tutorId.id,
                action = "DELETE_PAYMENT",
                entityType = "payment",
                entityId = paymentIdStr,
                studentId = payment.studentId.id,
                subjectId = payment.subjectId.id.toInt(),
                payment,
                null
            )

            KSLog.info("PaymentsApi.DELETE: User ${tutorId.id} deleted payment $paymentIdStr")
            call.respond(HttpStatusCode.OK, mapOf("success" to true))
        }
    }
}
