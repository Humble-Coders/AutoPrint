package org.example.autoprint.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.autoprint.models.PrinterSettings

@Composable
fun SettingsScreen(
    printerSettings: PrinterSettings,
    availablePrinters: List<String>,
    onSettingsChange: (PrinterSettings) -> Unit,
    onRefreshPrinters: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "⚙️ Printer Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A237E)
                )
            }

            OutlinedButton(onClick = onRefreshPrinters) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Refresh Printers")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Available Printers Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🖨️ Available Printers (${availablePrinters.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A237E)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (availablePrinters.isEmpty()) {
                    Text(
                        text = "No printers found. Please check printer connections.",
                        color = Color(0xFFF44336),
                        fontSize = 14.sp
                    )
                } else {
                    availablePrinters.forEach { printer ->
                        Text(
                            text = "• $printer",
                            fontSize = 14.sp,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Printer Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎯 Printer Configuration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A237E),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Color Printer Dropdown
                PrinterDropdownSection(
                    title = "🌈 Color Printer",
                    description = "Printer for COLOR mode orders",
                    selectedPrinter = printerSettings.colorPrinter,
                    availablePrinters = availablePrinters,
                    onPrinterSelected = { printer ->
                        onSettingsChange(printerSettings.copy(colorPrinter = printer))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Black & White Printer Dropdown
                PrinterDropdownSection(
                    title = "⚫ Black & White Printer",
                    description = "Printer for MONOCHROME/BW mode orders",
                    selectedPrinter = printerSettings.blackWhitePrinter,
                    availablePrinters = availablePrinters,
                    onPrinterSelected = { printer ->
                        onSettingsChange(printerSettings.copy(blackWhitePrinter = printer))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Both/Fallback Printer Dropdown
                PrinterDropdownSection(
                    title = "🔄 Fallback Printer",
                    description = "Default printer when specific printer not available",
                    selectedPrinter = printerSettings.bothPrinter,
                    availablePrinters = availablePrinters,
                    onPrinterSelected = { printer ->
                        onSettingsChange(printerSettings.copy(bothPrinter = printer))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Current Configuration Summary
        if (printerSettings.isConfigured()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = 4.dp,
                backgroundColor = Color(0xFFE8F5E8)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "✅ Current Configuration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (printerSettings.colorPrinter.isNotEmpty()) {
                        Text("🌈 Color: ${printerSettings.colorPrinter}", fontSize = 14.sp)
                    }
                    if (printerSettings.blackWhitePrinter.isNotEmpty()) {
                        Text("⚫ B&W: ${printerSettings.blackWhitePrinter}", fontSize = 14.sp)
                    }
                    if (printerSettings.bothPrinter.isNotEmpty()) {
                        Text("🔄 Fallback: ${printerSettings.bothPrinter}", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PrinterDropdownSection(
    title: String,
    description: String,
    selectedPrinter: String,
    availablePrinters: List<String>,
    onPrinterSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A237E)
        )
        Text(
            text = description,
            fontSize = 12.sp,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = if (selectedPrinter.isNotEmpty()) Color(0xFFE3F2FD) else Color.White
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedPrinter.ifEmpty { "Select Printer..." },
                        color = if (selectedPrinter.isNotEmpty()) Color(0xFF1976D2) else Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                // Clear selection option
                DropdownMenuItem(
                    onClick = {
                        onPrinterSelected("")
                        expanded = false
                    }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Gray)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear Selection", color = Color.Gray)
                    }
                }

                Divider()

                // Available printers
                availablePrinters.forEach { printer ->
                    DropdownMenuItem(
                        onClick = {
                            onPrinterSelected(printer)
                            expanded = false
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = null,
                                tint = if (printer == selectedPrinter) Color(0xFF4CAF50) else Color.Gray
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = printer,
                                color = if (printer == selectedPrinter) Color(0xFF4CAF50) else Color.Black,
                                fontWeight = if (printer == selectedPrinter) FontWeight.Medium else FontWeight.Normal
                            )
                            if (printer == selectedPrinter) {
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}