package com.example.data

import androidx.room.withTransaction
import com.example.model.ApprovalStatus
import com.example.model.CartItemEntity
import com.example.model.CommissionCalculator
import com.example.model.CommissionStatus
import com.example.model.DeliveryMethod
import com.example.model.OrderEntity
import com.example.model.OrderItemEntity
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.PlatformSettingsEntity
import com.example.model.ProductCategory
import com.example.model.ProductEntity
import com.example.model.ReviewEntity
import com.example.model.ShopEntity
import com.example.model.UserEntity
import com.example.model.UserRole
import com.example.model.WithdrawalEntity
import com.example.model.WithdrawalStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class KrishiRepository(
    private val db: KrishiDatabase,
    private val prefs: AppPreferences
) {
    private val dao = db.krishiDao()

    val languageFlow = prefs.languageFlow
    val activeUserIdFlow = prefs.activeUserIdFlow
    val allUsersFlow: Flow<List<UserEntity>> = dao.observeAllUsers()
    val allShopsFlow: Flow<List<ShopEntity>> = dao.observeAllShops()
    val allProductsFlow: Flow<List<ProductEntity>> = dao.observeAllProducts()
    val allOrdersFlow: Flow<List<OrderEntity>> = dao.observeAllOrders()
    val allOrderItemsFlow: Flow<List<OrderItemEntity>> = dao.observeAllOrderItems()
    val allReviewsFlow: Flow<List<ReviewEntity>> = dao.observeAllReviews()
    val allWithdrawalsFlow: Flow<List<WithdrawalEntity>> = dao.observeAllWithdrawals()
    val settingsFlow: Flow<PlatformSettingsEntity> = dao.observeSettings().map { it ?: PlatformSettingsEntity() }

    suspend fun initializeDatabaseAndSession() {
        KrishiSeedData.ensureSeeded(dao)
        if (!prefs.isInitialSessionReady()) {
            prefs.setActiveUserId("user_buyer_rakib")
            prefs.markInitialSessionReady()
        }
    }

    fun observeCart(buyerUserId: String): Flow<List<CartItemEntity>> = dao.observeCartItems(buyerUserId)

    fun observeUser(userId: String): Flow<UserEntity?> = dao.observeUserById(userId)

    suspend fun setLanguage(language: com.example.model.AppLanguage) {
        prefs.setLanguage(language)
    }

    suspend fun login(identifier: String, password: String): Result<UserEntity> {
        val clean = identifier.trim()
        if (clean.isEmpty() || password.isEmpty()) {
            return Result.failure(IllegalArgumentException("Email/Phone and password are required"))
        }
        val user = dao.findUserByEmailOrPhone(clean)
            ?: return Result.failure(IllegalArgumentException("Account not found"))
        if (user.isBlocked) {
            return Result.failure(IllegalStateException("This account has been blocked by Main Admin"))
        }
        if (user.passwordHash != PasswordHasher.hash(password)) {
            return Result.failure(IllegalArgumentException("Incorrect password"))
        }
        prefs.setActiveUserId(user.id)
        return Result.success(user)
    }

    suspend fun quickDemoLogin(userId: String): Result<UserEntity> {
        val user = dao.getUserById(userId)
            ?: return Result.failure(IllegalStateException("Demo user unavailable"))
        if (user.isBlocked) {
            return Result.failure(IllegalStateException("Account is blocked by Main Admin"))
        }
        prefs.setActiveUserId(user.id)
        return Result.success(user)
    }

    suspend fun registerUser(
        fullName: String,
        phone: String,
        email: String,
        password: String,
        role: UserRole,
        district: String,
        address: String
    ): Result<UserEntity> {
        if (fullName.isBlank() || phone.isBlank() || email.isBlank() || password.length < 4) {
            return Result.failure(IllegalArgumentException("Please fill all fields (password min 4 chars)"))
        }
        if (role == UserRole.MAIN_ADMIN) {
            return Result.failure(SecurityException("Cannot self-register Main Admin"))
        }
        if (dao.findUserByEmailOrPhone(email.trim()) != null || dao.findUserByEmailOrPhone(phone.trim()) != null) {
            return Result.failure(IllegalStateException("Phone or email already registered"))
        }
        val newUser = UserEntity(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            fullName = fullName.trim(),
            phone = phone.trim(),
            email = email.trim(),
            passwordHash = PasswordHasher.hash(password),
            role = role,
            defaultAddress = address.trim(),
            district = district.trim().ifBlank { "Dhaka" }
        )
        dao.insertUser(newUser)
        prefs.setActiveUserId(newUser.id)
        return Result.success(newUser)
    }

    suspend fun logout() {
        prefs.setActiveUserId(null)
    }

    suspend fun updateUserAddress(userId: String, address: String, district: String) {
        dao.updateUserAddress(userId, address.trim(), district.trim())
    }

    suspend fun updateAdminPassword(userId: String, newPassword: String): Result<Unit> {
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }
        val user = dao.getUserById(userId)
            ?: return Result.failure(IllegalStateException("User not found"))
        if (user.role != UserRole.MAIN_ADMIN) {
            return Result.failure(SecurityException("Only Main Admin can update admin password"))
        }
        dao.insertUser(user.copy(passwordHash = PasswordHasher.hash(newPassword)))
        return Result.success(Unit)
    }

    suspend fun addToCart(buyerUserId: String, productId: String, quantityDelta: Int = 1): Result<Unit> {
        val product = dao.getProductById(productId)
            ?: return Result.failure(IllegalArgumentException("Product not found"))
        if (product.stockQuantity <= 0) {
            return Result.failure(IllegalStateException("Product is out of stock"))
        }
        val existing = dao.getCartItem(buyerUserId, productId)
        val newQty = ((existing?.quantity ?: 0) + quantityDelta).coerceIn(1, product.stockQuantity)
        if (existing == null) {
            dao.insertCartItem(CartItemEntity(buyerUserId = buyerUserId, productId = productId, quantity = newQty))
        } else {
            dao.updateCartQuantity(existing.id, newQty)
        }
        return Result.success(Unit)
    }

    suspend fun updateCartItemQty(cartItemId: Long, newQty: Int, maxStock: Int) {
        if (newQty <= 0) {
            dao.deleteCartItem(cartItemId)
        } else {
            dao.updateCartQuantity(cartItemId, newQty.coerceAtMost(maxStock.coerceAtLeast(1)))
        }
    }

    suspend fun removeCartItem(cartItemId: Long) {
        dao.deleteCartItem(cartItemId)
    }

    /**
     * Places separate vendor orders per shop cleanly inside a single atomic Room transaction,
     * decrements inventory, computes agreed commission per item, and clears buyer cart.
     */
    suspend fun placeOrdersFromCart(
        buyer: UserEntity,
        cartItems: List<CartItemEntity>,
        deliveryMethod: DeliveryMethod,
        paymentMethod: PaymentMethod,
        transactionId: String,
        senderAccount: String,
        deliveryAddress: String,
        district: String
    ): Result<List<String>> {
        if (buyer.isBlocked) return Result.failure(IllegalStateException("Blocked accounts cannot place orders"))
        if (cartItems.isEmpty()) return Result.failure(IllegalArgumentException("Cart is empty"))
        if (deliveryAddress.isBlank()) return Result.failure(IllegalArgumentException("Delivery address is required"))
        if (paymentMethod.requiresTrxId && transactionId.trim().length < 5) {
            return Result.failure(IllegalArgumentException("Valid TrxID is required for ${paymentMethod.titleEn}"))
        }

        val settings = dao.getSettings() ?: PlatformSettingsEntity()
        val deliveryFeePerShop = when (deliveryMethod) {
            DeliveryMethod.SELLER_DELIVERY -> settings.sellerDeliveryChargeBdt
            DeliveryMethod.COURIER_DELIVERY -> settings.courierDeliveryChargeBdt
        }

        return try {
            val createdOrderIds = db.withTransaction {
                val resolvedProducts = cartItems.mapNotNull { cart ->
                    val prod = dao.getProductById(cart.productId) ?: return@mapNotNull null
                    if (prod.stockQuantity < cart.quantity) {
                        throw IllegalStateException("Insufficient stock for ${prod.nameEn}")
                    }
                    cart to prod
                }
                if (resolvedProducts.isEmpty()) {
                    throw IllegalStateException("No valid products in cart")
                }

                val groupedByShop = resolvedProducts.groupBy { it.second.shopId }
                val orderIds = mutableListOf<String>()

                groupedByShop.forEach { (shopId, itemsForShop) ->
                    val sellerId = itemsForShop.first().second.sellerUserId
                    val orderId = "KB-${(1000..9999).random()}-${UUID.randomUUID().toString().take(3).uppercase()}"
                    var subtotal = 0.0
                    var commissionTotal = 0.0

                    val orderItems = itemsForShop.map { (cart, product) ->
                        val lineSub = product.priceBdt * cart.quantity
                        val rate = CommissionCalculator.effectiveRate(product.commissionPercentOverride, settings)
                        val lineComm = CommissionCalculator.calculateLineCommission(lineSub, rate)
                        subtotal += lineSub
                        commissionTotal += lineComm

                        // Deduct stock atomically
                        dao.updateProductStock(product.id, product.stockQuantity - cart.quantity)

                        OrderItemEntity(
                            orderId = orderId,
                            productId = product.id,
                            productNameBn = product.nameBn,
                            productNameEn = product.nameEn,
                            unitPrice = product.priceBdt,
                            quantity = cart.quantity,
                            appliedCommissionPercent = rate,
                            lineSubtotal = lineSub,
                            lineCommissionAmount = lineComm
                        )
                    }

                    val order = OrderEntity(
                        id = orderId,
                        buyerUserId = buyer.id,
                        buyerName = buyer.fullName,
                        buyerPhone = buyer.phone,
                        deliveryAddress = deliveryAddress.trim(),
                        district = district.trim().ifBlank { buyer.district },
                        shopId = shopId,
                        sellerUserId = sellerId,
                        deliveryMethod = deliveryMethod,
                        deliveryCharge = deliveryFeePerShop,
                        paymentMethod = paymentMethod,
                        paymentStatus = PaymentStatus.PENDING_VERIFICATION,
                        transactionId = transactionId.trim(),
                        senderAccount = senderAccount.trim(),
                        orderStatus = OrderStatus.PLACED,
                        grossSubtotal = subtotal,
                        totalPayable = subtotal + deliveryFeePerShop,
                        agreedCommissionTotal = commissionTotal,
                        realizedCommissionAmount = 0.0,
                        sellerNetPayout = 0.0,
                        commissionStatus = CommissionStatus.PENDING,
                        trackingNote = "অর্ডার সফলভাবে গৃহীত হয়েছে"
                    )
                    dao.insertOrder(order)
                    dao.insertOrderItems(orderItems)
                    orderIds.add(orderId)
                }

                dao.updateUserAddress(buyer.id, deliveryAddress.trim(), district.trim().ifBlank { buyer.district })
                dao.clearCart(buyer.id)
                orderIds
            }
            Result.success(createdOrderIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus, note: String = ""): Result<Unit> {
        val current = dao.getOrderById(orderId) ?: return Result.failure(IllegalArgumentException("Order not found"))
        val resolved = CommissionCalculator.resolveOrderSettlement(current, newStatus)
            .let { if (note.isNotBlank()) it.copy(trackingNote = note) else it }
        dao.updateOrder(resolved)
        return Result.success(Unit)
    }

    suspend fun verifyOrderPayment(orderId: String, status: PaymentStatus): Result<Unit> {
        val current = dao.getOrderById(orderId) ?: return Result.failure(IllegalArgumentException("Order not found"))
        val updated = if (status == PaymentStatus.REFUNDED) {
            CommissionCalculator.resolveOrderSettlement(current, OrderStatus.REFUNDED)
                .copy(trackingNote = "অ্যাডমিন পেমেন্ট রিফান্ড করেছেন এবং কমিশন সমন্বয় হয়েছে")
        } else {
            current.copy(paymentStatus = status, updatedAt = System.currentTimeMillis())
        }
        dao.updateOrder(updated)
        return Result.success(Unit)
    }

    suspend fun addReview(
        productId: String,
        buyerUserId: String,
        buyerName: String,
        rating: Int,
        comment: String
    ): Result<Unit> {
        if (comment.isBlank()) return Result.failure(IllegalArgumentException("Please enter a review comment"))
        dao.insertReview(
            ReviewEntity(
                productId = productId,
                buyerUserId = buyerUserId,
                buyerName = buyerName,
                rating = rating.coerceIn(1, 5),
                comment = comment.trim()
            )
        )
        return Result.success(Unit)
    }

    // Seller Operations
    suspend fun registerOrUpdateShop(
        ownerUserId: String,
        existingShopId: String?,
        shopNameBn: String,
        shopNameEn: String,
        district: String,
        phone: String,
        description: String,
        imageUri: String
    ): Result<ShopEntity> {
        if (shopNameBn.isBlank() || phone.isBlank()) {
            return Result.failure(IllegalArgumentException("Shop name and phone are required"))
        }
        val shop = ShopEntity(
            id = existingShopId ?: "shop_${UUID.randomUUID().toString().take(8)}",
            ownerUserId = ownerUserId,
            shopNameBn = shopNameBn.trim(),
            shopNameEn = shopNameEn.trim().ifBlank { shopNameBn.trim() },
            district = district.trim().ifBlank { "Dhaka" },
            phone = phone.trim(),
            description = description.trim(),
            imageUri = imageUri.ifBlank { "drawable://img_cat_vegetables" },
            approvalStatus = ApprovalStatus.PENDING,
            sellerCommissionAccepted = true
        )
        dao.insertShop(shop)
        return Result.success(shop)
    }

    suspend fun saveSellerProduct(
        existingProduct: ProductEntity?,
        shopId: String,
        sellerUserId: String,
        nameBn: String,
        nameEn: String,
        category: ProductCategory,
        priceBdt: Double,
        unitLabel: String,
        stockQuantity: Int,
        description: String,
        imageUri: String,
        customCommissionPercent: Double?,
        sellerCommissionAccepted: Boolean
    ): Result<Unit> {
        if (nameBn.isBlank() || priceBdt <= 0.0 || stockQuantity < 0) {
            return Result.failure(IllegalArgumentException("Valid product name, price, and stock are required"))
        }
        if (!sellerCommissionAccepted) {
            return Result.failure(IllegalStateException("Seller must accept the commission rate"))
        }
        val settings = dao.getSettings() ?: PlatformSettingsEntity()
        val boundedCommission = customCommissionPercent?.coerceIn(
            settings.minCommissionPercent,
            settings.maxCommissionPercent
        )
        val product = ProductEntity(
            id = existingProduct?.id ?: "prod_${UUID.randomUUID().toString().take(8)}",
            shopId = shopId,
            sellerUserId = sellerUserId,
            nameBn = nameBn.trim(),
            nameEn = nameEn.trim().ifBlank { nameBn.trim() },
            category = category,
            priceBdt = priceBdt,
            unitLabel = unitLabel.trim().ifBlank { "১ কেজি / 1 kg" },
            stockQuantity = stockQuantity,
            description = description.trim(),
            imageUri = imageUri.ifBlank { category.defaultDrawableKey },
            commissionPercentOverride = boundedCommission,
            sellerCommissionAccepted = true,
            approvalStatus = if (existingProduct?.approvalStatus == ApprovalStatus.APPROVED) {
                ApprovalStatus.APPROVED
            } else {
                ApprovalStatus.PENDING
            },
            featured = existingProduct?.featured ?: false,
            createdAt = existingProduct?.createdAt ?: System.currentTimeMillis()
        )
        dao.insertProduct(product)
        return Result.success(Unit)
    }

    suspend fun adjustProductStock(productId: String, newStock: Int) {
        dao.updateProductStock(productId, newStock.coerceAtLeast(0))
    }

    suspend fun requestWithdrawal(
        sellerUserId: String,
        shop: ShopEntity,
        amountBdt: Double,
        payoutMethod: PaymentMethod,
        payoutAccount: String,
        availableBalance: Double
    ): Result<Unit> {
        if (amountBdt <= 0.0) return Result.failure(IllegalArgumentException("Enter a valid amount"))
        if (amountBdt > availableBalance + 0.01) {
            return Result.failure(IllegalStateException("Insufficient available balance (৳${availableBalance.toInt()})"))
        }
        if (payoutAccount.isBlank() || payoutMethod == PaymentMethod.COD) {
            return Result.failure(IllegalArgumentException("Select bKash, Nagad, or Rocket account"))
        }
        dao.insertWithdrawal(
            WithdrawalEntity(
                id = "WD-${(100..999).random()}-${UUID.randomUUID().toString().take(3).uppercase()}",
                sellerUserId = sellerUserId,
                shopId = shop.id,
                shopName = shop.shopNameBn,
                amountBdt = amountBdt,
                payoutMethod = payoutMethod,
                payoutAccount = payoutAccount.trim(),
                status = WithdrawalStatus.PENDING
            )
        )
        return Result.success(Unit)
    }

    // Admin Operations
    suspend fun approveShop(shopId: String, status: ApprovalStatus) {
        dao.updateShopApproval(shopId, status)
    }

    suspend fun approveProduct(productId: String, status: ApprovalStatus) {
        dao.updateProductApproval(productId, status)
    }

    suspend fun updateProductCommissionByAdmin(product: ProductEntity, newRate: Double?) {
        val settings = dao.getSettings() ?: PlatformSettingsEntity()
        val bounded = newRate?.coerceIn(settings.minCommissionPercent, settings.maxCommissionPercent)
        dao.updateProduct(product.copy(commissionPercentOverride = bounded))
    }

    suspend fun toggleProductFeatured(productId: String, featured: Boolean) {
        dao.updateProductFeatured(productId, featured)
    }

    suspend fun setUserBlocked(userId: String, blocked: Boolean) {
        dao.setUserBlocked(userId, blocked)
    }

    suspend fun resolveWithdrawal(withdrawal: WithdrawalEntity, status: WithdrawalStatus, note: String) {
        dao.updateWithdrawal(
            withdrawal.copy(
                status = status,
                adminNote = note.trim(),
                resolvedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun withdrawAdminCommission(
        adminUser: UserEntity,
        amountBdt: Double,
        payoutMethod: PaymentMethod,
        payoutAccount: String,
        note: String,
        availableBalance: Double
    ): Result<Unit> {
        if (adminUser.role != UserRole.MAIN_ADMIN) {
            return Result.failure(SecurityException("Only Main Admin can withdraw platform commission"))
        }
        if (amountBdt <= 0.0) {
            return Result.failure(IllegalArgumentException("Enter a valid withdrawal amount"))
        }
        if (amountBdt > availableBalance + 0.01) {
            return Result.failure(IllegalStateException("Insufficient platform commission balance (৳${availableBalance.toInt()})"))
        }
        if (payoutAccount.trim().length < 6 || payoutMethod == PaymentMethod.COD) {
            return Result.failure(IllegalArgumentException("Select bKash, Nagad, or Rocket and enter a valid account number"))
        }
        val now = System.currentTimeMillis()
        dao.insertWithdrawal(
            WithdrawalEntity(
                id = "AWD-${(100..999).random()}-${UUID.randomUUID().toString().take(3).uppercase()}",
                sellerUserId = adminUser.id,
                shopId = "PLATFORM_ADMIN",
                shopName = "প্রধান অ্যাডমিন কমিশন",
                amountBdt = amountBdt,
                payoutMethod = payoutMethod,
                payoutAccount = payoutAccount.trim(),
                status = WithdrawalStatus.APPROVED,
                adminNote = note.trim().ifBlank { "Owner Commission Payout" },
                requestedAt = now,
                resolvedAt = now
            )
        )
        return Result.success(Unit)
    }

    suspend fun savePlatformSettings(settings: PlatformSettingsEntity): Result<Unit> {
        if (settings.minCommissionPercent < 0 || settings.maxCommissionPercent > 100 ||
            settings.minCommissionPercent > settings.maxCommissionPercent ||
            settings.defaultCommissionPercent !in settings.minCommissionPercent..settings.maxCommissionPercent
        ) {
            return Result.failure(IllegalArgumentException("Invalid commission bounds (Min <= Default <= Max, 0..100)"))
        }
        dao.saveSettings(settings)
        return Result.success(Unit)
    }

    suspend fun createBackupSnapshot(
        users: List<UserEntity>,
        shops: List<ShopEntity>,
        products: List<ProductEntity>,
        orders: List<OrderEntity>,
        settings: PlatformSettingsEntity
    ): String {
        val now = System.currentTimeMillis()
        dao.saveSettings(settings.copy(lastBackupAt = now))
        val root = JSONObject().apply {
            put("app", "Krishi Bazar BD")
            put("exportedAt", now)
            put("defaultCommissionPercent", settings.defaultCommissionPercent)
            put("counts", JSONObject().apply {
                put("users", users.size)
                put("shops", shops.size)
                put("products", products.size)
                put("orders", orders.size)
            })
            put("completedOrders", JSONArray(orders.filter { it.orderStatus == OrderStatus.COMPLETED }.map {
                JSONObject().apply {
                    put("id", it.id)
                    put("grossSubtotal", it.grossSubtotal)
                    put("realizedCommission", it.realizedCommissionAmount)
                    put("sellerNetPayout", it.sellerNetPayout)
                }
            }))
        }
        return root.toString(2)
    }
}
