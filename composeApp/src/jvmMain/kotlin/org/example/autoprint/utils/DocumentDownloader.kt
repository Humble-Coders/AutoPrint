package org.example.autoprint.utils

// Remove this import:
// import com.google.firebase.database.utilities.Pair

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.example.autoprint.models.DownloadStatus
import java.io.File

class DocumentDownloader {
    private val client = HttpClient(CIO)

    fun downloadDocument(url: String, fileName: String): Flow<Pair<DownloadStatus, File?>> = flow {
        try {
            emit(DownloadStatus.Downloading(0f) to null)

            val response = client.get(url)
            val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
            val downloadsDir = File(System.getProperty("user.home"), "Downloads/PrintShop")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val file = File(downloadsDir, fileName)
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(8192)
            var totalBytes = 0L

            file.outputStream().use { output ->
                while (!channel.isClosedForRead) {
                    val bytes = channel.readAvailable(buffer)
                    if (bytes > 0) {
                        output.write(buffer, 0, bytes)
                        totalBytes += bytes
                        if (contentLength > 0) {
                            val progress = totalBytes.toFloat() / contentLength.toFloat()
                            emit(DownloadStatus.Downloading(progress) to null)
                        }
                    }
                }
            }

            emit(DownloadStatus.Completed to file)
        } catch (e: Exception) {
            emit(DownloadStatus.Error(e.message ?: "Download failed") to null)
        }
    }
}
