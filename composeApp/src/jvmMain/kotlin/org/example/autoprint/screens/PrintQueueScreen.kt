package org.example.autoprint.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.autoprint.models.PrintOrder
import org.example.autoprint.models.DownloadStatus
import org.example.autoprint.services.PrintJobStatus
import org.example.autoprint.services.PrintStatus
import org.example.autoprint.viewModels.PrintQueueViewModel
import java.io.File

@Composable
fun PrintQueueScreen(viewModel: PrintQueueViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(16.dp)
    ) {
        // Header with Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🖨️ Print Queue",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A237E)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (viewModel.isPrinting) {
                    Button(
                        onClick = { viewModel.stopPrinting() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("Stop Printing", color = Color.White)
                    }
                } else {
                    Button(
                        onClick = { viewModel.startPrinting() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF1976D2)),
                        enabled = viewModel.printOrders.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("Start Printing", color = Color.White)
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.clearCompletedJobs() }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Printer Info
        PrinterInfoCard(
            defaultPrinter = viewModel.defaultPrinter,
            availablePrinters = viewModel.availablePrinters,
            onRefresh = { viewModel.refreshPrinters() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Queue Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1A237E),
                elevation = 4.dp
            ) {
                Text(
                    text = "${viewModel.printOrders.size} Orders in Queue",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF4CAF50),
                elevation = 4.dp
            ) {
                Text(
                    text = "${viewModel.printedOrders.size} Printed",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }

            if (viewModel.isPrinting) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF9800),
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Printing...", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main content with print queue and printed orders
        Row(modifier = Modifier.fillMaxSize()) {
            // Print Queue (Left side - 70%)
            Column(modifier = Modifier.weight(0.7f)) {
                Text(
                    text = "📋 Print Queue",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A237E),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(viewModel.printOrders) { index, order ->
                        PrintOrderCard(
                            order = order,
                            position = index + 1,
                            downloadStatus = viewModel.downloadStates[order.orderId] ?: DownloadStatus.Idle,
                            downloadedFile = viewModel.downloadedFiles[order.orderId],
                            printStatus = viewModel.printStatuses[order.orderId]
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Printed Orders (Right side - 30%)
            Column(modifier = Modifier.weight(0.3f)) {
                Text(
                    text = "✅ Printed Orders",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(viewModel.printedOrders) { order ->
                        PrintedOrderCard(order)
                    }
                }
            }
        }
    }
}
        @Composable
        fun PrintedOrderCard(order: PrintOrder) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                elevation = 2.dp,
                backgroundColor = Color(0xFFF1F8E9)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = order.documentName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Order: ${order.orderId.take(8)}...",
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )

                    Text(
                        text = "${order.printSettings.copies} copies • ${order.printSettings.paperSize}",
                        fontSize = 10.sp,
                        color = Color(0xFF666666)
                    )

                    Text(
                        text = order.getFormattedDateTime(),
                        fontSize = 9.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
        }

        @Composable
        fun PrinterInfoCard(
            defaultPrinter: String?,
            availablePrinters: List<String>,
            onRefresh: () -> Unit
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🖨️ Printer Status",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Default Printer:", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = defaultPrinter ?: "None found",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (defaultPrinter != null) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }

                        Column {
                            Text("Available Printers:", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = "${availablePrinters.size} found",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        @Composable
        fun PrintOrderCard(
            order: PrintOrder,
            position: Int,
            downloadStatus: DownloadStatus,
            downloadedFile: File?,
            printStatus: PrintJobStatus?
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
            ) {
                // Queue Position
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .background(
                            color = when (printStatus?.status) {
                                PrintStatus.COMPLETED -> Color(0xFF4CAF50)
                                PrintStatus.PRINTING -> Color(0xFFFF9800)
                                PrintStatus.FAILED -> Color(0xFFF44336)
                                PrintStatus.CANCELLED -> Color(0xFF9E9E9E)
                                else -> Color(0xFF1976D2)
                            },
                            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$position",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Order Details
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = order.documentName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF263238),
                            modifier = Modifier.weight(1f)
                        )

                        StatusChip(status = order.orderStatus, paid = order.paid)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Order Info
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            DetailItem("Order ID", order.orderId.take(8) + "...")
                            DetailItem("Pages", "${order.pageCount}")
                            DetailItem("Customer", order.customerId)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            DetailItem("Created", order.getFormattedDateTime())
                            DetailItem("Amount", "₹${order.paymentAmount}")
                            DetailItem("Phone", order.customerPhone)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    PrintSettingsRow(order.printSettings)

                    Spacer(modifier = Modifier.height(12.dp))
                    DownloadStatusRow(downloadStatus, downloadedFile)

                    // Print Status
                    printStatus?.let { status ->
                        Spacer(modifier = Modifier.height(8.dp))
                        PrintStatusRow(status)
                    }
                }
            }
        }

        @Composable
        fun PrintStatusRow(status: PrintJobStatus) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = when (status.status) {
                            PrintStatus.COMPLETED -> Color(0xFFE8F5E8)
                            PrintStatus.PRINTING -> Color(0xFFFFF3E0)
                            PrintStatus.FAILED -> Color(0xFFFFEBEE)
                            PrintStatus.CANCELLED -> Color(0xFFF5F5F5)
                            else -> Color(0xFFE3F2FD)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (icon, color) = when (status.status) {
                    PrintStatus.WAITING -> Icons.Default.HourglassEmpty to Color(0xFF1976D2)
                    PrintStatus.PRINTING -> Icons.Default.Print to Color(0xFFFF9800)
                    PrintStatus.COMPLETED -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
                    PrintStatus.FAILED -> Icons.Default.Error to Color(0xFFF44336)
                    PrintStatus.CANCELLED -> Icons.Default.Cancel to Color(0xFF9E9E9E)
                }

                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))

                Column {
                    Text(
                        text = status.status.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = color
                    )
                    if (status.progress.isNotEmpty()) {
                        Text(
                            text = status.progress,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = status.message,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        @Composable
        fun StatusChip(status: String, paid: Boolean) {
            val (color, label) = when {
                paid -> Color(0xFF4CAF50) to "PAID"
                status == "SUBMITTED" -> Color(0xFF1976D2) to "SUBMITTED"
                else -> Color(0xFFBDBDBD) to status
            }

            Surface(
                color = color,
                shape = RoundedCornerShape(12.dp),
                elevation = 2.dp
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        @Composable
        fun DetailItem(label: String, value: String) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(text = label, fontSize = 11.sp, color = Color.Gray)
                Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF37474F))
            }
        }

        @Composable
        fun PrintSettingsRow(settings: org.example.autoprint.models.PrintSettings) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F3F4), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SettingChip(settings.paperSize, Icons.Default.Description)
                SettingChip("${settings.copies}x", Icons.Default.ContentCopy)
                SettingChip(settings.colorMode, Icons.Default.Palette)
                SettingChip(settings.quality, Icons.Default.HighQuality)
            }
        }

        @Composable
        fun SettingChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = text,
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
        }

        @Composable
        fun DownloadStatusRow(status: DownloadStatus, file: File?) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (status) {
                    is DownloadStatus.Idle -> {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Waiting to download...", fontSize = 12.sp, color = Color.Gray)
                    }

                    is DownloadStatus.Downloading -> {
                        CircularProgressIndicator(
                            progress = status.progress,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Downloading ${(status.progress * 100).toInt()}%", fontSize = 12.sp)
                    }

                    is DownloadStatus.Completed -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Downloaded: ${file?.name}", fontSize = 12.sp, color = Color(0xFF4CAF50))
                    }

                    is DownloadStatus.Error -> {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFF44336))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Error: ${status.message}", fontSize = 12.sp, color = Color(0xFFF44336))
                    }
                }
            }
        }