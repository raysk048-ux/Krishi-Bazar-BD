package com.example.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppPreferences
import com.example.data.FirestoreKrishiRepository
import com.example.data.KrishiShowcaseData
import com.example.firebase.authStateFlow
import com.example.firebase.signOutWithCleanup
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.ProductCategory
import com.example.model.UserRole
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CartCheckoutScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SellerDashboardScreen
import com.example.ui.screens.SellerShopScreen
import com.example.viewmodel.CloudKrishiViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

enum class MainTab {
    HOME, CART, ORDERS, SELLER, ADMIN, PROFILE
}

private data class NavTabMeta(
    val tab: MainTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun KrishiApp(
    repository: FirestoreKrishiRepository,
    prefs: AppPreferences,
    credentialManager: CredentialManager
) {
    val authFlow = remember { Firebase.auth.authStateFlow() }
    val firebaseUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    if (firebaseUser == null) {
        GuestKrishiApp(prefs = prefs)
    } else {
        val currentFirebaseUser = firebaseUser!!
        key(currentFirebaseUser.uid) {
            val cloudViewModel: CloudKrishiViewModel = viewModel(
                key = currentFirebaseUser.uid,
                factory = CloudKrishiViewModel.provideFactory(repository, currentFirebaseUser)
            )
            SignedInKrishiApp(
                viewModel = cloudViewModel,
                firebaseUid = currentFirebaseUser.uid,
                credentialManager = credentialManager
            )
        }
    }
}

@Composable
private fun GuestKrishiApp(prefs: AppPreferences) {
    val language by prefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.BANGLA)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<ProductCategory?>(null) }
    var selectedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedShopId by rememberSaveable { mutableStateOf<String?>(null) }

    val state = remember(language, searchQuery, selectedCategory) {
        KrishiShowcaseData.buildGuestUiState(
            language = language,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory
        )
    }

    val toggleLanguage: () -> Unit = {
        val next = if (language == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
        scope.launch { prefs.setLanguage(next) }
    }

    val promptSignIn: () -> Unit = {
        activeTab = MainTab.PROFILE
        scope.launch {
            snackbarHostState.showSnackbar(
                if (language == AppLanguage.BANGLA) {
                    "অর্ডার ও কার্ট ব্যবহার করতে গুগল দিয়ে সাইন-ইন করুন"
                } else {
                    "Sign in with Google to use cart & orders"
                }
            )
        }
    }

    val selectedProduct = selectedProductId?.let { pid ->
        state.allProducts.firstOrNull { it.product.id == pid }
    }
    val selectedShop = selectedShopId?.let { sid ->
        state.allShops.firstOrNull { it.id == sid }
    }

    if (selectedProduct != null) {
        BackHandler { selectedProductId = null }
        val reviews = state.allReviews.filter { it.productId == selectedProduct.product.id }
        ProductDetailScreen(
            productWithShop = selectedProduct,
            reviews = reviews,
            lang = language,
            onBack = { selectedProductId = null },
            onOpenShop = { shop ->
                selectedProductId = null
                selectedShopId = shop.id
            },
            onAddToCart = { _, _ ->
                selectedProductId = null
                promptSignIn()
            }
        )
        return
    }

    if (selectedShop != null) {
        BackHandler { selectedShopId = null }
        val shopProducts = state.allProducts.filter { it.product.shopId == selectedShop.id }
        SellerShopScreen(
            shop = selectedShop,
            shopProducts = shopProducts,
            lang = language,
            onBack = { selectedShopId = null },
            onOpenProduct = { pws -> selectedProductId = pws.product.id },
            onAddToCart = { _ ->
                selectedShopId = null
                promptSignIn()
            }
        )
        return
    }

    if (activeTab != MainTab.HOME) {
        BackHandler { activeTab = MainTab.HOME }
    }

    val navTabs = listOf(
        NavTabMeta(MainTab.HOME, AppStrings.navHome(language), Icons.Filled.Home, Icons.Outlined.Home),
        NavTabMeta(MainTab.CART, AppStrings.navCart(language), Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
        NavTabMeta(MainTab.ORDERS, AppStrings.navOrders(language), Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
        NavTabMeta(
            MainTab.PROFILE,
            if (language == AppLanguage.BANGLA) "সাইন-ইন" else "Sign In",
            Icons.Filled.Login,
            Icons.Outlined.Login
        )
    )

    AdaptiveNavShell(
        navTabs = navTabs,
        activeTab = activeTab,
        onSelectTab = { activeTab = it },
        snackbarHostState = snackbarHostState
    ) {
        when (activeTab) {
            MainTab.HOME -> HomeScreen(
                state = state,
                onSearchChange = { searchQuery = it },
                onSelectCategory = { selectedCategory = it },
                onToggleLanguage = toggleLanguage,
                onOpenProduct = { pws -> selectedProductId = pws.product.id },
                onOpenShop = { shop -> selectedShopId = shop.id },
                onAddToCart = { _ -> promptSignIn() },
                onRequestSignIn = { activeTab = MainTab.PROFILE }
            )
            MainTab.CART, MainTab.ORDERS, MainTab.SELLER, MainTab.ADMIN, MainTab.PROFILE -> AuthScreen(
                language = language,
                onToggleLanguage = toggleLanguage,
                onAuthSuccess = { activeTab = MainTab.HOME }
            )
        }
    }
}

@Composable
private fun SignedInKrishiApp(
    viewModel: CloudKrishiViewModel,
    firebaseUid: String,
    credentialManager: CredentialManager
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val performSignOut: () -> Unit = {
        signOutWithCleanup(credentialManager = credentialManager, scope = scope)
    }

    if (!state.isReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentUser = state.currentUser
    val lang = state.language

    if (currentUser == null) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.streamError ?: if (lang == AppLanguage.BANGLA) {
                        "প্রোফাইল লোড করা যাচ্ছে না"
                    } else {
                        "Unable to load user profile"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = performSignOut) {
                    Text(if (lang == AppLanguage.BANGLA) "সাইন আউট" else "Sign Out")
                }
            }
        }
        return
    }

    if (currentUser.isBlocked) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (lang == AppLanguage.BANGLA) {
                        "এই অ্যাকাউন্টটি অ্যাডমিন দ্বারা স্থগিত করা হয়েছে"
                    } else {
                        "This account has been blocked by Main Admin"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = performSignOut) {
                    Text(if (lang == AppLanguage.BANGLA) "অন্য অ্যাকাউন্টে প্রবেশ করুন" else "Switch Account")
                }
            }
        }
        return
    }

    var activeTab by rememberSaveable(firebaseUid) { mutableStateOf(MainTab.HOME) }
    var initialRoleRouted by rememberSaveable(firebaseUid) { mutableStateOf(false) }

    LaunchedEffect(currentUser.role) {
        if (!initialRoleRouted) {
            activeTab = when (currentUser.role) {
                UserRole.MAIN_ADMIN -> MainTab.ADMIN
                UserRole.SELLER -> MainTab.SELLER
                UserRole.BUYER -> MainTab.HOME
            }
            initialRoleRouted = true
        }
    }

    var selectedProductId by rememberSaveable(firebaseUid) { mutableStateOf<String?>(null) }
    var selectedShopId by rememberSaveable(firebaseUid) { mutableStateOf<String?>(null) }

    val selectedProduct = selectedProductId?.let { pid ->
        state.allProducts.firstOrNull { it.product.id == pid }
    }
    val selectedShop = selectedShopId?.let { sid ->
        state.allShops.firstOrNull { it.id == sid }
    }

    if (selectedProduct != null) {
        BackHandler { selectedProductId = null }
        val reviews = state.allReviews.filter { it.productId == selectedProduct.product.id }
        ProductDetailScreen(
            productWithShop = selectedProduct,
            reviews = reviews,
            lang = lang,
            onBack = { selectedProductId = null },
            onOpenShop = { shop ->
                selectedProductId = null
                selectedShopId = shop.id
            },
            onAddToCart = { pid, qty ->
                viewModel.addToCart(pid, qty)
                selectedProductId = null
            }
        )
        return
    }

    if (selectedShop != null) {
        BackHandler { selectedShopId = null }
        val shopProducts = state.approvedProducts.filter { it.product.shopId == selectedShop.id }
        SellerShopScreen(
            shop = selectedShop,
            shopProducts = shopProducts,
            lang = lang,
            onBack = { selectedShopId = null },
            onOpenProduct = { pws -> selectedProductId = pws.product.id },
            onAddToCart = { pid -> viewModel.addToCart(pid, 1) }
        )
        return
    }

    if (activeTab != MainTab.HOME) {
        BackHandler { activeTab = MainTab.HOME }
    }

    val navTabs = buildList {
        add(NavTabMeta(MainTab.HOME, AppStrings.navHome(lang), Icons.Filled.Home, Icons.Outlined.Home))
        if (currentUser.role == UserRole.SELLER) {
            add(NavTabMeta(MainTab.SELLER, AppStrings.navSeller(lang), Icons.Filled.Storefront, Icons.Outlined.Storefront))
        }
        if (currentUser.role == UserRole.MAIN_ADMIN) {
            val pendingAlerts = state.adminReport.pendingShopsCount +
                state.adminReport.pendingProductsCount +
                state.adminReport.pendingWithdrawalsCount
            add(
                NavTabMeta(
                    MainTab.ADMIN,
                    AppStrings.navAdmin(lang),
                    Icons.Filled.AdminPanelSettings,
                    Icons.Outlined.AdminPanelSettings,
                    badgeCount = pendingAlerts
                )
            )
        }
        add(
            NavTabMeta(
                MainTab.CART,
                AppStrings.navCart(lang),
                Icons.Filled.ShoppingCart,
                Icons.Outlined.ShoppingCart,
                badgeCount = state.cartCount
            )
        )
        add(NavTabMeta(MainTab.ORDERS, AppStrings.navOrders(lang), Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong))
        add(NavTabMeta(MainTab.PROFILE, AppStrings.navProfile(lang), Icons.Filled.Person, Icons.Outlined.Person))
    }

    AdaptiveNavShell(
        navTabs = navTabs,
        activeTab = activeTab,
        onSelectTab = { activeTab = it },
        snackbarHostState = snackbarHostState
    ) {
        when (activeTab) {
            MainTab.HOME -> HomeScreen(
                state = state,
                onSearchChange = viewModel::setSearchQuery,
                onSelectCategory = viewModel::selectCategory,
                onToggleLanguage = viewModel::toggleLanguage,
                onOpenProduct = { pws -> selectedProductId = pws.product.id },
                onOpenShop = { shop -> selectedShopId = shop.id },
                onAddToCart = { pid -> viewModel.addToCart(pid, 1) }
            )
            MainTab.CART -> CartCheckoutScreen(
                state = state,
                onUpdateQty = viewModel::updateCartQty,
                onRemoveItem = viewModel::removeCartItem,
                onPlaceOrder = { del, pay, trx, phone, addr, dist ->
                    viewModel.placeOrder(del, pay, trx, phone, addr, dist) {
                        activeTab = MainTab.ORDERS
                    }
                }
            )
            MainTab.ORDERS -> OrdersScreen(
                state = state,
                onUpdateOrderStatus = viewModel::updateOrderStatus,
                onSubmitReview = viewModel::submitReview
            )
            MainTab.SELLER -> SellerDashboardScreen(
                state = state,
                onSaveShop = viewModel::saveShop,
                onSaveProduct = viewModel::saveProduct,
                onAdjustStock = viewModel::adjustStock,
                onUpdateOrderStatus = viewModel::updateOrderStatus,
                onRequestWithdrawal = viewModel::requestWithdrawal
            )
            MainTab.ADMIN -> if (currentUser.role == UserRole.MAIN_ADMIN) {
                AdminDashboardScreen(
                    state = state,
                    onApproveShop = viewModel::approveShop,
                    onApproveProduct = viewModel::approveProduct,
                    onUpdateProductCommission = viewModel::updateProductCommission,
                    onToggleFeatured = viewModel::toggleFeatured,
                    onSetUserBlocked = viewModel::setUserBlocked,
                    onVerifyPayment = viewModel::verifyPayment,
                    onUpdateOrderStatus = viewModel::updateOrderStatus,
                    onResolveWithdrawal = viewModel::resolveWithdrawal,
                    onWithdrawAdminCommission = viewModel::withdrawAdminCommission,
                    onSaveSettings = viewModel::saveSettings,
                    onGenerateBackup = viewModel::generateBackup,
                    onClearBackupPreview = viewModel::clearBackupPreview
                )
            } else {
                HomeScreen(
                    state = state,
                    onSearchChange = viewModel::setSearchQuery,
                    onSelectCategory = viewModel::selectCategory,
                    onToggleLanguage = viewModel::toggleLanguage,
                    onOpenProduct = { pws -> selectedProductId = pws.product.id },
                    onOpenShop = { shop -> selectedShopId = shop.id },
                    onAddToCart = { pid -> viewModel.addToCart(pid, 1) }
                )
            }
            MainTab.PROFILE -> ProfileScreen(
                state = state,
                onToggleLanguage = viewModel::toggleLanguage,
                onUpdateAddress = viewModel::updateAddress,
                onSwitchRole = { role ->
                    viewModel.switchRole(role)
                    if (role == UserRole.SELLER) {
                        activeTab = MainTab.SELLER
                    }
                },
                onLogout = performSignOut
            )
        }
    }
}

@Composable
private fun AdaptiveNavShell(
    navTabs: List<NavTabMeta>,
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navTabs.forEach { item ->
                            val selected = activeTab == item.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(item.tab) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge { Text("${item.badgeCount}") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = { Text(item.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${item.tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        navTabs.forEach { item ->
                            val selected = activeTab == item.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onSelectTab(item.tab) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (item.badgeCount > 0) {
                                                Badge { Text("${item.badgeCount}") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = { Text(item.label, maxLines = 1) },
                                modifier = Modifier.testTag("rail_tab_${item.tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    content()
                }
            }
        }
    }
}
