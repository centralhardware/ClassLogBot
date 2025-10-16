package me.centralhardware.znatoki.telegram.statistic.dto

import kotlinx.serialization.Serializable

// ===== Core Entity DTOs =====

@Serializable
data class TutorDto(
    val id: Long,
    val name: String,
    val permissions: List<String> = emptyList(),
    val subjects: List<SubjectDto> = emptyList()
)

@Serializable
data class SubjectDto(
    val id: Long,
    val name: String
)

@Serializable
data class StudentDto(
    val id: Int,
    val name: String,
    val secondName: String,
    val lastName: String,
    val schoolClass: Int? = null,
    val recordDate: String? = null,
    val birthDate: String? = null,
    val source: String? = null,
    val phone: String? = null,
    val responsiblePhone: String? = null,
    val motherFio: String? = null
)

@Serializable
data class LessonDto(
    val id: String,
    val dateTime: String,
    val students: List<StudentDto>,
    val subject: SubjectDto,
    val amount: Double,
    val isGroup: Boolean,
    val isExtra: Boolean,
    val tutorId: Long,
    val photoReport: String? = null
)

@Serializable
data class PaymentDto(
    val id: Int,
    val dateTime: String,
    val student: StudentDto,
    val subject: SubjectDto,
    val amount: Int,
    val tutorId: Long,
    val photoReport: String? = null
)

// ===== Audit Log DTOs =====

@Serializable
data class AuditLogEntryDto(
    val id: Int,
    val timestamp: String,
    val userName: String,
    val action: String,
    val entityType: String,
    val entityId: String?,
    val details: String,
    val diff: String?
)

@Serializable
data class AuditLogDto(
    val id: Int,
    val userId: Long,
    val userName: String?,
    val action: String,
    val entityType: String?,
    val entityId: String?,
    val details: String?,
    val timestamp: String,
    val studentName: String?,
    val subject: String?
)

@Serializable
data class AuditLogResponse(
    val logs: List<AuditLogDto>,
    val total: Long,
    val limit: Int,
    val offset: Long
)

// ===== User DTOs =====

@Serializable
data class CurrentUserDto(
    val tutorId: Long,
    val name: String,
    val isAdmin: Boolean,
    val subjects: List<SubjectDto>,
    val permissions: List<String>
)

// ===== Report DTOs =====

@Serializable
data class ReportDto(
    val lessons: List<LessonDto>,
    val payments: List<PaymentDto>,
    val students: List<StudentReportDto>
)

@Serializable
data class StudentReportDto(
    val student: StudentDto,
    val lessonsCount: Int,
    val totalAmount: Int,
    val totalPaid: Int,
    val balance: Int
)

@Serializable
data class ReportDataDto(
    val tutorName: String,
    val subjectName: String,
    val month: String,
    val year: Int,
    val students: List<StudentReportDtoLocal>,
    val totalIndividual: Int,
    val totalGroup: Int,
    val totalPayments: Int,
    val lessons: List<LessonReportDto>,
    val payments: List<PaymentReportDto>
)

@Serializable
data class StudentReportDtoLocal(
    val studentId: Int,
    val fio: String,
    val schoolClass: String,
    val individual: Int,
    val group: Int,
    val payment: Int,
    val dates: String
)

@Serializable
data class LessonReportDto(
    val dateTime: String,
    val students: List<String>,
    val amount: Double,
    val forceGroup: Boolean,
    val extraHalfHour: Boolean,
    val photoReportLink: String?
)

@Serializable
data class PaymentReportDto(
    val dateTime: String,
    val studentFio: String,
    val amount: Int,
    val photoReportLink: String?
)

// ===== Statistics DTOs =====

@Serializable
data class StatisticsDto(
    val totalLessons: Int,
    val individualLessons: Int,
    val groupLessons: Int,
    val totalIncome: Int,
    val totalPayments: Int,
    val uniqueStudents: Int,
    val subjectBreakdown: List<SubjectStatsDto>
)

@Serializable
data class SubjectStatsDto(
    val subject: String,
    val lessonsCount: Int,
    val individualCount: Int,
    val groupCount: Int,
    val totalIncome: Int,
    val studentsCount: Int
)

@Serializable
data class AggregatedStatsDto(
    val period: PeriodStatsDto,
    val previousPeriod: PeriodStatsDto,
    val comparison: ComparisonDto
)

@Serializable
data class PeriodStatsDto(
    val month: String,
    val year: Int,
    val totalLessons: Int,
    val totalIndividual: Int,
    val totalGroup: Int,
    val totalPayments: Int,
    val totalStudents: Int,
    val subjectStats: List<SubjectStatsDto>
)

@Serializable
data class ComparisonDto(
    val lessonsChange: Int,
    val lessonsChangePercent: Double,
    val paymentsChange: Int,
    val paymentsChangePercent: Double,
    val studentsChange: Int,
    val studentsChangePercent: Double
)

// ===== Student Detail DTOs =====

@Serializable
data class StudentLessonDto(
    val id: String,
    val dateTime: String,
    val subjectName: String,
    val amount: Double,
    val forceGroup: Boolean,
    val extraHalfHour: Boolean
)

@Serializable
data class StudentPaymentDto(
    val id: Int,
    val dateTime: String,
    val amount: Int
)

@Serializable
data class StudentDetailsDto(
    val lessons: List<StudentLessonDto>,
    val payments: List<StudentPaymentDto>
)

// ===== Request DTOs =====

@Serializable
data class UpdateStudentRequest(
    val name: String,
    val secondName: String,
    val lastName: String,
    val schoolClass: Int? = null,
    val recordDate: String? = null,
    val birthDate: String? = null,
    val source: String? = null,
    val phone: String? = null,
    val responsiblePhone: String? = null,
    val motherFio: String? = null
)

@Serializable
data class UpdateTutorRequest(
    val name: String? = null,
    val permissions: List<String>,
    val subjectIds: List<Long>
)

@Serializable
data class CreatePaymentRequest(
    val studentId: Int,
    val subjectId: Long,
    val amount: Int,
    val tutorId: Long,
    val photoReport: String? = null
)

@Serializable
data class UpdatePaymentRequest(
    val amount: Int,
    val subjectId: Long
)

@Serializable
data class CreateLessonRequest(
    val studentIds: List<Int>,
    val subjectId: Long,
    val amount: Int,
    val tutorId: Long,
    val forceGroup: Boolean = false,
    val extraHalfHour: Boolean = false,
    val photoReport: String? = null
)

@Serializable
data class UpdateLessonRequest(
    val studentId: Int,
    val subjectId: Long,
    val amount: Int,
    val forceGroup: Boolean,
    val extraHalfHour: Boolean
)

// ===== Response DTOs =====

@Serializable
data class ImageUploadResponse(
    val imageUrl: String
)

@Serializable
data class VersionResponse(
    val version: String
)
