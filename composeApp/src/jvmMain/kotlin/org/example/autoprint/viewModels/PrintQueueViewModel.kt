package org.example.autoprint.viewModels

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.example.autoprint.repositories.FirestoreRepository
import org.example.autoprint.utils.DocumentDownloader
import org.example.autoprint.services.PrintService
import org.example.autoprint.services.PrintQueueManager
import org.example.autoprint.services.PrintJobStatus
import org.example.autoprint.models.PrintOrder
import org.example.autoprint.models.DownloadStatus
import java.io.File

class PrintQueueViewModel(
    private val repository: FirestoreRepository,
    private val downloader: DocumentDownloader
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val printService = PrintService()
    private val printQueueManager = PrintQueueManager(printService) { orderId ->
        // Callback when order is printed - update Firebase status
        repository.updateOrderStatus(orderId, "PRINTED")
    }

    var printOrders by mutableStateOf<List<PrintOrder>>(emptyList())
        private set

    var printedOrders by mutableStateOf<List<PrintOrder>>(emptyList())
        private set

    var downloadStates by mutableStateOf<Map<String, DownloadStatus>>(emptyMap())
        private set

    var downloadedFiles by mutableStateOf<Map<String, File>>(emptyMap())
        private set

    var printStatuses by mutableStateOf<Map<String, PrintJobStatus>>(emptyMap())
        private set

    var availablePrinters by mutableStateOf<List<String>>(emptyList())
        private set

    var defaultPrinter by mutableStateOf<String?>(null)
        private set

    val isPrinting: Boolean get() = printQueueManager.isRunning

    init {
        observePrintOrders()
        observePrintStatuses()
        loadPrinterInfo()
    }

    private fun observePrintOrders() {
        scope.launch {
            repository.listenToPrintOrders().collect { orders ->
                println("📋 Received ${orders.size} orders from repository")
                orders.forEach { order ->
                    println("📄 Order: ${order.orderId} - paid: ${order.paid}, inQueue: ${order.inQueue}, status: ${order.orderStatus}")
                }

                // Separate printed orders from queue
                val (printed, nonPrinted) = orders.partition { it.orderStatus == "PRINTED" }
                printedOrders = printed
                printOrders = nonPrinted

                // Auto-add paid orders to queue
                nonPrinted.filter { it.paid && !it.inQueue }.forEach { order ->
                    println("🎯 Adding paid order to queue: ${order.orderId}")
                    scope.launch {
                        try {
                            repository.updateOrderInQueue(order.orderId, true)
                        } catch (e: Exception) {
                            println("❌ Failed to add order to queue: ${e.message}")
                        }
                    }
                }

                // Auto-download documents for orders in queue
                nonPrinted.filter { it.paid && it.inQueue }.forEach { order ->
                    if (order.documentUrl.isNotEmpty() &&
                        !downloadedFiles.containsKey(order.orderId) &&
                        downloadStates[order.orderId] !is DownloadStatus.Downloading) {
                        println("🔽 Starting download for order: ${order.orderId}")
                        downloadDocument(order)
                    }
                }
            }
        }
    }

    private fun observePrintStatuses() {
        scope.launch {
            printQueueManager.printStatuses.collect { statuses ->
                printStatuses = statuses
            }
        }
    }

    private fun loadPrinterInfo() {
        scope.launch {
            availablePrinters = printService.getAvailablePrinters()
            defaultPrinter = printService.getDefaultPrinter()
        }
    }

    private fun downloadDocument(order: PrintOrder) {
        scope.launch {
            downloader.downloadDocument(order.documentUrl, order.documentName)
                .collect { (status, file) ->
                    downloadStates = downloadStates + (order.orderId to status)
                    if (file != null && status is DownloadStatus.Completed) {
                        downloadedFiles = downloadedFiles + (order.orderId to file)
                    }
                }
        }
    }

    fun startPrinting() {
        val readyOrders = printOrders.filter { order ->
            order.paid && order.inQueue &&
                    downloadedFiles.containsKey(order.orderId) &&
                    downloadStates[order.orderId] is DownloadStatus.Completed
        }

        if (readyOrders.isNotEmpty()) {
            printQueueManager.startPrinting(readyOrders, downloadedFiles)
        }
    }

    fun stopPrinting() {
        printQueueManager.stopPrinting()
    }

    fun clearCompletedJobs() {
        printQueueManager.clearCompletedJobs()
    }

    fun refreshPrinters() {
        loadPrinterInfo()
    }
}