package org.example.autoprint

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.example.autoprint.screens.PrintQueueScreen
import org.example.autoprint.utils.PrintShopInitializer
import java.io.File

fun main() {
    // Define the path to your Firebase service account key file
    val currentDir = System.getProperty("user.dir")
    val credentialsPath = "$currentDir/firebase-credentials.json"

    // Check if the credentials file exists
    if (!File(credentialsPath).exists()) {
        println("Error: Firebase credentials file not found at $credentialsPath")
        println("Please place your firebase-credentials.json file in the project directory.")
        return
    }

    try {
        // Initialize Firebase with the path to credentials
        PrintShopInitializer.initialize(credentialsPath)

        // Create and show the application window
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Print Shop Manager",
                state = rememberWindowState(width = 1280.dp, height = 800.dp)
            ) {
                // Get the ViewModel from the initializer
                val viewModel = PrintShopInitializer.getViewModel()

                // Render the main app UI
                PrintQueueScreen(viewModel)
            }
        }
    } catch (e: Exception) {
        println("Error starting application: ${e.message}")
        e.printStackTrace()
    }
}