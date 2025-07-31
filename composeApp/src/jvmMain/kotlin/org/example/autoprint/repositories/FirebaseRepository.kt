package org.example.autoprint.repositories



import com.google.cloud.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.example.autoprint.models.PrintOrder
import org.example.autoprint.models.PrintSettings
import java.sql.DriverManager.println
import java.time.format.DateTimeFormatter
import java.util.Collections.emptyList
import java.util.Collections.emptyMap
import java.util.logging.Logger

class FirestoreRepository(private val firestore: Firestore) {
    private val logger = Logger.getLogger("PrintShopApp")

    fun listenToPrintOrders(): Flow<List<PrintOrder>> = callbackFlow {
        logger.info("🔥 Starting to listen to print_orders collection...")

        val listener = firestore.collection("print_orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    logger.severe("❌ Error in snapshot listener: ${exception.message}")
                    exception.printStackTrace()
                    close(exception)
                    return@addSnapshotListener
                }

                logger.info("📊 Snapshot received!")
                logger.info("📊 Document count: ${snapshot?.documents?.size ?: 0}")

                if (snapshot?.documents?.isEmpty() == true) {
                    logger.warning("⚠️ No documents found in print_orders collection")
                }

                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        logger.info("📄 Processing document: ${doc.id}")

                        // Handle createdAt as Timestamp
                        val createdAtTimestamp = doc.getTimestamp("createdAt")
                        val createdAtString = createdAtTimestamp?.let { timestamp ->
                            val instant = timestamp.toDate().toInstant()
                            val localDateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                            localDateTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm:ss a"))
                        } ?: ""

                        // Handle updatedAt as Timestamp
                        val updatedAtTimestamp = doc.getTimestamp("updatedAt")
                        val updatedAtString = updatedAtTimestamp?.let { timestamp ->
                            val instant = timestamp.toDate().toInstant()
                            val localDateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                            localDateTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm:ss a"))
                        } ?: ""

                        val order = PrintOrder(
                            orderId = doc.getString("orderId") ?: "",
                            orderStatus = doc.getString("orderStatus") ?: "",
                            pageCount = doc.getLong("pageCount")?.toInt() ?: 0,
                            paid = doc.getBoolean("paid") ?: false,
                            paymentAmount = doc.getDouble("paymentAmount") ?: 0.0,
                            paymentStatus = doc.getString("paymentStatus") ?: "",
                            createdAt = createdAtString,
                            customerId = doc.getString("customerId") ?: "",
                            customerPhone = doc.getString("customerPhone") ?: "",
                            documentName = doc.getString("documentName") ?: "",
                            documentSize = doc.getLong("documentSize") ?: 0,
                            documentUrl = doc.getString("documentUrl") ?: "",
                            hasSettings = doc.getBoolean("hasSettings") ?: false,
                            inQueue = doc.getBoolean("inQueue") ?: false,
                            printSettings = doc.get("printSettings")?.let { settings ->
                                val settingsMap = settings as? Map<*, *> ?: emptyMap<String, Any>()
                                PrintSettings(
                                    colorMode = settingsMap["colorMode"] as? String ?: "COLOR",
                                    copies = (settingsMap["copies"] as? Long)?.toInt() ?: 1,
                                    customPages = settingsMap["customPages"] as? String ?: "",
                                    orientation = settingsMap["orientation"] as? String ?: "PORTRAIT",
                                    pagesToPrint = settingsMap["pagesToPrint"] as? String ?: "ALL",
                                    paperSize = settingsMap["paperSize"] as? String ?: "A4",
                                    quality = settingsMap["quality"] as? String ?: "HIGH"
                                )
                            } ?: PrintSettings(),
                            canAutoPrint = doc.getBoolean("canAutoPrint") ?: false,
                            queuePriority = doc.getLong("queuePriority")?.toInt() ?: 0,
                            razorpayOrderId = doc.getString("razorpayOrderId") ?: "",
                            razorpayPaymentId = doc.getString("razorpayPaymentId") ?: "",
                            updatedAt = updatedAtString
                        )

                        logger.info("✅ Successfully parsed order: ${order.orderId} - ${order.documentName}")
                        order
                    } catch (e: Exception) {
                        logger.severe("❌ Error parsing document ${doc.id}: ${e.message}")
                        e.printStackTrace()
                        null
                    }
                } ?: emptyList()

                logger.info("📋 Final orders list size: ${orders.size}")

                val sendResult = trySend(orders)
                if (sendResult.isFailure) {
                    logger.severe("❌ Failed to send orders to flow: ${sendResult.exceptionOrNull()?.message}")
                } else {
                    logger.info("✅ Successfully sent ${orders.size} orders to flow")
                }
            }

        logger.info("🎯 Snapshot listener attached successfully")
        awaitClose {
            logger.info("🔒 Closing Firestore listener")
            listener.remove()
        }
    }
}