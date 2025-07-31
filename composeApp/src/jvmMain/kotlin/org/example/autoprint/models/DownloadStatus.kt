package org.example.autoprint.models

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val progress: Float) : DownloadStatus()
    object Completed : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}
