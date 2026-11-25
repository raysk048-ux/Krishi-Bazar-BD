package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.ApprovalStatus
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.ProductCategory
import com.example.model.ProductEntity
import com.example.model.WithdrawalStatus
import com.example.ui.components.KrishiImage
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBdt
import com.example.viewmodel.KrishiUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    state: KrishiUiState,
    onSaveShop: (String, String, String, String, String, String) -> Unit,
    onSaveProduct: (ProductEntity?, String, String, ProductCategory, Double, String, Int, String, String, Double?, Boolean) -> Unit,
    onAdjustStock: (String, Int) -> Unit,
    onUpdateOrderStatus: (String, OrderStatus, String) -> Unit,
    onRequestWithdrawal: (Double, PaymentMethod, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    val context = LocalContext.current
    val shop = state.sellerShop
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showShopEditor by remember { mutableStateOf(false) }

    if (shop == null || showShopEditor) {
        ShopRegistrationForm(
            existingShop = shop,
            lang = lang,
            initialPhone = state.currentUser?.phone ?: "",
            initialDistrict = state.currentUser?.district ?: "Bogura",
            onPickPersistUri = { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            },
            onSave = { bn, en, dist, phone, desc, img ->
                onSaveShop(bn, en, dist, phone, desc, img)
                showShopEditor = false
            },
            onCancel = if (shop != null) ({ showShopEditor = false }) else null,
            modifier = modifier
        )
        return
    }

    val tabs = if (lang == AppLanguage.BANGLA) {
        listOf("হিসাব ও ওভারভিউ", "পণ্য ও স্টক", "অর্ডার", "টাকা উত্তোলন")
    } else {
        listOf("Overview", "Products & Stock", "Orders", "Withdrawals")
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showProductDialog = true
                    },
                    modifier = Modifier.testTag("seller_add_product_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = AppStrings.addProduct(lang))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Shop Banner Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KrishiImage(
                        imageUri = shop.imageUri,
                        contentDescription = shop.shopNameBn,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lang == AppLanguage.BANGLA) shop.shopNameBn else shop.shopNameEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(
                                text = if (lang == AppLanguage.BANGLA) shop.approvalStatus.titleBn else shop.approvalStatus.titleEn,
                                containerColor = when (shop.approvalStatus) {
                                    ApprovalStatus.APPROVED -> MaterialTheme.colorScheme.primary
                                    ApprovalStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                                    ApprovalStatus.REJECTED -> MaterialTheme.colorScheme.error
                                },
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(text = shop.district, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    IconButton(onClick = { showShopEditor = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Shop")
                    }
                }
            }

            PrimaryScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }

            when (selectedTab) {
                0 -> SellerFinanceOverview(state, lang)
                1 -> SellerProductsTab(
                    state = state,
                    lang = lang,
                    onEdit = {
                        editingProduct = it
                        showProductDialog = true
                    },
                    onAdjustStock = onAdjustStock,
                    onOpenAdd = {
                        editingProduct = null
                        showProductDialog = true
                    }
                )
                2 -> SellerOrdersTab(state, lang, onUpdateOrderStatus)
                3 -> SellerWithdrawalsTab(state, lang, onRequestWithdrawal)
            }
        }
    }

    if (showProductDialog) {
        ProductEditorDialog(
            existing = editingProduct,
            state = state,
            lang = lang,
            onDismiss = { showProductDialog = false },
            onSave = { bn, en, cat, price, unit, stock, desc, img, comm, consent ->
                onSaveProduct(editingProduct, bn, en, cat, price, unit, stock, desc, img, comm, consent)
                showProductDialog = false
            }
        )
    }
}

