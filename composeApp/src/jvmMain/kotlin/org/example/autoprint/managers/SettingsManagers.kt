package org.example.autoprint.managers

import kotlinx.serialization.json.Json
import org.example.autoprint.models.PrinterSettings
import java.io.File
import java.util.logging.Logger

class SettingsManager {
    private val logger = Logger.getLogger("SettingsManager")
    private val settingsFile = File("printer-settings.json")
    private val json = Json { prettyPrint = true }

    fun savePrinterSettings(settings: PrinterSettings) {
        try {
            val jsonString = json.encodeToString(PrinterSettings.serializer(), settings)
            settingsFile.writeText(jsonString)
            logger.info("✅ Printer settings saved successfully")
        } catch (e: Exception) {
            logger.severe("❌ Failed to save printer settings: ${e.message}")
        }
    }

    fun loadPrinterSettings(): PrinterSettings {
        return try {
            if (settingsFile.exists()) {
                val jsonString = settingsFile.readText()
                val settings = json.decodeFromString(PrinterSettings.serializer(), jsonString)
                logger.info("✅ Printer settings loaded successfully: $settings")
                settings
            } else {
                logger.info("⚠️ Settings file not found, using default settings")
                PrinterSettings()
            }
        } catch (e: Exception) {
            logger.severe("❌ Failed to load printer settings: ${e.message}")
            PrinterSettings()
        }
    }
}