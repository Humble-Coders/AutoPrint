package org.example.autoprint.viewModels

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.example.autoprint.repositories.FirestoreRepository
import org.example.autoprint.utils.DocumentDownloader
import org.example.autoprint.models.PrintOrder
import org.example.autoprint.models.DownloadStatus
import java.io.File
import java.util.Collections.emptyList
import java.util.Collections.emptyMap

class PrintQueueViewModel(
    private val repository: FirestoreRepository,
    private val downloader: DocumentDownloader
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var printOrders by mutableStateOf<List<PrintOrder>>(emptyList())
        private set

    var downloadStates by mutableStateOf<Map<String, DownloadStatus>>(emptyMap())
        private set

    var downloadedFiles by mutableStateOf<Map<String, File>>(emptyMap())
        private set

    init {
        observePrintOrders()
    }

    private fun observePrintOrders() {
        scope.launch {
            repository.listenToPrintOrders().collect { orders ->
                printOrders = orders
                // Auto-download new documents
                orders.forEach { order ->
                    if (order.documentUrl.isNotEmpty() &&
                        !downloadedFiles.containsKey(order.orderId) &&
                        downloadStates[order.orderId] !is DownloadStatus.Downloading) {
                        downloadDocument(order)
                    }
                }
            }
        }
    }

    private fun downloadDocument(order: PrintOrder) {
        scope.launch {
            downloader.downloadDocument(order.documentUrl, order.documentName)
                .collect { pair ->
                    val status = pair.first
                    val file = pair.second
                    downloadStates = downloadStates + (order.orderId to status)
                    if (file != null && status is DownloadStatus.Completed) {
                        downloadedFiles = downloadedFiles + (order.orderId to file)
                    }
                }
        }
    }
}