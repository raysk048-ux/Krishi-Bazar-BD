package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.ApprovalStatus
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.PlatformSettingsEntity
import com.example.model.ProductEntity
import com.example.model.UserRole
import com.example.model.WithdrawalEntity
import com.example.model.WithdrawalStatus
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.RadioButton
import com.example.ui.components.CommissionAnalyticsChart
import com.example.ui.components.StatusPill
import com.example.ui.components.formatBdt
import com.example.ui.theme.HarvestGoldTertiary
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.viewmodel.KrishiUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    state: KrishiUiState,
    onApproveShop: (String, ApprovalStatus) -> Unit,
    onApproveProduct: (String, ApprovalStatus) -> Unit,
    onUpdateProductCommission: (ProductEntity, Double?) -> Unit,
    onToggleFeatured: (String, Boolean) -> Unit,
    onSetUserBlocked: (String, Boolean) -> Unit,
    onVerifyPayment: (String, PaymentStatus) -> Unit,
    onUpdateOrderStatus: (String, OrderStatus, String) -> Unit,
    onResolveWithdrawal: (WithdrawalEntity, WithdrawalStatus, String) -> Unit,
    onWithdrawAdminCommission: (Double, PaymentMethod, String, String) -> Unit,
    onSaveSettings: (PlatformSettingsEntity) -> Unit,
    onGenerateBackup: () -> Unit,
    onClearBackupPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = if (lang == AppLanguage.BANGLA) {
        listOf("রিপোর্ট ও ইউজার", "অনুমোদন", "অর্ডার ও রিফান্ড", "উত্তোলন", "কমিশন ও সেটিংস")
    } else {
        listOf("Reports & Users", "Approvals", "Orders & Refunds", "Withdrawals", "Commission & Settings")
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (lang == AppLanguage.BANGLA) "প্রধান অ্যাডমিন কন্ট্রোল প্যানেল" else "Main Admin Control Panel",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        PrimaryScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 16.dp) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("admin_tab_$idx")
                )
            }
        }

        when (selectedTab) {
            0 -> AdminReportsAndUsersTab(
                state = state,
                lang = lang,
                onSetUserBlocked = onSetUserBlocked,
                onGoToWithdrawals = { selectedTab = 3 }
            )
            1 -> AdminApprovalsTab(state, lang, onApproveShop, onApproveProduct, onUpdateProductCommission, onToggleFeatured)
            2 -> AdminOrdersAndRefundsTab(state, lang, onVerifyPayment, onUpdateOrderStatus)
            3 -> AdminWithdrawalsTab(state, lang, onWithdrawAdminCommission, onResolveWithdrawal)
            4 -> AdminCommissionSettingsTab(state, lang, onSaveSettings, onGenerateBackup)
        }
    }

    state.lastBackupJson?.let { json ->
        AlertDialog(
            onDismissRequest = onClearBackupPreview,
            title = { Text(if (lang == AppLanguage.BANGLA) "ডাটাবেস ব্যাকআপ স্ন্যাপশট" else "Database Backup Snapshot") },
            text = { Text(json, style = MaterialTheme.typography.bodySmall) },
            confirmButton = {
                TextButton(onClick = onClearBackupPreview) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun AdminReportsAndUsersTab(
    state: KrishiUiState,
    lang: AppLanguage,
    onSetUserBlocked: (String, Boolean) -> Unit,
    onGoToWithdrawals: () -> Unit
) {
    val rep = state.adminReport
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(
                        text = if (lang == AppLanguage.BANGLA) "অ্যাডমিনের উত্তোলনযোগ্য কমিশন ব্যালেন্স" else "Available Admin Commission Balance",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                    Text(
                        text = formatBdt(rep.adminAvailableCommissionBdt),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (lang == AppLanguage.BANGLA) {
                            "মোট অর্জিত: ${formatBdt(rep.totalPlatformCommissionBdt)} | উত্তোলিত: ${formatBdt(rep.adminWithdrawnBdt)} | বাতিল/রিফান্ড সমন্বয়: ${formatBdt(rep.totalAdjustedCommissionBdt)}"
                        } else {
                            "Total Earned: ${formatBdt(rep.totalPlatformCommissionBdt)} | Withdrawn: ${formatBdt(rep.adminWithdrawnBdt)} | Reversed: ${formatBdt(rep.totalAdjustedCommissionBdt)}"
                        },
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onGoToWithdrawals,
                        modifier = Modifier.fillMaxWidth().testTag("admin_goto_withdraw_button")
                    ) {
                        Icon(
                            Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lang == AppLanguage.BANGLA) "কমিশন টাকা উত্তোলন করুন" else "Withdraw Commission Money",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricMiniCard(
                    title = if (lang == AppLanguage.BANGLA) "মোট বিক্রয় (সম্পন্ন)" else "Completed Gross Sales",
                    value = formatBdt(rep.totalGrossSalesBdt),
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    title = if (lang == AppLanguage.BANGLA) "বিক্রেতাদের নিট প্রাপ্য" else "Seller Net Payouts",
                    value = formatBdt(rep.totalSellerPayoutsBdt),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            CommissionAnalyticsChart(breakdown = rep.categorySalesBreakdown, lang = lang)
        }

        item {
            Text(
                text = if (lang == AppLanguage.BANGLA) "সকল ব্যবহারকারী নিয়ন্ত্রণ (${state.allUsers.size})" else "All Users (${state.allUsers.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(state.allUsers, key = { it.id }) { user ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.fullName, fontWeight = FontWeight.Bold)
                        Text("${user.phone} • ${if (lang == AppLanguage.BANGLA) user.role.titleBn else user.role.titleEn}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (user.role != UserRole.MAIN_ADMIN) {
                        OutlinedButton(
                            onClick = { onSetUserBlocked(user.id, !user.isBlocked) }
                        ) {
                            Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (user.isBlocked) {
                                    if (lang == AppLanguage.BANGLA) "আনব্লক" else "Unblock"
                                } else {
                                    if (lang == AppLanguage.BANGLA) "ব্লক" else "Block"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminApprovalsTab(
    state: KrishiUiState,
    lang: AppLanguage,
    onApproveShop: (String, ApprovalStatus) -> Unit,
    onApproveProduct: (String, ApprovalStatus) -> Unit,
    onUpdateProductCommission: (ProductEntity, Double?) -> Unit,
    onToggleFeatured: (String, Boolean) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (lang == AppLanguage.BANGLA) "দোকান অনুমোদন (${state.allShops.size})" else "Seller Shops (${state.allShops.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(state.allShops, key = { it.id }) { shop ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (lang == AppLanguage.BANGLA) shop.shopNameBn else shop.shopNameEn, fontWeight = FontWeight.Bold)
                        Text("${shop.district} • ${shop.phone}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusPill(
                            text = if (lang == AppLanguage.BANGLA) shop.approvalStatus.titleBn else shop.approvalStatus.titleEn,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Row {
                        IconButton(onClick = { onApproveShop(shop.id, ApprovalStatus.APPROVED) }) {
                            Icon(Icons.Filled.Check, contentDescription = "Approve", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onApproveShop(shop.id, ApprovalStatus.REJECTED) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = if (lang == AppLanguage.BANGLA) "পণ্য অনুমোদন ও কমিশন (${state.allProducts.size})" else "Products & Per-Item Commission (${state.allProducts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(state.allProducts, key = { it.product.id }) { pws ->
            val p = pws.product
            var customRateText by remember(p.id, pws.effectiveCommissionPercent) {
                mutableStateOf(pws.effectiveCommissionPercent.toInt().toString())
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (lang == AppLanguage.BANGLA) p.nameBn else p.nameEn, fontWeight = FontWeight.Bold)
                            Text("${formatBdt(p.priceBdt)} • ${if (lang == AppLanguage.BANGLA) p.approvalStatus.titleBn else p.approvalStatus.titleEn}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onToggleFeatured(p.id, !p.featured) }) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "Featured",
                                tint = if (p.featured) HarvestGoldTertiary else MaterialTheme.colorScheme.outline
                            )
                        }
                        IconButton(onClick = { onApproveProduct(p.id, ApprovalStatus.APPROVED) }) {
                            Icon(Icons.Filled.Check, contentDescription = "Approve", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onApproveProduct(p.id, ApprovalStatus.REJECTED) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customRateText,
                            onValueChange = { customRateText = it },
                            label = { Text(if (lang == AppLanguage.BANGLA) "কমিশন %" else "Comm %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = {
                            val r = customRateText.toDoubleOrNull()
                            onUpdateProductCommission(p, r)
                        }) {
                            Text(if (lang == AppLanguage.BANGLA) "কমিশন সেট" else "Set Rate")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminOrdersAndRefundsTab(
    state: KrishiUiState,
    lang: AppLanguage,
    onVerifyPayment: (String, PaymentStatus) -> Unit,
    onUpdateOrderStatus: (String, OrderStatus, String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.allOrders, key = { it.order.id }) { detailed ->
            val o = detailed.order
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${o.id} • ${formatBdt(o.totalPayable)}", fontWeight = FontWeight.Bold)
                        StatusPill(
                            text = if (lang == AppLanguage.BANGLA) o.orderStatus.titleBn else o.orderStatus.titleEn,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text("${o.paymentMethod.titleEn} (${o.transactionId.ifBlank { "COD" }}) • ${if (lang == AppLanguage.BANGLA) o.paymentStatus.titleBn else o.paymentStatus.titleEn}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "${if (lang == AppLanguage.BANGLA) "কমিশন" else "Commission"}: ${formatBdt(o.realizedCommissionAmount)} (${if (lang == AppLanguage.BANGLA) o.commissionStatus.titleBn else o.commissionStatus.titleEn})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (o.paymentStatus != PaymentStatus.VERIFIED && o.orderStatus != OrderStatus.REFUNDED) {
                            OutlinedButton(onClick = { onVerifyPayment(o.id, PaymentStatus.VERIFIED) }, modifier = Modifier.weight(1f)) {
                                Text(if (lang == AppLanguage.BANGLA) "পেমেন্ট যাচাই" else "Verify Payment")
                            }
                        }
                        if (o.orderStatus == OrderStatus.REFUND_REQUESTED || o.orderStatus == OrderStatus.DELIVERED) {
                            Button(onClick = { onVerifyPayment(o.id, PaymentStatus.REFUNDED) }, modifier = Modifier.weight(1f)) {
                                Text(if (lang == AppLanguage.BANGLA) "রিফান্ড অনুমোদন" else "Approve Refund")
                            }
                        }
                        if (o.orderStatus == OrderStatus.DELIVERED) {
                            Button(
                                onClick = { onUpdateOrderStatus(o.id, OrderStatus.COMPLETED, "Admin completed order") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (lang == AppLanguage.BANGLA) "সম্পন্ন করুন" else "Complete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminWithdrawalsTab(
    state: KrishiUiState,
    lang: AppLanguage,
    onWithdrawAdminCommission: (Double, PaymentMethod, String, String) -> Unit,
    onResolveWithdrawal: (WithdrawalEntity, WithdrawalStatus, String) -> Unit
) {
    val rep = state.adminReport
    var amountText by rememberSaveable { mutableStateOf("") }
    var accountPhone by rememberSaveable(state.currentUser?.id) {
        mutableStateOf(state.currentUser?.phone ?: "01700000001")
    }
    var method by rememberSaveable { mutableStateOf(PaymentMethod.BKASH) }
    var note by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section 1: Main Admin Commission Withdrawal
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (lang == AppLanguage.BANGLA) "অ্যাডমিনের কমিশন টাকা উত্তোলন" else "Admin Commission Withdrawal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (lang == AppLanguage.BANGLA) "উত্তোলনযোগ্য ব্যালেন্স" else "Available Balance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatBdt(rep.adminAvailableCommissionBdt),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                if (rep.adminAvailableCommissionBdt > 0.0) {
                                    amountText = rep.adminAvailableCommissionBdt.toInt().toString()
                                }
                            }
                        ) {
                            Text(if (lang == AppLanguage.BANGLA) "সর্বোচ্চ পরিমাণ" else "Max")
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(if (lang == AppLanguage.BANGLA) "উত্তোলনের পরিমাণ (৳)" else "Withdrawal Amount (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_withdraw_amount_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        label = { Text(if (lang == AppLanguage.BANGLA) "অ্যাডমিনের বিকাশ/নগদ/রকেট নম্বর" else "Admin Payout Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_withdraw_account_input")
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(if (lang == AppLanguage.BANGLA) "মন্তব্য / রেফারেন্স (ঐচ্ছিক)" else "Note / Reference (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            onWithdrawAdminCommission(amt, method, accountPhone, note)
                            amountText = ""
                            note = ""
                        },
                        enabled = rep.adminAvailableCommissionBdt > 0.0,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_withdraw_submit_button")
                    ) {
                        Text(if (lang == AppLanguage.BANGLA) "কমিশন টাকা উত্তোলন করুন" else "Withdraw Commission")
                    }
                }
            }
        }

        if (state.adminWithdrawals.isNotEmpty()) {
            item {
                Text(
                    text = if (lang == AppLanguage.BANGLA) "অ্যাডমিন উত্তোলনের ইতিহাস (${state.adminWithdrawals.size})" else "Admin Withdrawal History (${state.adminWithdrawals.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(state.adminWithdrawals, key = { it.id }) { awd ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${awd.id} • ${formatBdt(awd.amountBdt)}", fontWeight = FontWeight.Bold)
                            Text("${awd.payoutMethod.titleEn}: ${awd.payoutAccount} • ${awd.adminNote}", style = MaterialTheme.typography.bodySmall)
                        }
                        StatusPill(
                            text = if (lang == AppLanguage.BANGLA) awd.status.titleBn else awd.status.titleEn,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = if (lang == AppLanguage.BANGLA) "বিক্রেতাদের উত্তোলন আবেদন (${state.allWithdrawals.size})" else "Seller Withdrawal Requests (${state.allWithdrawals.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(state.allWithdrawals, key = { it.id }) { wd ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${wd.shopName} • ${formatBdt(wd.amountBdt)}", fontWeight = FontWeight.Bold)
                        Text("${wd.payoutMethod.titleEn}: ${wd.payoutAccount}", style = MaterialTheme.typography.bodySmall)
                        StatusPill(
                            text = if (lang == AppLanguage.BANGLA) wd.status.titleBn else wd.status.titleEn,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (wd.status == WithdrawalStatus.PENDING) {
                        Row {
                            IconButton(onClick = { onResolveWithdrawal(wd, WithdrawalStatus.APPROVED, "Paid via MFS") }) {
                                Icon(Icons.Filled.Check, contentDescription = "Approve", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onResolveWithdrawal(wd, WithdrawalStatus.REJECTED, "Rejected by Admin") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Reject", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminCommissionSettingsTab(
    state: KrishiUiState,
    lang: AppLanguage,
    onSaveSettings: (PlatformSettingsEntity) -> Unit,
    onGenerateBackup: () -> Unit
) {
    val s = state.settings
    var defaultComm by remember(s) { mutableStateOf(s.defaultCommissionPercent.toInt().toString()) }
    var minComm by remember(s) { mutableStateOf(s.minCommissionPercent.toInt().toString()) }
    var maxComm by remember(s) { mutableStateOf(s.maxCommissionPercent.toInt().toString()) }
    var sellerFee by remember(s) { mutableStateOf(s.sellerDeliveryChargeBdt.toInt().toString()) }
    var courierFee by remember(s) { mutableStateOf(s.courierDeliveryChargeBdt.toInt().toString()) }
    var bannerBn by remember(s) { mutableStateOf(s.siteBannerTitleBn) }
    var bannerEn by remember(s) { mutableStateOf(s.siteBannerTitleEn) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (lang == AppLanguage.BANGLA) "কমিশন নীতিমালা (ডিফল্ট ৫০%)" else "Commission Policy (Default 50%)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = defaultComm,
                        onValueChange = { defaultComm = it },
                        label = { Text(if (lang == AppLanguage.BANGLA) "ডিফল্ট কমিশন (%)" else "Default Commission (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("admin_default_commission_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = minComm,
                            onValueChange = { minComm = it },
                            label = { Text(if (lang == AppLanguage.BANGLA) "সর্বনিম্ন (%)" else "Min (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxComm,
                            onValueChange = { maxComm = it },
                            label = { Text(if (lang == AppLanguage.BANGLA) "সর্বোচ্চ (%)" else "Max (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider()
                    Text(
                        text = if (lang == AppLanguage.BANGLA) "ডেলিভারি চার্জ ও ব্যানার সেটিংস" else "Delivery Charges & Banner Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = sellerFee,
                            onValueChange = { sellerFee = it },
                            label = { Text(if (lang == AppLanguage.BANGLA) "বিক্রেতা ডেলিভারি (৳)" else "Seller Delivery (৳)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = courierFee,
                            onValueChange = { courierFee = it },
                            label = { Text(if (lang == AppLanguage.BANGLA) "কুরিয়ার ডেলিভারি (৳)" else "Courier Delivery (৳)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(value = bannerBn, onValueChange = { bannerBn = it }, label = { Text("Banner Title (বাংলা)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bannerEn, onValueChange = { bannerEn = it }, label = { Text("Banner Title (English)") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            onSaveSettings(
                                s.copy(
                                    defaultCommissionPercent = defaultComm.toDoubleOrNull() ?: 50.0,
                                    minCommissionPercent = minComm.toDoubleOrNull() ?: 5.0,
                                    maxCommissionPercent = maxComm.toDoubleOrNull() ?: 60.0,
                                    sellerDeliveryChargeBdt = sellerFee.toDoubleOrNull() ?: 60.0,
                                    courierDeliveryChargeBdt = courierFee.toDoubleOrNull() ?: 110.0,
                                    siteBannerTitleBn = bannerBn,
                                    siteBannerTitleEn = bannerEn
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_admin_settings_button")
                    ) {
                        Text(if (lang == AppLanguage.BANGLA) "সেটিংস সংরক্ষণ করুন" else "Save Platform Settings")
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onGenerateBackup,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_backup_button")
            ) {
                Icon(Icons.Filled.Backup, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (lang == AppLanguage.BANGLA) "ডাটাবেস ব্যাকআপ তৈরি করুন (JSON)" else "Create Database Backup (JSON)")
            }
        }
    }
}
