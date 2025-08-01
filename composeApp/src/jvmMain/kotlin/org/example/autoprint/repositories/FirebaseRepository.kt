package org.example.autoprint.repositories

import com.google.cloud.firestore.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.example.autoprint.models.PrintOrder
import org.example.autoprint.models.PrintSettings
import java.time.format.DateTimeFormatter
import java.util.logging.Logger

class FirestoreRepository(private val firestore: Firestore) {
    private val logger = Logger.getLogger("PrintShopApp")

    suspend fun updateOrderStatus(orderId: String, status: String) {
        try {
            logger.info("🔄 Updating order status for: $orderId to $status")
            firestore.collection("print_orders")
                .document(orderId)
                .update("orderStatus", status)
                .get() // Wait for completion
            logger.info("✅ Successfully updated status for order: $orderId")
        } catch (e: Exception) {
            logger.severe("❌ Failed to update status for order $orderId: ${e.message}")
            throw e
        }
    }

    suspend fun updateOrderInQueue(orderId: String, inQueue: Boolean) {
        try {
            logger.info("🔄 Updating inQueue status for order: $orderId to $inQueue")
            firestore.collection("print_orders")
                .document(orderId)
                .update("inQueue", inQueue)
                .get() // Wait for completion
            logger.info("✅ Successfully updated inQueue for order: $orderId")
        } catch (e: Exception) {
            logger.severe("❌ Failed to update inQueue for order $orderId: ${e.message}")
            throw e
        }
    }

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
                            paid = doc.getBoolean("isPaid") ?: false, // Changed from "paid" to "isPaid"
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
                                println("🔧 Print settings map: $settingsMap") // Debug log
                                PrintSettings(
                                    colorMode = settingsMap["colorMode"] as? String ?: "COLOR",
                                    copies = when (val copiesValue = settingsMap["copies"]) {
                                        is Long -> copiesValue.toInt()
                                        is Int -> copiesValue
                                        is String -> copiesValue.toIntOrNull() ?: 1
                                        else -> 1
                                    },
                                    customPages = settingsMap["customPages"] as? String ?: "",
                                    orientation = settingsMap["orientation"] as? String ?: "PORTRAIT",
                                    pagesToPrint = settingsMap["pagesToPrint"] as? String ?: "ALL",
                                    paperSize = settingsMap["paperSize"] as? String ?: "A4",
                                    quality = settingsMap["quality"] as? String ?: "HIGH"
                                ).also { parsedSettings ->
                                    println("✅ Parsed settings - copies: ${parsedSettings.copies}")
                                }
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