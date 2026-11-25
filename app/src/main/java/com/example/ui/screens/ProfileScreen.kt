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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.UserRole
import com.example.ui.components.PolicyDialog
import com.example.ui.components.StatusPill
import com.example.viewmodel.KrishiUiState

@Composable
fun ProfileScreen(
    state: KrishiUiState,
    onToggleLanguage: () -> Unit,
    onUpdateAddress: (String, String) -> Unit,
    onSwitchRole: (UserRole) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    val user = state.currentUser ?: return
    var address by rememberSaveable(user.id) { mutableStateOf(user.defaultAddress) }
    var district by rememberSaveable(user.id) { mutableStateOf(user.district) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(user.email, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        StatusPill(
                            text = if (lang == AppLanguage.BANGLA) user.role.titleBn else user.role.titleEn,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (lang == AppLanguage.BANGLA) "অ্যাপের ভাষা (বাংলা / English)" else "App Language (বাংলা / English)", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(onClick = onToggleLanguage, modifier = Modifier.testTag("profile_language_toggle")) {
                        Text(if (lang == AppLanguage.BANGLA) "English" else "বাংলা")
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(AppStrings.deliveryAddress(lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text(if (lang == AppLanguage.BANGLA) "জেলা" else "District") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(if (lang == AppLanguage.BANGLA) "ঠিকানা" else "Address") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onUpdateAddress(address, district) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (lang == AppLanguage.BANGLA) "ঠিকানা সংরক্ষণ" else "Save Address")
                    }
                }
            }
        }

        if (user.role != UserRole.MAIN_ADMIN) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (lang == AppLanguage.BANGLA) "ক্রেতা / বিক্রেতা মোড পরিবর্তন" else "Switch Buyer / Seller Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = { onSwitchRole(UserRole.BUYER) },
                            enabled = user.role != UserRole.BUYER,
                            modifier = Modifier.fillMaxWidth().testTag("switch_buyer")
                        ) {
                            Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (lang == AppLanguage.BANGLA) "ক্রেতা হিসেবে ব্যবহার করুন" else "Use as Buyer")
                        }
                        OutlinedButton(
                            onClick = { onSwitchRole(UserRole.SELLER) },
                            enabled = user.role != UserRole.SELLER,
                            modifier = Modifier.fillMaxWidth().testTag("switch_seller")
                        ) {
                            Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (lang == AppLanguage.BANGLA) "বিক্রেতা পোর্টাল চালু করুন (দোকান খুলুন)" else "Enable Seller Portal (Open Shop)")
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showPrivacy = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PrivacyTip, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.privacyPolicyTitle(lang))
                }
                OutlinedButton(onClick = { showTerms = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Description, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.termsTitle(lang))
                }
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == AppLanguage.BANGLA) "গুগল সাইন আউট" else "Sign Out")
                }
            }
        }
    }

    if (showPrivacy) {
        PolicyDialog(
            title = AppStrings.privacyPolicyTitle(lang),
            body = AppStrings.privacyPolicyBody(lang),
            dismissLabel = if (lang == AppLanguage.BANGLA) "বন্ধ করুন" else "Close",
            onDismiss = { showPrivacy = false }
        )
    }
    if (showTerms) {
        PolicyDialog(
            title = AppStrings.termsTitle(lang),
            body = AppStrings.termsBody(lang),
            dismissLabel = if (lang == AppLanguage.BANGLA) "বন্ধ করুন" else "Close",
            onDismiss = { showTerms = false }
        )
    }
}
