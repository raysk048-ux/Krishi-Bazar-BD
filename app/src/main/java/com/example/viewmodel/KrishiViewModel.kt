package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreKrishiRepository
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.firebase.unwrapFirestoreException
import com.example.model.AdminReportSummary
import com.example.model.AppLanguage
import com.example.model.ApprovalStatus
import com.example.model.CartItemDetailed
import com.example.model.CommissionCalculator
import com.example.model.DeliveryMethod
import com.example.model.OrderDetailed
import com.example.model.OrderEntity
import com.example.model.OrderItemEntity
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.PlatformSettingsEntity
import com.example.model.ProductCategory
import com.example.model.ProductEntity
import com.example.model.ProductWithShop
import com.example.model.ReviewEntity
import com.example.model.SellerFinanceSummary
import com.example.model.ShopEntity
import com.example.model.UserEntity
import com.example.model.UserRole
import com.example.model.WithdrawalEntity
import com.example.model.WithdrawalStatus
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KrishiUiState(
    val isReady: Boolean = false,
    val language: AppLanguage = AppLanguage.BANGLA,
    val currentUser: UserEntity? = null,
    val settings: PlatformSettingsEntity = PlatformSettingsEntity(),
    val searchQuery: String = "",
    val selectedCategory: ProductCategory? = null,
    val allUsers: List<UserEntity> = emptyList(),
    val allShops: List<ShopEntity> = emptyList(),
    val approvedShops: List<ShopEntity> = emptyList(),
    val allProducts: List<ProductWithShop> = emptyList(),
    val approvedProducts: List<ProductWithShop> = emptyList(),
    val featuredProducts: List<ProductWithShop> = emptyList(),
    val cartItems: List<CartItemDetailed> = emptyList(),
    val buyerOrders: List<OrderDetailed> = emptyList(),
    val sellerShop: ShopEntity? = null,
    val sellerProducts: List<ProductWithShop> = emptyList(),
    val sellerOrders: List<OrderDetailed> = emptyList(),
    val sellerWithdrawals: List<WithdrawalEntity> = emptyList(),
    val sellerFinance: SellerFinanceSummary = SellerFinanceSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
    val allOrders: List<OrderDetailed> = emptyList(),
    val allWithdrawals: List<WithdrawalEntity> = emptyList(),
    val adminWithdrawals: List<WithdrawalEntity> = emptyList(),
    val allReviews: List<ReviewEntity> = emptyList(),
    val adminReport: AdminReportSummary = AdminReportSummary(0, 0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, emptyList()),
    val snackbarMessage: String? = null,
    val lastBackupJson: String? = null,
    val streamError: String? = null
) {
    val cartCount: Int get() = cartItems.sumOf { it.cartItem.quantity }
    val cartSubtotal: Double get() = cartItems.sumOf { it.lineSubtotal }
}

private data class CloudUserCore(
    val lang: AppLanguage,
    val currentUser: UserEntity?,
    val settings: PlatformSettingsEntity,
    val approvedShops: List<ShopEntity>,
    val sellerShops: List<ShopEntity>
)

private data class CloudCatalogSlice(
    val approvedProducts: List<ProductEntity>,
    val sellerProducts: List<ProductEntity>,
    val reviews: List<ReviewEntity>,
    val cart: List<com.example.model.CartItemEntity>,
    val buyerOrders: List<Pair<OrderEntity, List<OrderItemEntity>>>
)

private data class CloudSellerAdminSlice(
    val sellerOrders: List<Pair<OrderEntity, List<OrderItemEntity>>>,
    val sellerWithdrawals: List<WithdrawalEntity>,
    val adminUsers: List<UserEntity>,
    val adminShops: List<ShopEntity>,
    val adminProducts: List<ProductEntity>,
    val adminOrders: List<Pair<OrderEntity, List<OrderItemEntity>>>,
    val adminWithdrawals: List<WithdrawalEntity>
)