@Composable
private fun ShopRegistrationForm(
    existingShop: com.example.model.ShopEntity?,
    lang: AppLanguage,
    initialPhone: String,
    initialDistrict: String,
    onPickPersistUri: (android.net.Uri) -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var nameBn by rememberSaveable { mutableStateOf(existingShop?.shopNameBn ?: "") }
    var nameEn by rememberSaveable { mutableStateOf(existingShop?.shopNameEn ?: "") }
    var district by rememberSaveable { mutableStateOf(existingShop?.district ?: initialDistrict) }
    var phone by rememberSaveable { mutableStateOf(existingShop?.phone ?: initialPhone) }
    var desc by rememberSaveable { mutableStateOf(existingShop?.description ?: "") }
    var imageUri by rememberSaveable { mutableStateOf(existingShop?.imageUri ?: "drawable://img_cat_vegetables") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            onPickPersistUri(uri)
            imageUri = uri.toString()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = AppStrings.registerShopTitle(lang),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KrishiImage(
                imageUri = imageUri,
                contentDescription = "Shop Photo",
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp))
            )
            OutlinedButton(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            ) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (lang == AppLanguage.BANGLA) "দোকানের ছবি আপলোড" else "Upload Shop Photo")
            }
        }
        OutlinedTextField(
            value = nameBn,
            onValueChange = { nameBn = it },
            label = { Text(if (lang == AppLanguage.BANGLA) "দোকানের নাম (বাংলা)" else "Shop Name (Bangla)") },
            modifier = Modifier.fillMaxWidth().testTag("shop_name_bn_input")
        )
        OutlinedTextField(
            value = nameEn,
            onValueChange = { nameEn = it },
            label = { Text(if (lang == AppLanguage.BANGLA) "দোকানের নাম (English)" else "Shop Name (English)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = district,
            onValueChange = { district = it },
            label = { Text(if (lang == AppLanguage.BANGLA) "জেলা" else "District") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(if (lang == AppLanguage.BANGLA) "যোগাযোগ নম্বর" else "Contact Phone") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text(if (lang == AppLanguage.BANGLA) "দোকানের বিবরণ" else "Shop Description") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onCancel != null) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text(if (lang == AppLanguage.BANGLA) "বাতিল" else "Cancel")
                }
            }
            Button(
                onClick = { onSave(nameBn, nameEn, district, phone, desc, imageUri) },
                modifier = Modifier.weight(1f).height(50.dp).testTag("save_shop_button")
            ) {
                Text(if (lang == AppLanguage.BANGLA) "দোকান সংরক্ষণ করুন" else "Save Shop")
            }
        }
    }
}

