package org.example.autoprint.utils

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.cloud.firestore.Firestore
import org.example.autoprint.repositories.FirestoreRepository
import org.example.autoprint.utils.DocumentDownloader
import org.example.autoprint.viewModels.PrintQueueViewModel
import java.io.FileInputStream
import java.nio.file.Path
import java.sql.DriverManager.println

object PrintShopInitializer {
    private var initialized = false
    private lateinit var firebaseApp: FirebaseApp
    private lateinit var firestore: Firestore

    private var repository: FirestoreRepository? = null
    private var viewModel: PrintQueueViewModel? = null
    private var downloader: DocumentDownloader? = null

    fun initialize(credentialsPath: String) {
        if (initialized) return

        try {
            val credentialsFile = Path.of(credentialsPath).toFile()
            if (!credentialsFile.exists()) {
                throw IllegalArgumentException("Firebase credentials file not found at: $credentialsPath")
            }

            val credentials = GoogleCredentials.fromStream(FileInputStream(credentialsFile))
            val projectId = extractProjectId(credentialsFile.readText())

            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build()

            firebaseApp = FirebaseApp.initializeApp(options)
            firestore = FirestoreClient.getFirestore(firebaseApp)

            repository = FirestoreRepository(firestore)
            downloader = DocumentDownloader()
            viewModel = PrintQueueViewModel(repository!!, downloader!!)

            initialized = true
            println("Firebase initialized successfully with project ID: $projectId")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize Firebase: ${e.message}", e)
        }
    }

    private fun extractProjectId(jsonContent: String): String {
        val projectIdPattern = "\"project_id\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val matchResult = projectIdPattern.find(jsonContent)
        return matchResult?.groupValues?.get(1)
            ?: throw IllegalArgumentException("Could not extract project_id from credentials file")
    }

    fun getViewModel(): PrintQueueViewModel {
        checkInitialized()
        return viewModel!!
    }

    fun getFirestore(): Firestore {
        checkInitialized()
        return firestore
    }

    private fun checkInitialized() {
        if (!initialized) {
            throw IllegalStateException("PrintShopInitializer not initialized. Call initialize() first.")
        }
    }
}