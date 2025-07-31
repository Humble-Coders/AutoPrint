package org.example.autoprint.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import org.example.autoprint.viewModels.PrintQueueViewModel
import java.io.File


@Composable
fun PrintQueueScreen(viewModel: PrintQueueViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🖨️ Print Queue",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A237E)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1A237E),
                elevation = 4.dp
            ) {
                Text(
                    text = "${viewModel.printOrders.size} Orders",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Queue List with indexed orders
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            itemsIndexed(viewModel.printOrders) { index, order ->
                PrintOrderCard(
                    order = order,
                    position = index + 1, // #1 at top
                    downloadStatus = viewModel.downloadStates[order.orderId] ?: DownloadStatus.Idle,
                    downloadedFile = viewModel.downloadedFiles[order.orderId]
                )
            }
        }
    }
}




@Composable
fun PrintOrderCard(
    order: PrintOrder,
    position: Int,
    downloadStatus: DownloadStatus,
    downloadedFile: File?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
    ) {
        // Vertical Queue Number Strip
        Box(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .background(Color(0xFF1976D2), shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
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

            // Header Row
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
                    maxLines = 1,
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