private data class CloudFilterSlice(
    val searchQuery: String,
    val selectedCategory: ProductCategory?,
    val snackbarMessage: String?,
    val lastBackupJson: String?,
    val profileReady: Boolean,
    val streamError: String?
)

class CloudKrishiViewModel(
    private val repository: FirestoreKrishiRepository,
    private val firebaseUser: FirebaseUser
) : ViewModel() {

    private val uid: String = firebaseUser.uid
    private val isOwnerAdmin: Boolean = repository.isOwnerAdminUser(firebaseUser)

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<ProductCategory?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    private val _lastBackupJson = MutableStateFlow<String?>(null)
    private val _profileReady = MutableStateFlow(false)
    private val _streamError = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(firebaseUser)
                .onFailure { err ->
                    _streamError.value = err.localizedMessage
                    _snackbarMessage.value = err.localizedMessage
                }
            _profileReady.value = true
        }
    }

    private fun <T> Flow<T>.safeFirestoreFlow(path: String, fallback: T): Flow<T> =
        this.catch { error ->
            val firestoreError = unwrapFirestoreException(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing $path", root)
            if (firestoreError != null) {
                handleFirestoreError(firestoreError, OperationType.LIST, path)
            }
            _streamError.value = root.localizedMessage ?: "Failed to sync $path"
            emit(fallback)
        }

    private val coreFlow = combine(
        repository.languageFlow,
        repository.observeUserProfile(uid).safeFirestoreFlow("users/$uid", null),
        repository.observeSettings().safeFirestoreFlow("platform_settings/global", PlatformSettingsEntity()),
        repository.observeApprovedShops().safeFirestoreFlow("shops", emptyList()),
        repository.observeSellerShops(uid).safeFirestoreFlow("shops", emptyList())
    ) { lang, user, settings, approvedShops, sellerShops ->
        CloudUserCore(lang, user, settings, approvedShops, sellerShops)
    }

    private val catalogFlow = combine(
        repository.observeApprovedProducts().safeFirestoreFlow("products", emptyList()),
        repository.observeSellerProducts(uid).safeFirestoreFlow("products", emptyList()),
        repository.observePublishedReviews().safeFirestoreFlow("reviews", emptyList()),
        repository.observeCart(uid).safeFirestoreFlow("users/$uid/cart", emptyList()),
        repository.observeBuyerOrders(uid).safeFirestoreFlow("orders", emptyList())
    ) { approvedProds, sellerProds, reviews, cart, buyerOrders ->
        CloudCatalogSlice(approvedProds, sellerProds, reviews, cart, buyerOrders)
    }

    private val adminUsersFlow = if (isOwnerAdmin) {
        repository.observeAllUsersAdmin().safeFirestoreFlow("users", emptyList())
    } else flowOf(emptyList())

    private val adminShopsFlow = if (isOwnerAdmin) {
        repository.observeAllShopsAdmin().safeFirestoreFlow("shops", emptyList())
    } else flowOf(emptyList())

    private val adminProductsFlow = if (isOwnerAdmin) {
        repository.observeAllProductsAdmin().safeFirestoreFlow("products", emptyList())
    } else flowOf(emptyList())

    private val adminOrdersFlow = if (isOwnerAdmin) {
        repository.observeAllOrdersAdmin().safeFirestoreFlow("orders", emptyList())
    } else flowOf(emptyList())

    private val adminWithdrawalsFlow = if (isOwnerAdmin) {
        repository.observeAllWithdrawalsAdmin().safeFirestoreFlow("withdrawals", emptyList())
    } else flowOf(emptyList())

    private val sellerAndAdminFlow = combine(
        repository.observeSellerOrders(uid).safeFirestoreFlow("orders", emptyList()),
        repository.observeSellerWithdrawals(uid).safeFirestoreFlow("withdrawals", emptyList()),
        combine(adminUsersFlow, adminShopsFlow, adminProductsFlow) { u, s, p -> Triple(u, s, p) },
        combine(adminOrdersFlow, adminWithdrawalsFlow) { o, w -> o to w }
    ) { sellerOrders, sellerWithdrawals, usp, ow ->
        CloudSellerAdminSlice(
            sellerOrders = sellerOrders,
            sellerWithdrawals = sellerWithdrawals,
            adminUsers = usp.first,
            adminShops = usp.second,
            adminProducts = usp.third,
            adminOrders = ow.first,
            adminWithdrawals = ow.second
        )
    }

    private val filterFlow = combine(
        combine(_searchQuery, _selectedCategory, _snackbarMessage) { q, c, m -> Triple(q, c, m) },
        combine(_lastBackupJson, _profileReady, _streamError) { b, r, e -> Triple(b, r, e) }
    ) { qcm, bre ->
        CloudFilterSlice(
            searchQuery = qcm.first,
            selectedCategory = qcm.second,
            snackbarMessage = qcm.third,
            lastBackupJson = bre.first,
            profileReady = bre.second,
            streamError = bre.third
        )
    }

    val uiState: StateFlow<KrishiUiState> = combine(
        coreFlow,
        catalogFlow,
        sellerAndAdminFlow,
        filterFlow
    ) { core, catalog, sa, filters ->
        val mergedShops = (sa.adminShops + core.approvedShops + core.sellerShops)
            .associateBy { it.id }.values.toList()
        val shopsMap = mergedShops.associateBy { it.id }

        val mergedProducts = (sa.adminProducts + catalog.approvedProducts + catalog.sellerProducts)
            .associateBy { it.id }.values.toList()
        val productsMap = mergedProducts.associateBy { it.id }

        val reviewsByProduct = catalog.reviews.groupBy { it.productId }

        fun ProductEntity.toWithShop(): ProductWithShop {
            val prodReviews = reviewsByProduct[id].orEmpty()
            val avg = if (prodReviews.isEmpty()) 0.0 else prodReviews.map { it.rating }.average()
            return ProductWithShop(
                product = this,
                shop = shopsMap[shopId],
                effectiveCommissionPercent = CommissionCalculator.effectiveRate(
                    commissionPercentOverride,
                    core.settings
                ),
                averageRating = avg,
                reviewCount = prodReviews.size
            )
        }

        val allEnriched = mergedProducts.map { it.toWithShop() }
        val approvedShopIds = core.approvedShops.map { it.id }.toSet()
        val liveApproved = catalog.approvedProducts
            .filter { it.shopId in approvedShopIds }
            .map { it.toWithShop() }

        val q = filters.searchQuery.trim().lowercase()
        val filteredApproved = liveApproved.filter { pws ->
            val catOk = filters.selectedCategory == null || pws.product.category == filters.selectedCategory
            val qOk = q.isEmpty() ||
                pws.product.nameBn.lowercase().contains(q) ||
                pws.product.nameEn.lowercase().contains(q) ||
                (pws.shop?.shopNameBn?.lowercase()?.contains(q) == true) ||
                (pws.shop?.shopNameEn?.lowercase()?.contains(q) == true)
            catOk && qOk
        }

        val detailedCart = catalog.cart.mapNotNull { c ->
            val prod = productsMap[c.productId] ?: return@mapNotNull null
            CartItemDetailed(
                cartItem = c,
                product = prod,
                shop = shopsMap[prod.shopId],
                effectiveCommissionPercent = CommissionCalculator.effectiveRate(
                    prod.commissionPercentOverride,
                    core.settings
                )
            )
        }

        fun List<Pair<OrderEntity, List<OrderItemEntity>>>.toDetailed(): List<OrderDetailed> =
            map { (order, items) ->
                OrderDetailed(order = order, items = items, shop = shopsMap[order.shopId])
            }

        val buyerOrdersDetailed = catalog.buyerOrders.toDetailed()
        val sellerOrdersDetailed = sa.sellerOrders.toDetailed()
        val adminOrdersDetailed = sa.adminOrders.toDetailed()

        val sellerShop = core.sellerShops.firstOrNull()
        val sellerProductsDetailed = catalog.sellerProducts.map { it.toWithShop() }
        val sellerOnlyWithdrawalsForSeller = sa.sellerWithdrawals.filter {
            it.shopId != FirestoreKrishiRepository.PLATFORM_ADMIN_SHOP_ID
        }

        val completedSellerOrders = sellerOrdersDetailed.filter { it.order.orderStatus == OrderStatus.COMPLETED }
        val completedGross = completedSellerOrders.sumOf { it.order.grossSubtotal }
        val earnedComm = completedSellerOrders.sumOf { it.order.realizedCommissionAmount }
        val completedNet = completedSellerOrders.sumOf { it.order.sellerNetPayout }
        val pendingOrderVal = sellerOrdersDetailed.filter {
            it.order.orderStatus !in listOf(OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REFUNDED)
        }.sumOf { it.order.grossSubtotal }
        val approvedWithdrawn = sellerOnlyWithdrawalsForSeller.filter { it.status == WithdrawalStatus.APPROVED }.sumOf { it.amountBdt }
        val pendingWithdrawn = sellerOnlyWithdrawalsForSeller.filter { it.status == WithdrawalStatus.PENDING }.sumOf { it.amountBdt }
        val availSellerBalance = (completedNet - approvedWithdrawn - pendingWithdrawn).coerceAtLeast(0.0)

        val adminWithdrawals = sa.adminWithdrawals.filter { it.shopId == FirestoreKrishiRepository.PLATFORM_ADMIN_SHOP_ID }
        val allSellerWithdrawalsAdmin = sa.adminWithdrawals.filter { it.shopId != FirestoreKrishiRepository.PLATFORM_ADMIN_SHOP_ID }

        val completedAdminOrders = adminOrdersDetailed.filter { it.order.orderStatus == OrderStatus.COMPLETED }
        val cancelledOrRefundedAdmin = adminOrdersDetailed.filter {
            it.order.orderStatus == OrderStatus.CANCELLED || it.order.orderStatus == OrderStatus.REFUNDED
        }
        val categorySales = ProductCategory.entries.map { cat ->
            val sum = completedAdminOrders.flatMap { it.items }.filter { item ->
                productsMap[item.productId]?.category == cat
            }.sumOf { it.lineSubtotal }
            cat to sum
        }.filter { it.second > 0.0 }

        val totalPlatformComm = completedAdminOrders.sumOf { it.order.realizedCommissionAmount }
        val adminWithdrawn = adminWithdrawals.filter { it.status == WithdrawalStatus.APPROVED }.sumOf { it.amountBdt }
        val adminAvailable = (totalPlatformComm - adminWithdrawn).coerceAtLeast(0.0)

        val adminReport = AdminReportSummary(
            totalUsers = sa.adminUsers.size,
            totalShops = mergedShops.size,
            pendingShopsCount = mergedShops.count { it.approvalStatus == ApprovalStatus.PENDING },
            pendingProductsCount = mergedProducts.count { it.approvalStatus == ApprovalStatus.PENDING },
            pendingWithdrawalsCount = allSellerWithdrawalsAdmin.count { it.status == WithdrawalStatus.PENDING },
            refundRequestedCount = adminOrdersDetailed.count { it.order.orderStatus == OrderStatus.REFUND_REQUESTED },
            completedOrdersCount = completedAdminOrders.size,
            cancelledOrRefundedCount = cancelledOrRefundedAdmin.size,
            totalGrossSalesBdt = completedAdminOrders.sumOf { it.order.grossSubtotal },
            totalPlatformCommissionBdt = totalPlatformComm,
            totalAdjustedCommissionBdt = cancelledOrRefundedAdmin.sumOf { it.order.agreedCommissionTotal },
            totalSellerPayoutsBdt = completedAdminOrders.sumOf { it.order.sellerNetPayout },
            adminWithdrawnBdt = adminWithdrawn,
            adminAvailableCommissionBdt = adminAvailable,
            categorySalesBreakdown = categorySales
        )

        KrishiUiState(
            isReady = filters.profileReady,
            language = core.lang,
            currentUser = core.currentUser,
            settings = core.settings,
            searchQuery = filters.searchQuery,
            selectedCategory = filters.selectedCategory,
            allUsers = sa.adminUsers,
            allShops = mergedShops,
            approvedShops = core.approvedShops,
            allProducts = allEnriched,
            approvedProducts = filteredApproved,
            featuredProducts = liveApproved.filter { it.product.featured },
            cartItems = detailedCart,
            buyerOrders = buyerOrdersDetailed,
            sellerShop = sellerShop,
            sellerProducts = sellerProductsDetailed,
            sellerOrders = sellerOrdersDetailed,
            sellerWithdrawals = sellerOnlyWithdrawalsForSeller,
            sellerFinance = SellerFinanceSummary(
                completedGrossSales = completedGross,
                earnedCommissionDeducted = earnedComm,
                completedNetEarnings = completedNet,
                pendingOrderValue = pendingOrderVal,
                approvedWithdrawn = approvedWithdrawn,
                pendingWithdrawalAmount = pendingWithdrawn,
                availableBalance = availSellerBalance
            ),
            allOrders = adminOrdersDetailed,
            allWithdrawals = allSellerWithdrawalsAdmin,
            adminWithdrawals = adminWithdrawals,
            allReviews = catalog.reviews,
            adminReport = adminReport,
            snackbarMessage = filters.snackbarMessage,
            lastBackupJson = filters.lastBackupJson,
            streamError = filters.streamError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = KrishiUiState()
    )

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun selectCategory(cat: ProductCategory?) { _selectedCategory.value = cat }
    fun clearSnackbar() { _snackbarMessage.value = null }
    fun clearBackupPreview() { _lastBackupJson.value = null }

    fun toggleLanguage() {
        val next = if (uiState.value.language == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
        viewModelScope.launch { repository.setLanguage(next) }
    }

    fun switchRole(role: UserRole) {
        viewModelScope.launch {
            repository.switchBuyerSellerRole(role)
                .onSuccess {
                    showMessage(if (uiState.value.language == AppLanguage.BANGLA) "মোড পরিবর্তন হয়েছে" else "Role updated")
                }
                .onFailure { showMessage(it.message ?: "Failed") }
        }
    }

    fun updateAddress(address: String, district: String) {
        viewModelScope.launch {
            repository.updateUserAddress(null, null, address, district)
                .onSuccess { showMessage(if (uiState.value.language == AppLanguage.BANGLA) "ঠিকানা সংরক্ষণ হয়েছে" else "Address updated") }
                .onFailure { showMessage(it.message ?: "Update failed") }
        }
    }

    fun addToCart(productId: String, qty: Int = 1) {
        val state = uiState.value
        val existingQty = state.cartItems.firstOrNull { it.product.id == productId }?.cartItem?.quantity ?: 0
        val maxStock = state.allProducts.firstOrNull { it.product.id == productId }?.product?.stockQuantity ?: 0
        viewModelScope.launch {
            repository.addToCart(productId, existingQty, maxStock, qty)
                .onSuccess { showMessage(if (state.language == AppLanguage.BANGLA) "কার্টে যোগ হয়েছে" else "Added to cart") }
                .onFailure { showMessage(it.message ?: "Cannot add to cart") }
        }
    }

    fun updateCartQty(cartItemId: Long, newQty: Int, maxStock: Int) {
        val productId = uiState.value.cartItems.firstOrNull { it.cartItem.id == cartItemId }?.product?.id ?: return
        viewModelScope.launch { repository.updateCartQty(productId, newQty, maxStock) }
    }

    fun removeCartItem(cartItemId: Long) {
        val productId = uiState.value.cartItems.firstOrNull { it.cartItem.id == cartItemId }?.product?.id ?: return
        viewModelScope.launch { repository.removeCartItem(productId) }
    }

    fun placeOrder(
        deliveryMethod: DeliveryMethod,
        paymentMethod: PaymentMethod,
        trxId: String,
        senderPhone: String,
        address: String,
        district: String,
        onSuccess: () -> Unit
    ) {
        val state = uiState.value
        val buyer = state.currentUser ?: return
        viewModelScope.launch {
            repository.placeOrdersFromCart(
                buyer = buyer,
                cartItems = state.cartItems,
                settings = state.settings,
                deliveryMethod = deliveryMethod,
                paymentMethod = paymentMethod,
                transactionId = trxId,
                senderAccount = senderPhone,
                deliveryAddress = address,
                district = district
            ).onSuccess { ids ->
                showMessage(if (state.language == AppLanguage.BANGLA) "অর্ডার সম্পন্ন: ${ids.joinToString()}" else "Order placed: ${ids.joinToString()}")
                onSuccess()
            }.onFailure { showMessage(it.message ?: "Order failed") }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus, note: String = "") {
        val state = uiState.value
        val order = (state.buyerOrders + state.sellerOrders + state.allOrders)
            .firstOrNull { it.order.id == orderId }?.order ?: return
        viewModelScope.launch {
            repository.updateOrderStatus(order, status, note)
                .onSuccess { showMessage(if (state.language == AppLanguage.BANGLA) "অর্ডার আপডেট হয়েছে" else "Order status updated") }
                .onFailure { showMessage(it.message ?: "Update failed") }
        }
    }

    fun verifyPayment(orderId: String, paymentStatus: PaymentStatus) {
        viewModelScope.launch {
            repository.verifyOrderPayment(orderId, paymentStatus)
                .onSuccess { showMessage(if (uiState.value.language == AppLanguage.BANGLA) "পেমেন্ট স্ট্যাটাস আপডেট হয়েছে" else "Payment status updated") }
        }
    }

    fun submitReview(productId: String, rating: Int, comment: String) {
        val user = uiState.value.currentUser ?: return
        viewModelScope.launch {
            repository.addReview(productId, user.fullName, rating, comment)
                .onSuccess { showMessage(if (uiState.value.language == AppLanguage.BANGLA) "আপনার রিভিউ যুক্ত হয়েছে" else "Review submitted") }
                .onFailure { showMessage(it.message ?: "Failed") }
        }
    }

    fun saveShop(
        shopNameBn: String,
        shopNameEn: String,
        district: String,
        @Suppress("UNUSED_PARAMETER") phone: String,
        description: String,
        imageUri: String
    ) {
        val state = uiState.value
        viewModelScope.launch {
            repository.saveShop(
                existingShopId = state.sellerShop?.id,
                shopNameBn = shopNameBn,
                shopNameEn = shopNameEn,
                district = district,
                description = description,
                imageUri = imageUri
            ).onSuccess {
                showMessage(if (state.language == AppLanguage.BANGLA) "দোকান সংরক্ষিত — অ্যাডমিন অনুমোদনের অপেক্ষায়" else "Shop saved — pending Admin approval")
            }.onFailure { showMessage(it.message ?: "Failed") }
        }
    }

    fun saveProduct(
        existing: ProductEntity?,
        nameBn: String,
        nameEn: String,
        category: ProductCategory,
        priceBdt: Double,
        unitLabel: String,
        stock: Int,
        description: String,
        imageUri: String,
        customCommission: Double?,
        consentAccepted: Boolean
    ) {
        val state = uiState.value
        val shop = state.sellerShop ?: return
        viewModelScope.launch {
            repository.saveSellerProduct(
                existingProduct = existing,
                shopId = shop.id,
                nameBn = nameBn,
                nameEn = nameEn,
                category = category,
                priceBdt = priceBdt,
                unitLabel = unitLabel,
                stockQuantity = stock,
                description = description,
                imageUri = imageUri,
                customCommissionPercent = customCommission,
                sellerCommissionAccepted = consentAccepted,
                settings = state.settings
            ).onSuccess {
                showMessage(if (state.language == AppLanguage.BANGLA) "পণ্য সংরক্ষণ সম্পন্ন হয়েছে" else "Product saved")
            }.onFailure { showMessage(it.message ?: "Save failed") }
        }
    }

    fun adjustStock(productId: String, newStock: Int) {
        viewModelScope.launch { repository.adjustProductStock(productId, newStock) }
    }

    fun requestWithdrawal(amount: Double, method: PaymentMethod, account: String) {
        val state = uiState.value
        val shop = state.sellerShop ?: return
        viewModelScope.launch {
            repository.requestWithdrawal(shop, amount, method, account, state.sellerFinance.availableBalance)
                .onSuccess { showMessage(if (state.language == AppLanguage.BANGLA) "টাকা উত্তোলনের আবেদন পাঠানো হয়েছে" else "Withdrawal request submitted") }
                .onFailure { showMessage(it.message ?: "Request failed") }
        }
    }

    fun approveShop(shopId: String, status: ApprovalStatus) {
        viewModelScope.launch { repository.approveShop(shopId, status) }
    }

    fun approveProduct(productId: String, status: ApprovalStatus) {
        viewModelScope.launch { repository.approveProduct(productId, status) }
    }

    fun updateProductCommission(product: ProductEntity, rate: Double?) {
        viewModelScope.launch { repository.updateProductCommissionByAdmin(product.id, rate, uiState.value.settings) }
    }

    fun toggleFeatured(productId: String, featured: Boolean) {
        viewModelScope.launch { repository.toggleProductFeatured(productId, featured) }
    }

    fun setUserBlocked(userId: String, blocked: Boolean) {
        viewModelScope.launch { repository.setUserBlocked(userId, blocked) }
    }

    fun resolveWithdrawal(w: WithdrawalEntity, status: WithdrawalStatus, note: String) {
        viewModelScope.launch { repository.resolveWithdrawal(w.id, status, note) }
    }

    fun withdrawAdminCommission(amount: Double, method: PaymentMethod, account: String, note: String) {
        val state = uiState.value
        val admin = state.currentUser ?: return
        viewModelScope.launch {
            repository.withdrawAdminCommission(
                adminUser = admin,
                amountBdt = amount,
                payoutMethod = method,
                payoutAccount = account,
                note = note,
                availableBalance = state.adminReport.adminAvailableCommissionBdt
            ).onSuccess {
                showMessage(
                    if (state.language == AppLanguage.BANGLA) {
                        "অ্যাডমিন কমিশন ৳${amount.toInt()} উত্তোলন সম্পন্ন হয়েছে"
                    } else {
                        "Admin commission ৳${amount.toInt()} withdrawn successfully"
                    }
                )
            }.onFailure {
                showMessage(it.message ?: "Withdrawal failed")
            }
        }
    }

    fun saveSettings(settings: PlatformSettingsEntity) {
        viewModelScope.launch {
            repository.savePlatformSettings(settings)
                .onSuccess { showMessage(if (uiState.value.language == AppLanguage.BANGLA) "সেটিংস সংরক্ষণ হয়েছে" else "Settings saved") }
                .onFailure { showMessage(it.message ?: "Invalid settings") }
        }
    }

    fun generateBackup() {
        val state = uiState.value
        val json = repository.createBackupSnapshot(
            users = state.allUsers,
            shops = state.allShops,
            products = state.allProducts.map { it.product },
            orders = state.allOrders.map { it.order },
            settings = state.settings
        )
        _lastBackupJson.value = json
        showMessage(if (state.language == AppLanguage.BANGLA) "ব্যাকআপ তৈরি সম্পন্ন" else "Backup snapshot created")
    }

    private fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    companion object {
        private const val TAG = "CloudKrishiVM"

        fun provideFactory(
            repository: FirestoreKrishiRepository,
            firebaseUser: FirebaseUser
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CloudKrishiViewModel(repository, firebaseUser) as T
            }
        }
    }
}