@Composable
private fun SellerFinanceOverview(state: KrishiUiState, lang: AppLanguage) {
    val fin = state.sellerFinance
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(
                        text = if (lang == AppLanguage.BANGLA) "উত্তোলনযোগ্য নিট আয়" else "Available Net Balance",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatBdt(fin.availableBalance),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (lang == AppLanguage.BANGLA) {
                            "প্ল্যাটফর্ম ডিফল্ট কমিশন: ${state.settings.defaultCommissionPercent.toInt()}% (শুধু সম্পন্ন অর্ডারে)"
                        } else {
                            "Platform Default Commission: ${state.settings.defaultCommissionPercent.toInt()}% (Completed orders only)"
                        },
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricMiniCard(
                    title = if (lang == AppLanguage.BANGLA) "মোট সম্পন্ন বিক্রি" else "Completed Sales",
                    value = formatBdt(fin.completedGrossSales),
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    title = if (lang == AppLanguage.BANGLA) "কর্তিত কমিশন" else "Commission Deducted",
                    value = formatBdt(fin.earnedCommissionDeducted),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricMiniCard(
                    title = if (lang == AppLanguage.BANGLA) "চলমান অর্ডার মূল্য" else "Pending Orders",
                    value = formatBdt(fin.pendingOrderValue),
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    title = if (lang == AppLanguage.BANGLA) "মোট উত্তোলিত" else "Withdrawn",
                    value = formatBdt(fin.approvedWithdrawn),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricMiniCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SellerProductsTab(
    state: KrishiUiState,
    lang: AppLanguage,
    onEdit: (ProductEntity) -> Unit,
    onAdjustStock: (String, Int) -> Unit,
    onOpenAdd: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedButton(onClick = onOpenAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.addProduct(lang))
            }
        }
        items(state.sellerProducts, key = { it.product.id }) { pws ->
            val p = pws.product
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KrishiImage(
                        imageUri = p.imageUri,
                        contentDescription = p.nameBn,
                        modifier = Modifier.size(62.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lang == AppLanguage.BANGLA) p.nameBn else p.nameEn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${formatBdt(p.priceBdt)} • ${if (lang == AppLanguage.BANGLA) "কমিশন" else "Comm"}: ${pws.effectiveCommissionPercent.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusPill(
                            text = if (lang == AppLanguage.BANGLA) p.approvalStatus.titleBn else p.approvalStatus.titleEn,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        IconButton(onClick = { onEdit(p) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onAdjustStock(p.id, p.stockQuantity - 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease Stock", modifier = Modifier.size(16.dp))
                            }
                            Text(text = "${p.stockQuantity}", fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { onAdjustStock(p.id, p.stockQuantity + 1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase Stock", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerOrdersTab(
    state: KrishiUiState,
    lang: AppLanguage,
    onUpdateOrderStatus: (String, OrderStatus, String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.sellerOrders.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(AppStrings.emptyOrders(lang))
                }
            }
        }
        items(state.sellerOrders, key = { it.order.id }) { detailed ->
            val order = detailed.order
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(order.id, fontWeight = FontWeight.Bold)
                        StatusPill(
                            text = if (lang == AppLanguage.BANGLA) order.orderStatus.titleBn else order.orderStatus.titleEn,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text("${order.buyerName} • ${order.buyerPhone}", style = MaterialTheme.typography.bodySmall)
                    Text(order.deliveryAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${if (lang == AppLanguage.BANGLA) "মোট" else "Subtotal"}: ${formatBdt(order.grossSubtotal)} • ${if (lang == AppLanguage.BANGLA) "সম্মত কমিশন" else "Agreed Comm"}: ${formatBdt(order.agreedCommissionTotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    val nextAction = when (order.orderStatus) {
                        OrderStatus.PLACED -> OrderStatus.CONFIRMED to (if (lang == AppLanguage.BANGLA) "অর্ডার নিশ্চিত করুন" else "Confirm Order")
                        OrderStatus.CONFIRMED -> OrderStatus.PACKED to (if (lang == AppLanguage.BANGLA) "প্যাকেটজাত সম্পন্ন" else "Mark Packed")
                        OrderStatus.PACKED -> OrderStatus.SHIPPED to (if (lang == AppLanguage.BANGLA) "ডেলিভারিতে পাঠান" else "Mark Shipped")
                        OrderStatus.SHIPPED -> OrderStatus.DELIVERED to (if (lang == AppLanguage.BANGLA) "ডেলিভারি সম্পন্ন দিন" else "Mark Delivered")
                        else -> null
                    }
                    if (nextAction != null) {
                        Button(
                            onClick = { onUpdateOrderStatus(order.id, nextAction.first, nextAction.second) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(nextAction.second)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SellerWithdrawalsTab(
    state: KrishiUiState,
    lang: AppLanguage,
    onRequestWithdrawal: (Double, PaymentMethod, String) -> Unit
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var accountPhone by rememberSaveable { mutableStateOf(state.currentUser?.phone ?: "") }
    var method by rememberSaveable { mutableStateOf(PaymentMethod.BKASH) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(AppStrings.requestWithdrawal(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "${if (lang == AppLanguage.BANGLA) "উত্তোলনযোগ্য ব্যালেন্স" else "Available"}: ${formatBdt(state.sellerFinance.availableBalance)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(if (lang == AppLanguage.BANGLA) "টাকার পরিমাণ (৳)" else "Amount (BDT)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(PaymentMethod.BKASH, PaymentMethod.NAGAD, PaymentMethod.ROCKET).forEach { pm ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = method == pm, onClick = { method = pm })
                                Text(pm.titleEn, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = accountPhone,
                        onValueChange = { accountPhone = it },
                        label = { Text(if (lang == AppLanguage.BANGLA) "মোবাইল অ্যাকাউন্ট নম্বর" else "Payout Account Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            onRequestWithdrawal(amt, method, accountPhone)
                            amountText = ""
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(AppStrings.requestWithdrawal(lang))
                    }
                }
            }
        }

        items(state.sellerWithdrawals, key = { it.id }) { wd ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${wd.id} • ${formatBdt(wd.amountBdt)}", fontWeight = FontWeight.Bold)
                        Text("${wd.payoutMethod.titleEn} (${wd.payoutAccount})", style = MaterialTheme.typography.bodySmall)
                    }
                    StatusPill(
                        text = if (lang == AppLanguage.BANGLA) wd.status.titleBn else wd.status.titleEn,
                        containerColor = when (wd.status) {
                            WithdrawalStatus.APPROVED -> MaterialTheme.colorScheme.primaryContainer
                            WithdrawalStatus.PENDING -> MaterialTheme.colorScheme.tertiaryContainer
                            WithdrawalStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
                        },
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorDialog(
    existing: ProductEntity?,
    state: KrishiUiState,
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (String, String, ProductCategory, Double, String, Int, String, String, Double?, Boolean) -> Unit
) {
    val context = LocalContext.current
    var nameBn by remember { mutableStateOf(existing?.nameBn ?: "") }
    var nameEn by remember { mutableStateOf(existing?.nameEn ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: ProductCategory.VEGETABLES) }
    var catExpanded by remember { mutableStateOf(false) }
    var priceText by remember { mutableStateOf(existing?.priceBdt?.toInt()?.toString() ?: "") }
    var unitLabel by remember { mutableStateOf(existing?.unitLabel ?: "১ কেজি / 1 kg") }
    var stockText by remember { mutableStateOf(existing?.stockQuantity?.toString() ?: "20") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var imageUri by remember { mutableStateOf(existing?.imageUri ?: category.defaultDrawableKey) }
    var commissionSlider by remember {
        mutableFloatStateOf(
            (existing?.commissionPercentOverride ?: state.settings.defaultCommissionPercent).toFloat()
        )
    }
    var consentChecked by remember { mutableStateOf(existing?.sellerCommissionAccepted ?: true) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            imageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) AppStrings.addProduct(lang) else (if (lang == AppLanguage.BANGLA) "পণ্য সম্পাদনা" else "Edit Product")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KrishiImage(
                        imageUri = imageUri,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                    )
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (lang == AppLanguage.BANGLA) "ছবি বাছুন" else "Pick Photo")
                    }
                }
                OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text(if (lang == AppLanguage.BANGLA) "পণ্যের নাম (বাংলা)" else "Name (Bangla)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text("Name (English)") }, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                    OutlinedTextField(
                        value = if (lang == AppLanguage.BANGLA) category.titleBn else category.titleEn,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (lang == AppLanguage.BANGLA) "ক্যাটাগরি" else "Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        ProductCategory.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(if (lang == AppLanguage.BANGLA) cat.titleBn else cat.titleEn) },
                                onClick = {
                                    category = cat
                                    if (imageUri.startsWith("drawable://")) imageUri = cat.defaultDrawableKey
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text(if (lang == AppLanguage.BANGLA) "দাম (৳)" else "Price (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text(if (lang == AppLanguage.BANGLA) "স্টক" else "Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(value = unitLabel, onValueChange = { unitLabel = it }, label = { Text(if (lang == AppLanguage.BANGLA) "একক (যেমন: ১ কেজি)" else "Unit (e.g. 1 kg)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(if (lang == AppLanguage.BANGLA) "বিবরণ" else "Description") }, modifier = Modifier.fillMaxWidth())

                HorizontalDivider()
                Text(
                    text = "${if (lang == AppLanguage.BANGLA) "কমিশন হার" else "Commission Rate"}: ${commissionSlider.roundToInt()}% (${state.settings.minCommissionPercent.toInt()}% - ${state.settings.maxCommissionPercent.toInt()}%)",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = commissionSlider,
                    onValueChange = { commissionSlider = it },
                    valueRange = state.settings.minCommissionPercent.toFloat()..state.settings.maxCommissionPercent.toFloat()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consentChecked, onCheckedChange = { consentChecked = it })
                    Text(
                        text = AppStrings.sellerConsentText(lang, commissionSlider.roundToInt().toDouble()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    nameBn,
                    nameEn,
                    category,
                    priceText.toDoubleOrNull() ?: 0.0,
                    unitLabel,
                    stockText.toIntOrNull() ?: 0,
                    desc,
                    imageUri,
                    commissionSlider.roundToInt().toDouble(),
                    consentChecked
                )
            }) {
                Text(if (lang == AppLanguage.BANGLA) "সংরক্ষণ" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (lang == AppLanguage.BANGLA) "বাতিল" else "Cancel")
            }
        }
    )
}
