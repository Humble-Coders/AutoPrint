package org.example.autoprint.models

import kotlinx.serialization.Serializable

@Serializable
data class PrinterSettings(
    val colorPrinter: String = "",
    val blackWhitePrinter: String = "",
    val bothPrinter: String = "" // For orders that can use either
) {
    fun getPrinterForColorMode(colorMode: String): String {
        return when (colorMode.uppercase()) {
            "COLOR" -> colorPrinter.ifEmpty { bothPrinter }
            "MONOCHROME", "BW", "BLACK_WHITE", "BLACKWHITE", "GRAYSCALE", "GREY" ->
                blackWhitePrinter.ifEmpty { bothPrinter }
            else -> bothPrinter
        }
    }

    fun isConfigured(): Boolean {
        return colorPrinter.isNotEmpty() || blackWhitePrinter.isNotEmpty() || bothPrinter.isNotEmpty()
    }
}