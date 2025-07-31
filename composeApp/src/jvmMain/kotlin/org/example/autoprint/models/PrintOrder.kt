package org.example.autoprint.models

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class PrintOrder(
    val orderId: String = "",
    val orderStatus: String = "",
    val pageCount: Int = 0,
    val paid: Boolean = false,
    val paymentAmount: Double = 0.0,
    val paymentStatus: String = "",
    val createdAt: String = "",
    val customerId: String = "",
    val customerPhone: String = "",
    val documentName: String = "",
    val documentSize: Long = 0,
    val documentUrl: String = "",
    val hasSettings: Boolean = false,
    val inQueue: Boolean = false,
    val printSettings: PrintSettings = PrintSettings(),
    val canAutoPrint: Boolean = false,
    val queuePriority: Int = 0,
    val razorpayOrderId: String = "",
    val razorpayPaymentId: String = "",
    val updatedAt: String = ""
) {
    fun getFormattedDateTime(): String {
        return try {
            val dateTime = LocalDateTime.parse(createdAt.replace(" UTC+5:30", ""),
                DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm:ss a"))
            dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        } catch (e: Exception) {
            createdAt
        }
    }
}

@Serializable
data class PrintSettings(
    val colorMode: String = "COLOR",
    val copies: Int = 1,
    val customPages: String = "",
    val orientation: String = "PORTRAIT",
    val pagesToPrint: String = "ALL",
    val paperSize: String = "A4",
    val quality: String = "HIGH"
)