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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.DeliveryMethod
import com.example.model.PaymentMethod
import com.example.ui.components.KrishiImage
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBdt
import com.example.ui.theme.BkashPink
import com.example.ui.theme.NagadOrange
import com.example.ui.theme.RocketPurple
import com.example.viewmodel.KrishiUiState

@Composable
fun CartCheckoutScreen(
    state: KrishiUiState,
    onUpdateQty: (Long, Int, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onPlaceOrder: (DeliveryMethod, PaymentMethod, String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    var address by rememberSaveable(state.currentUser?.id) {
        mutableStateOf(state.currentUser?.defaultAddress ?: "")
    }
    var district by rememberSaveable(state.currentUser?.id) {
        mutableStateOf(state.currentUser?.district ?: "Dhaka")
    }
    var deliveryMethod by rememberSaveable { mutableStateOf(DeliveryMethod.SELLER_DELIVERY) }
    var paymentMethod by rememberSaveable { mutableStateOf(PaymentMethod.BKASH) }
    var trxId by rememberSaveable { mutableStateOf("") }
    var senderPhone by rememberSaveable(state.currentUser?.id) {
        mutableStateOf(state.currentUser?.phone ?: "")
    }

    if (state.cartItems.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCartCheckout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = AppStrings.emptyCart(lang),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val shopCount = state.cartItems.map { it.product.shopId }.distinct().size.coerceAtLeast(1)
    val feePerShop = when (deliveryMethod) {
        DeliveryMethod.SELLER_DELIVERY -> state.settings.sellerDeliveryChargeBdt
        DeliveryMethod.COURIER_DELIVERY -> state.settings.courierDeliveryChargeBdt
    }
    val totalDelivery = feePerShop * shopCount
    val grandTotal = state.cartSubtotal + totalDelivery

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = AppStrings.navCart(lang) + " (${state.cartCount})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(state.cartItems, key = { it.cartItem.id }) { item ->
            val title = if (lang == AppLanguage.BANGLA) item.product.nameBn else item.product.nameEn
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KrishiImage(
                        imageUri = item.product.imageUri,
                        contentDescription = title,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Text(
                            text = "${formatBdt(item.product.priceBdt)} • ${item.product.unitLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onUpdateQty(item.cartItem.id, item.cartItem.quantity - 1, item.product.stockQuantity) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "${item.cartItem.quantity}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = { onUpdateQty(item.cartItem.id, item.cartItem.quantity + 1, item.product.stockQuantity) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        IconButton(onClick = { onRemoveItem(item.cartItem.id) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                        Text(
                            text = formatBdt(item.lineSubtotal),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Delivery Address Card
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = AppStrings.deliveryAddress(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text(if (lang == AppLanguage.BANGLA) "জেলা" else "District") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(if (lang == AppLanguage.BANGLA) "বাড়ি, রোড, এলাকা" else "House, Road, Area") },
                        modifier = Modifier.fillMaxWidth().testTag("checkout_address_input")
                    )
                }
            }
        }

        // Delivery Method Card
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = AppStrings.deliveryOption(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    DeliveryMethod.entries.forEach { method ->
                        val fee = if (method == DeliveryMethod.SELLER_DELIVERY) {
                            state.settings.sellerDeliveryChargeBdt
                        } else {
                            state.settings.courierDeliveryChargeBdt
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = deliveryMethod == method,
                                onClick = { deliveryMethod = method }
                            )
                            Text(
                                text = if (lang == AppLanguage.BANGLA) method.titleBn else method.titleEn,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${formatBdt(fee)} / ${if (lang == AppLanguage.BANGLA) "দোকান" else "shop"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Payment Method Card (bKash / Nagad / Rocket / COD)
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = AppStrings.paymentMethod(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    PaymentMethod.entries.forEach { method ->
                        val pillColor = when (method) {
                            PaymentMethod.BKASH -> BkashPink
                            PaymentMethod.NAGAD -> NagadOrange
                            PaymentMethod.ROCKET -> RocketPurple
                            PaymentMethod.COD -> MaterialTheme.colorScheme.primary
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method }
                            )
                            Text(
                                text = if (lang == AppLanguage.BANGLA) method.titleBn else method.titleEn,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            StatusPill(
                                text = if (method.requiresTrxId) "MFS Gateway" else "Cash",
                                containerColor = pillColor.copy(alpha = 0.14f),
                                contentColor = pillColor
                            )
                        }
                    }

                    if (paymentMethod.requiresTrxId) {
                        Text(
                            text = if (lang == AppLanguage.BANGLA) {
                                "মার্চেন্ট নম্বরে (01700-000001) পেমেন্ট করে TrxID দিন — অ্যাডমিন যাচাই করবেন।"
                            } else {
                                "Send payment to Merchant (01700-000001) & enter TrxID for verification."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = senderPhone,
                            onValueChange = { senderPhone = it },
                            label = { Text(AppStrings.senderPhoneLabel(lang)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = trxId,
                            onValueChange = { trxId = it },
                            label = { Text(AppStrings.trxIdLabel(lang)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("checkout_trx_input")
                        )
                    }
                }
            }
        }

        // Order Summary & Submit
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(AppStrings.subtotal(lang))
                        Text(formatBdt(state.cartSubtotal), fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${AppStrings.deliveryCharge(lang)} ($shopCount ${if (lang == AppLanguage.BANGLA) "দোকান" else "shop(s)"})")
                        Text(formatBdt(totalDelivery), fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(AppStrings.totalPayable(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(formatBdt(grandTotal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onPlaceOrder(deliveryMethod, paymentMethod, trxId, senderPhone, address, district) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("place_order_button")
                    ) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(AppStrings.placeOrder(lang), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
