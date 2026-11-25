package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.OrderItemEntity
import com.example.model.OrderStatus
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBdt
import com.example.ui.theme.HarvestGoldTertiary
import com.example.viewmodel.KrishiUiState

@Composable
fun OrdersScreen(
    state: KrishiUiState,
    onUpdateOrderStatus: (String, OrderStatus, String) -> Unit,
    onSubmitReview: (String, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    var reviewTargetItem by remember { mutableStateOf<OrderItemEntity?>(null) }

    if (state.buyerOrders.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(AppStrings.emptyOrders(lang), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = if (lang == AppLanguage.BANGLA) "আমার অর্ডার ও ট্র্যাকিং (${state.buyerOrders.size})" else "My Orders & Tracking (${state.buyerOrders.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(state.buyerOrders, key = { it.order.id }) { detailed ->
            val order = detailed.order
            val statusLabel = if (lang == AppLanguage.BANGLA) order.orderStatus.titleBn else order.orderStatus.titleEn
            val progress = when (order.orderStatus) {
                OrderStatus.PLACED -> 0.16f
                OrderStatus.CONFIRMED -> 0.35f
                OrderStatus.PACKED -> 0.55f
                OrderStatus.SHIPPED -> 0.75f
                OrderStatus.DELIVERED -> 0.90f
                OrderStatus.COMPLETED -> 1.0f
                else -> 0.0f
            }
            val isTerminalNegative = order.orderStatus in listOf(
                OrderStatus.CANCELLED,
                OrderStatus.REFUND_REQUESTED,
                OrderStatus.REFUNDED
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("order_card_${order.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = order.id, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = detailed.shop?.let { if (lang == AppLanguage.BANGLA) it.shopNameBn else it.shopNameEn } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            text = statusLabel,
                            containerColor = if (isTerminalNegative) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isTerminalNegative) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (!isTerminalNegative) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(7.dp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = order.trackingNote.ifBlank { statusLabel },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    detailed.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (lang == AppLanguage.BANGLA) item.productNameBn else item.productNameEn} × ${item.quantity}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(text = formatBdt(item.lineSubtotal), fontWeight = FontWeight.SemiBold)
                            if (order.orderStatus == OrderStatus.COMPLETED || order.orderStatus == OrderStatus.DELIVERED) {
                                IconButton(
                                    onClick = { reviewTargetItem = item },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RateReview,
                                        contentDescription = AppStrings.writeReview(lang),
                                        tint = HarvestGoldTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${if (lang == AppLanguage.BANGLA) order.paymentMethod.titleBn else order.paymentMethod.titleEn} (${if (lang == AppLanguage.BANGLA) order.paymentStatus.titleBn else order.paymentStatus.titleEn})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${AppStrings.totalPayable(lang)}: ${formatBdt(order.totalPayable)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Buyer Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (order.orderStatus == OrderStatus.DELIVERED) {
                            Button(
                                onClick = {
                                    onUpdateOrderStatus(
                                        order.id,
                                        OrderStatus.COMPLETED,
                                        if (lang == AppLanguage.BANGLA) "ক্রেতা ডেলিভারি নিশ্চিত করেছেন" else "Delivery confirmed by buyer"
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("confirm_delivery_${order.id}")
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(AppStrings.confirmDelivery(lang))
                            }
                        }
                        if (order.orderStatus in listOf(OrderStatus.PLACED, OrderStatus.CONFIRMED)) {
                            OutlinedButton(
                                onClick = {
                                    onUpdateOrderStatus(
                                        order.id,
                                        OrderStatus.CANCELLED,
                                        if (lang == AppLanguage.BANGLA) "ক্রেতা কর্তৃক বাতিল" else "Cancelled by buyer"
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(AppStrings.cancelOrder(lang))
                            }
                        }
                        if (order.orderStatus in listOf(OrderStatus.DELIVERED, OrderStatus.COMPLETED)) {
                            OutlinedButton(
                                onClick = {
                                    onUpdateOrderStatus(
                                        order.id,
                                        OrderStatus.REFUND_REQUESTED,
                                        if (lang == AppLanguage.BANGLA) "ক্রেতা রিফান্ড আবেদন করেছেন" else "Refund requested by buyer"
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(AppStrings.requestRefund(lang))
                            }
                        }
                    }
                }
            }
        }
    }

    reviewTargetItem?.let { item ->
        var rating by remember { mutableIntStateOf(5) }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reviewTargetItem = null },
            title = { Text(AppStrings.writeReview(lang)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (lang == AppLanguage.BANGLA) item.productNameBn else item.productNameEn, fontWeight = FontWeight.SemiBold)
                    Row {
                        (1..5).forEach { star ->
                            IconButton(onClick = { rating = star }) {
                                Icon(
                                    imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "$star stars",
                                    tint = if (star <= rating) HarvestGoldTertiary else Color.Gray
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text(if (lang == AppLanguage.BANGLA) "আপনার মতামত লিখুন" else "Write your review") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSubmitReview(item.productId, rating, comment)
                    reviewTargetItem = null
                }) {
                    Text(if (lang == AppLanguage.BANGLA) "জমা দিন" else "Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewTargetItem = null }) {
                    Text(if (lang == AppLanguage.BANGLA) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
