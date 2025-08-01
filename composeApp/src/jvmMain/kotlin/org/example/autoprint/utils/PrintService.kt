package org.example.autoprint.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.printing.PDFPageable
import org.apache.pdfbox.pdmodel.PDDocument
import org.example.autoprint.models.PrintOrder
import org.example.autoprint.models.PrintSettings
import java.awt.print.*
import java.io.File
import java.util.logging.Logger
import javax.print.*
import javax.print.PrintService
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.*

class PrintService {
    private val logger = Logger.getLogger("PrintService")

    suspend fun printDocument(
        file: File,
        order: PrintOrder,
        onProgress: (String) -> Unit = {},
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        try {
            logger.info("🖨️ Starting print job for: ${file.name}")
            onProgress("Preparing document for printing...")

            when (file.extension.lowercase()) {
                "pdf" -> printPDF(file, order, onProgress, onComplete)
                else -> {
                    logger.warning("❌ Unsupported file format: ${file.extension}")
                    onComplete(false, "Unsupported file format: ${file.extension}")
                }
            }
        } catch (e: Exception) {
            logger.severe("❌ Print service error: ${e.message}")
            onComplete(false, "Print error: ${e.message}")
        }
    }

    private suspend fun printPDF(
        file: File,
        order: PrintOrder,
        onProgress: (String) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            logger.info("📄 Loading PDF document: ${file.name}")
            onProgress("Loading PDF document...")
            document = org.apache.pdfbox.Loader.loadPDF(file)

            val totalPages = document.numberOfPages
            logger.info("📄 Document has $totalPages pages")
            logger.info("🔧 Print settings: ${order.printSettings}")

            onProgress("Setting up printer...")
            val printService = findPrintService()
            if (printService == null) {
                logger.warning("❌ No printer found")
                onComplete(false, "No printer found")
                return@withContext
            }

            logger.info("🖨️ Using printer: ${printService.name}")

            // Validate page range
            if (order.printSettings.pagesToPrint == "CUSTOM") {
                val validPages = parsePageRange(order.printSettings.customPages, totalPages)
                if (validPages.isEmpty()) {
                    logger.warning("❌ No valid pages to print from range: ${order.printSettings.customPages}")
                    onComplete(false, "No valid pages to print from range: ${order.printSettings.customPages}")
                    return@withContext
                }
                logger.info("✅ Valid pages to print: $validPages")
            }

            onProgress("Configuring print settings...")
            val attributes = createPrintAttributes(order.printSettings)
            val job = printService.createPrintJob()

            onProgress("Creating print job...")
            val pageable = PDFPageable(document)
            val printable = createPrintable(pageable, order.printSettings)

            val doc = SimpleDoc(printable, DocFlavor.SERVICE_FORMATTED.PRINTABLE, null)

            onProgress("Sending to printer...")
            logger.info("📤 Submitting print job to printer")
            job.print(doc, attributes)

            onProgress("Print job submitted successfully")
            logger.info("✅ Print job completed successfully for: ${file.name}")
            onComplete(true, "Document printed successfully")

        } catch (e: Exception) {
            logger.severe("❌ PDF print error for ${file.name}: ${e.message}")
            e.printStackTrace()
            onComplete(false, "PDF print error: ${e.message}")
        } finally {
            document?.close()
            logger.info("🔒 PDF document closed: ${file.name}")
        }
    }

    private fun findPrintService(): PrintService? {
        val printServices = PrintServiceLookup.lookupPrintServices(
            DocFlavor.SERVICE_FORMATTED.PRINTABLE, null
        )

        logger.info("🔍 Looking for available print services...")
        logger.info("📊 Found ${printServices.size} print services")

        // Try to find default printer first
        val defaultService = PrintServiceLookup.lookupDefaultPrintService()
        if (defaultService != null) {
            logger.info("✅ Using default printer: ${defaultService.name}")
            return defaultService
        }

        // Otherwise return first available printer
        val firstService = printServices.firstOrNull()
        if (firstService != null) {
            logger.info("✅ Using first available printer: ${firstService.name}")
        } else {
            logger.warning("❌ No printers available")
        }
        return firstService
    }

    private fun createPrintAttributes(settings: PrintSettings): HashPrintRequestAttributeSet {
        val attributes = HashPrintRequestAttributeSet()
        logger.info("🔧 Creating print attributes from settings: $settings")

        // Number of copies
        attributes.add(Copies(settings.copies))
        logger.info("📋 Copies set to: ${settings.copies}")

        // Paper size
        val mediaSize = when (settings.paperSize.uppercase()) {
            "A4" -> MediaSizeName.ISO_A4
            "A3" -> MediaSizeName.ISO_A3
            "LETTER" -> MediaSizeName.NA_LETTER
            "LEGAL" -> MediaSizeName.NA_LEGAL
            else -> {
                logger.warning("⚠️ Unknown paper size: ${settings.paperSize}, defaulting to A4")
                MediaSizeName.ISO_A4
            }
        }
        attributes.add(mediaSize)
        logger.info("📄 Paper size set to: ${mediaSize} (from ${settings.paperSize})")

        // Orientation
        val orientation = when (settings.orientation.uppercase()) {
            "LANDSCAPE" -> OrientationRequested.LANDSCAPE
            "PORTRAIT" -> OrientationRequested.PORTRAIT
            else -> {
                logger.warning("⚠️ Unknown orientation: ${settings.orientation}, defaulting to PORTRAIT")
                OrientationRequested.PORTRAIT
            }
        }
        attributes.add(orientation)
        logger.info("🔄 Orientation set to: ${orientation} (from ${settings.orientation})")

        // Color mode
        val colorMode = when (settings.colorMode.uppercase()) {
            "COLOR" -> Chromaticity.COLOR
            "MONOCHROME" -> Chromaticity.MONOCHROME
            "BW", "BLACK_WHITE", "BLACKWHITE" -> Chromaticity.MONOCHROME
            "GRAYSCALE", "GREY" -> Chromaticity.MONOCHROME
            else -> {
                logger.warning("⚠️ Unknown color mode: ${settings.colorMode}, defaulting to COLOR")
                Chromaticity.COLOR
            }
        }
        attributes.add(colorMode)
        logger.info("🎨 Color mode set to: ${colorMode} (from ${settings.colorMode})")

        // Print quality
        val quality = when (settings.quality.uppercase()) {
            "HIGH" -> PrintQuality.HIGH
            "NORMAL" -> PrintQuality.NORMAL
            "DRAFT" -> PrintQuality.DRAFT
            else -> {
                logger.warning("⚠️ Unknown quality: ${settings.quality}, defaulting to NORMAL")
                PrintQuality.NORMAL
            }
        }
        attributes.add(quality)
        logger.info("⭐ Print quality set to: ${quality} (from ${settings.quality})")

        logger.info("✅ Print attributes configured successfully")
        return attributes
    }

    private fun createPrintable(pageable: PDFPageable, settings: PrintSettings): Printable {
        return Printable { graphics, pageFormat, pageIndex ->
            if (shouldPrintPage(pageIndex, settings, pageable.numberOfPages)) {
                pageable.getPrintable(pageIndex).print(graphics, pageFormat, pageIndex)
            } else {
                Printable.NO_SUCH_PAGE
            }
        }
    }

    private fun shouldPrintPage(pageIndex: Int, settings: PrintSettings, totalPages: Int): Boolean {
        return when (settings.pagesToPrint) {
            "ALL" -> pageIndex < totalPages
            "CUSTOM" -> {
                if (settings.customPages.isBlank()) return pageIndex < totalPages
                val validPages = parsePageRange(settings.customPages, totalPages)
                validPages.contains(pageIndex + 1)
            }
            else -> pageIndex < totalPages
        }
    }

    private fun parsePageRange(pageRange: String, totalPages: Int): Set<Int> {
        val pages = mutableSetOf<Int>()
        try {
            logger.info("🔍 Parsing page range: '$pageRange' for document with $totalPages pages")
            pageRange.split(",").forEach { part ->
                val trimmed = part.trim()
                when {
                    trimmed.contains("-") -> {
                        val range = trimmed.split("-")
                        if (range.size == 2) {
                            val start = range[0].toIntOrNull() ?: 1
                            val end = range[1].toIntOrNull() ?: start
                            // Clamp to valid page range
                            val validStart = maxOf(1, start)
                            val validEnd = minOf(totalPages, end)
                            if (validStart <= validEnd) {
                                (validStart..validEnd).forEach { pages.add(it) }
                                logger.info("📄 Added page range: $validStart-$validEnd")
                            } else {
                                logger.warning("⚠️ Invalid range: $start-$end (document only has $totalPages pages)")
                            }
                        }
                    }
                    else -> {
                        val pageNum = trimmed.toIntOrNull()
                        if (pageNum != null && pageNum in 1..totalPages) {
                            pages.add(pageNum)
                            logger.info("📄 Added single page: $pageNum")
                        } else {
                            logger.warning("⚠️ Invalid page number: $trimmed (document only has $totalPages pages)")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.severe("❌ Error parsing page range '$pageRange': ${e.message}")
            // If parsing fails, print all pages
            (1..totalPages).forEach { pages.add(it) }
            logger.info("🔧 Fallback: printing all pages (1-$totalPages)")
        }

        logger.info("✅ Final page set to print: $pages")
        return pages
    }

    fun getAvailablePrinters(): List<String> {
        return try {
            val printers = PrintServiceLookup.lookupPrintServices(null, null).map { it.name }
            logger.info("🖨️ Found ${printers.size} available printers: $printers")
            printers
        } catch (e: Exception) {
            logger.severe("❌ Error getting available printers: ${e.message}")
            emptyList()
        }
    }

    fun getDefaultPrinter(): String? {
        return try {
            val defaultPrinter = PrintServiceLookup.lookupDefaultPrintService()?.name
            logger.info("🖨️ Default printer: $defaultPrinter")
            defaultPrinter
        } catch (e: Exception) {
            logger.severe("❌ Error getting default printer: ${e.message}")
            null
        }
    }
}