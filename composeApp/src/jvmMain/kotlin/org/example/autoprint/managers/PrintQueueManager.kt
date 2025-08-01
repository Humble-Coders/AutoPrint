package org.example.autoprint.services

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.example.autoprint.models.PrintOrder
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

data class PrintJobStatus(
    val orderId: String,
    val status: PrintStatus,
    val message: String = "",
    val progress: String = ""
)

enum class PrintStatus {
    WAITING, PRINTING, COMPLETED, FAILED, CANCELLED
}

class PrintQueueManager(
    private val printService: PrintService,
    private val onOrderPrinted: suspend (String) -> Unit = {} // Callback when order is printed
) {
    private val logger = Logger.getLogger("PrintQueueManager")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _isRunning = AtomicBoolean(false)
    private val _printStatuses = MutableStateFlow<Map<String, PrintJobStatus>>(emptyMap())
    private var printJob: Job? = null

    val printStatuses: StateFlow<Map<String, PrintJobStatus>> = _printStatuses.asStateFlow()
    val isRunning: Boolean get() = _isRunning.get()

    fun startPrinting(
        orders: List<PrintOrder>,
        downloadedFiles: Map<String, File>
    ) {
        if (_isRunning.get()) {
            logger.warning("⚠️ Print queue already running, ignoring start request")
            return
        }

        logger.info("🚀 Starting print queue with ${orders.size} orders")
        _isRunning.set(true)

        // Initialize all orders as waiting
        val initialStatuses = orders.associate { order ->
            order.orderId to PrintJobStatus(
                orderId = order.orderId,
                status = PrintStatus.WAITING,
                message = "Waiting in queue..."
            )
        }
        _printStatuses.value = initialStatuses
        logger.info("📋 Initialized ${initialStatuses.size} orders in print queue")

        printJob = scope.launch {
            try {
                processQueue(orders, downloadedFiles)
            } catch (e: Exception) {
                logger.severe("❌ Print queue processing error: ${e.message}")
            } finally {
                _isRunning.set(false)
                logger.info("🏁 Print queue finished")
            }
        }
    }

    fun stopPrinting() {
        logger.info("🛑 Stopping print queue")
        printJob?.cancel()
        _isRunning.set(false)

        // Update all waiting/printing jobs to cancelled
        val updatedStatuses = _printStatuses.value.mapValues { (orderId, status) ->
            when (status.status) {
                PrintStatus.WAITING, PrintStatus.PRINTING -> {
                    logger.info("❌ Cancelling order: $orderId")
                    status.copy(status = PrintStatus.CANCELLED, message = "Cancelled by user")
                }
                else -> status
            }
        }
        _printStatuses.value = updatedStatuses
    }

    private suspend fun processQueue(
        orders: List<PrintOrder>,
        downloadedFiles: Map<String, File>
    ) {
        logger.info("🔄 Processing ${orders.size} orders in queue")

        for ((index, order) in orders.withIndex()) {
            if (!_isRunning.get()) {
                logger.info("🛑 Print queue stopped, breaking out of processing")
                break
            }

            logger.info("📄 Processing order ${index + 1}/${orders.size}: ${order.orderId}")

            val file = downloadedFiles[order.orderId]
            if (file == null || !file.exists()) {
                logger.warning("❌ File not found for order: ${order.orderId}")
                updateStatus(order.orderId, PrintStatus.FAILED, "File not found")
                continue
            }

            logger.info("🖨️ Starting print job for: ${order.documentName}")
            updateStatus(order.orderId, PrintStatus.PRINTING, "Starting print job...")

            try {
                printService.printDocument(
                    file = file,
                    order = order,
                    onProgress = { progress ->
                        logger.info("📊 Print progress for ${order.orderId}: $progress")
                        updateProgress(order.orderId, progress)
                    },
                    onComplete = { success, message ->
                        val status = if (success) PrintStatus.COMPLETED else PrintStatus.FAILED
                        logger.info("${if (success) "✅" else "❌"} Print job completed for ${order.orderId}: $message")
                        updateStatus(order.orderId, status, message ?: "Unknown error")

                        // If successful, mark as printed in Firebase
                        if (success) {
                            scope.launch {
                                try {
                                    logger.info("🔄 Updating Firebase status to PRINTED for: ${order.orderId}")
                                    onOrderPrinted(order.orderId)
                                } catch (e: Exception) {
                                    logger.severe("❌ Failed to update Firebase status for ${order.orderId}: ${e.message}")
                                }
                            }
                        }
                    }
                )

                // Wait a bit between print jobs
                logger.info("⏳ Waiting 2 seconds before next print job...")
                delay(2000)

            } catch (e: Exception) {
                logger.severe("❌ Print error for order ${order.orderId}: ${e.message}")
                updateStatus(order.orderId, PrintStatus.FAILED, "Error: ${e.message}")
            }
        }

        logger.info("🏁 Queue processing completed")
    }

    private fun updateStatus(orderId: String, status: PrintStatus, message: String) {
        val currentStatuses = _printStatuses.value.toMutableMap()
        currentStatuses[orderId] = PrintJobStatus(
            orderId = orderId,
            status = status,
            message = message
        )
        _printStatuses.value = currentStatuses
    }

    private fun updateProgress(orderId: String, progress: String) {
        val currentStatuses = _printStatuses.value.toMutableMap()
        val currentStatus = currentStatuses[orderId]
        if (currentStatus != null && currentStatus.status == PrintStatus.PRINTING) {
            currentStatuses[orderId] = currentStatus.copy(progress = progress)
            _printStatuses.value = currentStatuses
        }
    }

    fun clearCompletedJobs() {
        val filteredStatuses = _printStatuses.value.filterValues {
            it.status !in setOf(PrintStatus.COMPLETED, PrintStatus.FAILED, PrintStatus.CANCELLED)
        }
        _printStatuses.value = filteredStatuses
    }
}