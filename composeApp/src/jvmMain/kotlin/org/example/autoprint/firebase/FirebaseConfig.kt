package org.example.autoprint.firebase

import com.sun.org.apache.bcel.internal.util.Args.require
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class FirebaseConfig(
    val apiKey: String,
    val authDomain: String,
    val projectId: String,
    val storageBucket: String,
    val messagingSenderId: String,
    val appId: String
)

object FirebaseConfigLoader {
    fun loadConfig(): FirebaseConfig {
        val configFile = File("firebase-config.json")
        require(configFile.exists()) { "firebase-config.json not found in project root" }

        val jsonString = configFile.readText()
        return Json.decodeFromString<FirebaseConfig>(jsonString)
    }
}