package com.example.data

import com.example.firebase.FsCartItem
import com.example.firebase.FsOrder
import com.example.firebase.FsOrderItem
import com.example.firebase.FsPlatformSettings
import com.example.firebase.FsProduct
import com.example.firebase.FsReview
import com.example.firebase.FsShop
import com.example.firebase.FsUserProfile
import com.example.firebase.FsWithdrawal
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.firebase.serverTs
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class FirestoreKrishiRepository(
    private val db: FirebaseFirestore,
    private val prefs: AppPreferences,
    private val auth: FirebaseAuth = Firebase.auth
) {
    companion object {
        const val OWNER_ADMIN_EMAIL = "raysk048@gmail.com"
        const val PLATFORM_ADMIN_SHOP_ID = "PLATFORM_ADMIN"
    }

    val languageFlow = prefs.languageFlow

    suspend fun setLanguage(language: com.example.model.AppLanguage) {
        prefs.setLanguage(language)
    }

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    fun isOwnerAdminUser(firebaseUser: FirebaseUser?): Boolean {
        return firebaseUser != null &&
            firebaseUser.email.equals(OWNER_ADMIN_EMAIL, ignoreCase = true) &&
            firebaseUser.isEmailVerified
    }

    // ---- Real-time Observations (1:1 aligned with firestore.rules) ----

    fun observeUserProfile(uid: String): Flow<UserEntity?> {
        require(uid.isNotBlank())
        return db.collection("users").document(uid)
            .snapshots()
            .map { snap -> snap.toObject(FsUserProfile::class.java, ServerTimestampBehavior.ESTIMATE)?.toDomain() }
    }

    fun observeAllUsersAdmin(): Flow<List<UserEntity>> =
        db.collection("users")
            .snapshots()
            .map { snap -> snap.toObjects(FsUserProfile::class.java, ServerTimestampBehavior.ESTIMATE).map { it.toDomain() } }

    fun observeApprovedShops(): Flow<List<ShopEntity>> =
        db.collection("shops")
            .whereEqualTo("approvalStatus", ApprovalStatus.APPROVED.name)
            .snapshots()
            .map { snap -> snap.toObjects(FsShop::class.java, ServerTimestampBehavior.ESTIMATE).map { it.toDomain() } }

    fun observeSellerShops(uid: String): Flow<List<ShopEntity>> =
        db.collection("shops")
            .whereEqualTo("ownerUserId", uid)
            .snapshots()
            .map { snap -> snap.toObjects(FsShop::class.java, ServerTimestampBehavior.ESTIMATE).map { it.toDomain() } }

    fun observeAllShopsAdmin(): Flow<List<ShopEntity>> =
        db.collection("shops")
            .snapshots()
            .map { snap -> snap.toObjects(FsShop::class.java, ServerTimestampBehavior.ESTIMATE).map { it.toDomain() } }

    fun observeApprovedProducts(): Flow<List<ProductEntity>> =
        db.collection("products")
            .whereEqualTo("approvalStatus", ApprovalStatus.APPROVED.name)
            .whereEqualTo("sellerCommissionAccepted", true)
            .snapshots()
            .map { snap -> snap.toObjects(FsProduct::class.java, ServerTimestampBehavior.ESTIMATE).map { it.toDomain() } }

    fun observeSellerProducts(uid: String): Flow<List<ProductEntity>> =
        db.collection("products")
            .whereEqualTo("sellerUserId", uid)
            .snapshots()
            .map { snap -> snap.toObjects(FsProduct::class.java, ServerTimestampBehavior.ESTIMATE).map { it.toDomain() } }

    fun observeAllProductsAdmin(): Flow<List<ProductEntity>> =
        db.collection("products")
            .snapshots()
            .map { snap -> snap.toObjects(FsProduct::class.java, ServerTimestampBehavior.ESTIMATE).map { it.toDomain() } }

    fun observeCart(uid: String): Flow<List<CartItemEntity>> =
        db.collection("users").document(uid).collection("cart")
            .snapshots()
            .map { snap -> snap.toObjects(FsCartItem::class.java, ServerTimestampBehavior.ESTIMATE).map { it.toDomain() } }

    fun observeBuyerOrders(uid: String): Flow<List<Pair<OrderEntity, List<OrderItemEntity>>>> =
        db.collection("orders")
            .whereEqualTo("buyerUserId", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(FsOrder::class.java, ServerTimestampBehavior.ESTIMATE)
                    .map { it.toDomainOrder() to it.toDomainItems() }
                    .sortedByDescending { it.first.createdAt }
            }

    fun observeSellerOrders(uid: String): Flow<List<Pair<OrderEntity, List<OrderItemEntity>>>> =
        db.collection("orders")
            .whereEqualTo("sellerUserId", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(FsOrder::class.java, ServerTimestampBehavior.ESTIMATE)
                    .map { it.toDomainOrder() to it.toDomainItems() }
                    .sortedByDescending { it.first.createdAt }
            }

    fun observeAllOrdersAdmin(): Flow<List<Pair<OrderEntity, List<OrderItemEntity>>>> =
        db.collection("orders")
            .snapshots()
            .map { snap ->
                snap.toObjects(FsOrder::class.java, ServerTimestampBehavior.ESTIMATE)
                    .map { it.toDomainOrder() to it.toDomainItems() }
                    .sortedByDescending { it.first.createdAt }
            }

    fun observePublishedReviews(): Flow<List<ReviewEntity>> =
        db.collection("reviews")
            .whereEqualTo("published", true)
            .snapshots()
            .map { snap -> snap.toObjects(FsReview::class.java, ServerTimestampBehavior.ESTIMATE).map { it.toDomain() } }

    fun observeSellerWithdrawals(uid: String): Flow<List<WithdrawalEntity>> =
        db.collection("withdrawals")
            .whereEqualTo("sellerUserId", uid)
            .snapshots()
            .map { snap -> snap.toObjects(FsWithdrawal::class.java, ServerTimestampBehavior.ESTIMATE).map { it.toDomain() } }

    fun observeAllWithdrawalsAdmin(): Flow<List<WithdrawalEntity>> =
        db.collection("withdrawals")
            .snapshots()
            .map { snap -> snap.toObjects(FsWithdrawal::class.java, ServerTimestampBehavior.ESTIMATE).map { it.toDomain() } }

    fun observeSettings(): Flow<PlatformSettingsEntity> =
        db.collection("platform_settings").document("global")
            .snapshots()
            .map { snap ->
                snap.toObject(FsPlatformSettings::class.java, ServerTimestampBehavior.ESTIMATE)?.toDomain()
                    ?: PlatformSettingsEntity()
            }

    // ---- Authenticated Mutations (all return Result<T> and log structured JSON errors) ----

    suspend fun ensureUserProfile(
        firebaseUser: FirebaseUser,
        preferredRole: UserRole = UserRole.BUYER
    ): Result<Unit> {
        val uid = firebaseUser.uid
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            val isOwner = isOwnerAdminUser(firebaseUser)
            if (!snap.exists()) {
                val safeRole = if (isOwner) UserRole.MAIN_ADMIN else if (preferredRole == UserRole.MAIN_ADMIN) UserRole.BUYER else preferredRole
                val payload = mapOf(
                    "userId" to uid,
                    "fullName" to (firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: if (isOwner) "প্রধান অ্যাডমিন" else "কৃষি বাজার ইউজার").take(120),
                    "phone" to (firebaseUser.phoneNumber ?: "").take(30),
                    "email" to (firebaseUser.email ?: "").take(150),
                    "role" to safeRole.name,
                    "blocked" to false,
                    "defaultAddress" to "ঢাকা, বাংলাদেশ",
                    "district" to "Dhaka",
                    "createdAt" to serverTs(),
                    "updatedAt" to serverTs()
                )
                docRef.set(payload).await()
            } else if (isOwner && snap.getString("role") != UserRole.MAIN_ADMIN.name) {
                docRef.update(
                    mapOf(
                        "role" to UserRole.MAIN_ADMIN.name,
                        "updatedAt" to serverTs()
                    )
                ).await()
            }

            if (isOwner) {
                seedInitialCloudCatalogIfMissing(uid)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun seedInitialCloudCatalogIfMissing(adminUid: String) {
        val settingsRef = db.collection("platform_settings").document("global")
        val settingsSnap = settingsRef.get().await()
        if (!settingsSnap.exists()) {
            settingsRef.set(
                mapOf(
                    "defaultCommissionPercent" to 50.0,
                    "minCommissionPercent" to 5.0,
                    "maxCommissionPercent" to 60.0,
                    "sellerDeliveryChargeBdt" to 60.0,
                    "courierDeliveryChargeBdt" to 110.0,
                    "siteBannerTitleBn" to "সরাসরি কৃষকের মাঠ থেকে আপনার ঘরে",
                    "siteBannerTitleEn" to "Fresh From Bangladeshi Farms to Your Home",
                    "maintenanceNotice" to "",
                    "updatedAt" to serverTs()
                )
            ).await()
        }

        val shopsSnap = db.collection("shops").limit(1).get().await()
        if (shopsSnap.isEmpty) {
            val batch = db.batch()
            val shop1Ref = db.collection("shops").document("shop_sobuj_bangla")
            batch.set(
                shop1Ref,
                mapOf(
                    "ownerUserId" to adminUid,
                    "shopNameBn" to "সবুজ বাংলা খামার",
                    "shopNameEn" to "Sobuj Bangla Farm",
                    "district" to "Bogura",
                    "description" to "বগুড়ার তাজা শাকসবজি, মিনিকেট চাল ও জৈব সার সরাসরি খামার থেকে।",
                    "imageUri" to "drawable://img_cat_vegetables",
                    "approvalStatus" to ApprovalStatus.APPROVED.name,
                    "sellerCommissionAccepted" to true,
                    "createdAt" to serverTs(),
                    "updatedAt" to serverTs()
                )
            )

            val starterProducts = listOf(
                Triple("prod_1", "তাজা লাল শাক ও পালং শাক বান্ডিল", "Fresh Red & Spinach Bundle") to Pair(ProductCategory.VEGETABLES, 90.0),
                Triple("prod_2", "প্রিমিয়াম বগুড়ার মিনিকেট চাল", "Premium Bogura Miniket Rice") to Pair(ProductCategory.RICE, 380.0),
                Triple("prod_3", "দেশি মসুর ডাল (খোসা ছাড়া)", "Deshi Red Masoor Lentils") to Pair(ProductCategory.LENTILS, 145.0),
                Triple("prod_4", "রাজশাহীর হিমসাগর আম", "Rajshahi Himsagar Mango") to Pair(ProductCategory.FRUITS, 450.0),
                Triple("prod_5", "পদ্মার তাজা রুই মাছ", "Fresh Padma Rui Fish") to Pair(ProductCategory.FISH, 620.0)
            )
            starterProducts.forEach { (idPair, catPrice) ->
                val (pid, bn, en) = idPair
                val (cat, price) = catPrice
                batch.set(
                    db.collection("products").document(pid),
                    mapOf(
                        "shopId" to "shop_sobuj_bangla",
                        "sellerUserId" to adminUid,
                        "nameBn" to bn,
                        "nameEn" to en,
                        "category" to cat.name,
                        "priceBdt" to price,
                        "unitLabel" to "১ কেজি / 1 kg",
                        "stockQuantity" to 30,
                        "description" to "$bn — ১০০% খাঁটি ও তাজা কৃষিপণ্য।",
                        "imageUri" to cat.defaultDrawableKey,
                        "sellerCommissionAccepted" to true,
                        "approvalStatus" to ApprovalStatus.APPROVED.name,
                        "featured" to true,
                        "createdAt" to serverTs(),
                        "updatedAt" to serverTs()
                    )
                )
            }
            batch.commit().await()
        }
    }

    suspend fun switchBuyerSellerRole(targetRole: UserRole): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (targetRole == UserRole.MAIN_ADMIN) {
            return Result.failure(SecurityException("Cannot self-assign Main Admin"))
        }
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid)
                .update(mapOf("role" to targetRole.name, "updatedAt" to serverTs()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserAddress(fullName: String?, phone: String?, address: String, district: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val updates = mutableMapOf<String, Any>(
                "defaultAddress" to address.trim().take(300),
                "district" to district.trim().ifBlank { "Dhaka" }.take(80),
                "updatedAt" to serverTs()
            )
            if (!fullName.isNullOrBlank()) updates["fullName"] = fullName.trim().take(120)
            if (!phone.isNullOrBlank()) updates["phone"] = phone.trim().take(30)
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addToCart(productId: String, currentQty: Int, maxStock: Int, delta: Int = 1): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (maxStock <= 0) return Result.failure(IllegalStateException("Product is out of stock"))
        val path = "users/$uid/cart/$productId"
        return try {
            val docRef = db.collection("users").document(uid).collection("cart").document(productId)
            val newQty = (currentQty + delta).coerceIn(1, maxStock)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "buyerUserId" to uid,
                        "productId" to productId,
                        "quantity" to newQty,
                        "createdAt" to serverTs(),
                        "updatedAt" to serverTs()
                    )
                ).await()
            } else {
                docRef.update(mapOf("quantity" to newQty, "updatedAt" to serverTs())).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCartQty(productId: String, newQty: Int, maxStock: Int): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val path = "users/$uid/cart/$productId"
        return try {
            val docRef = db.collection("users").document(uid).collection("cart").document(productId)
            if (newQty <= 0) {
                docRef.delete().await()
            } else {
                docRef.update(
                    mapOf(
                        "quantity" to newQty.coerceIn(1, maxStock.coerceAtLeast(1)),
                        "updatedAt" to serverTs()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeCartItem(productId: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val path = "users/$uid/cart/$productId"
        return try {
            db.collection("users").document(uid).collection("cart").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun placeOrdersFromCart(
        buyer: UserEntity,
        cartItems: List<com.example.model.CartItemDetailed>,
        settings: PlatformSettingsEntity,
        deliveryMethod: DeliveryMethod,
        paymentMethod: PaymentMethod,
        transactionId: String,
        senderAccount: String,
        deliveryAddress: String,
        district: String
    ): Result<List<String>> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (buyer.isBlocked) return Result.failure(IllegalStateException("Blocked accounts cannot place orders"))
        if (cartItems.isEmpty()) return Result.failure(IllegalArgumentException("Cart is empty"))
        if (deliveryAddress.isBlank()) return Result.failure(IllegalArgumentException("Delivery address is required"))
        if (paymentMethod.requiresTrxId && transactionId.trim().length < 5) {
            return Result.failure(IllegalArgumentException("Valid TrxID is required for ${paymentMethod.titleEn}"))
        }

        val deliveryFeePerShop = when (deliveryMethod) {
            DeliveryMethod.SELLER_DELIVERY -> settings.sellerDeliveryChargeBdt
            DeliveryMethod.COURIER_DELIVERY -> settings.courierDeliveryChargeBdt
        }

        return try {
            val batch = db.batch()
            val groupedByShop = cartItems.groupBy { it.product.shopId }
            val createdOrderIds = mutableListOf<String>()

            groupedByShop.forEach { (shopId, itemsForShop) ->
                val sellerId = itemsForShop.first().product.sellerUserId
                val orderId = "KB_${(1000..9999).random()}_${UUID.randomUUID().toString().take(4).uppercase()}"
                var subtotal = 0.0
                var commissionTotal = 0.0

                val fsItems = itemsForShop.map { detailed ->
                    val prod = detailed.product
                    val qty = detailed.cartItem.quantity
                    if (prod.stockQuantity < qty) {
                        throw IllegalStateException("Insufficient stock for ${prod.nameEn}")
                    }
                    val lineSub = prod.priceBdt * qty
                    val rate = CommissionCalculator.effectiveRate(prod.commissionPercentOverride, settings)
                    val lineComm = CommissionCalculator.calculateLineCommission(lineSub, rate)
                    subtotal += lineSub
                    commissionTotal += lineComm

                    // Decrement stock in batch
                    batch.update(
                        db.collection("products").document(prod.id),
                        mapOf(
                            "stockQuantity" to (prod.stockQuantity - qty),
                            "updatedAt" to serverTs()
                        )
                    )

                    FsOrderItem(
                        productId = prod.id,
                        productNameBn = prod.nameBn,
                        productNameEn = prod.nameEn,
                        unitPrice = prod.priceBdt,
                        quantity = qty.toLong(),
                        appliedCommissionPercent = rate,
                        lineSubtotal = lineSub,
                        lineCommissionAmount = lineComm
                    ).toMap()
                }

                val payStatus = if (paymentMethod == PaymentMethod.COD) {
                    PaymentStatus.PENDING_VERIFICATION
                } else {
                    PaymentStatus.PENDING_VERIFICATION
                }

                val orderPayload = mapOf(
                    "buyerUserId" to uid,
                    "buyerName" to buyer.fullName.ifBlank { "Buyer" }.take(120),
                    "buyerPhone" to buyer.phone.take(30),
                    "deliveryAddress" to deliveryAddress.trim().take(300),
                    "district" to district.trim().ifBlank { buyer.district }.take(80),
                    "shopId" to shopId,
                    "sellerUserId" to sellerId,
                    "deliveryMethod" to deliveryMethod.name,
                    "deliveryCharge" to deliveryFeePerShop,
                    "paymentMethod" to paymentMethod.name,
                    "paymentStatus" to payStatus.name,
                    "transactionId" to transactionId.trim().take(80),
                    "senderAccount" to senderAccount.trim().take(40),
                    "orderStatus" to OrderStatus.PLACED.name,
                    "grossSubtotal" to subtotal,
                    "totalPayable" to (subtotal + deliveryFeePerShop),
                    "agreedCommissionTotal" to commissionTotal,
                    "realizedCommissionAmount" to 0.0,
                    "sellerNetPayout" to 0.0,
                    "commissionStatus" to CommissionStatus.PENDING.name,
                    "trackingNote" to "অর্ডার গৃহীত হয়েছে — বিক্রেতার নিশ্চিতকরণের অপেক্ষায়",
                    "items" to fsItems,
                    "createdAt" to serverTs(),
                    "updatedAt" to serverTs()
                )
                batch.set(db.collection("orders").document(orderId), orderPayload)
                createdOrderIds.add(orderId)
            }

            // Clear user's cart items in the same batch
            cartItems.forEach { detailed ->
                batch.delete(db.collection("users").document(uid).collection("cart").document(detailed.product.id))
            }

            batch.commit().await()
            Result.success(createdOrderIds)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "orders")
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(
        order: OrderEntity,
        newStatus: OrderStatus,
        note: String = ""
    ): Result<Unit> {
        val path = "orders/${order.id}"
        return try {
            requireUserId()
            val settled = CommissionCalculator.resolveOrderSettlement(order, newStatus)
            val updates = mutableMapOf<String, Any>(
                "orderStatus" to newStatus.name,
                "trackingNote" to note.ifBlank { newStatus.titleBn }.take(300),
                "updatedAt" to serverTs()
            )
            when (newStatus) {
                OrderStatus.COMPLETED -> {
                    updates["commissionStatus"] = settled.commissionStatus.name
                    updates["realizedCommissionAmount"] = settled.realizedCommissionAmount
                    updates["sellerNetPayout"] = settled.sellerNetPayout
                    updates["paymentStatus"] = PaymentStatus.VERIFIED.name
                }
                OrderStatus.CANCELLED, OrderStatus.REFUNDED -> {
                    updates["commissionStatus"] = settled.commissionStatus.name
                    updates["realizedCommissionAmount"] = 0.0
                    updates["sellerNetPayout"] = 0.0
                    if (newStatus == OrderStatus.REFUNDED) {
                        updates["paymentStatus"] = PaymentStatus.REFUNDED.name
                    }
                }
                OrderStatus.REFUND_REQUESTED -> {
                    updates["commissionStatus"] = CommissionStatus.PENDING.name
                    updates["realizedCommissionAmount"] = 0.0
                    updates["sellerNetPayout"] = 0.0
                }
                else -> {}
            }
            db.collection("orders").document(order.id).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun verifyOrderPayment(orderId: String, paymentStatus: PaymentStatus): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.collection("orders").document(orderId)
                .update(mapOf("paymentStatus" to paymentStatus.name, "updatedAt" to serverTs()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addReview(
        productId: String,
        buyerName: String,
        rating: Int,
        comment: String
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (rating !in 1..5 || comment.isBlank()) {
            return Result.failure(IllegalArgumentException("Rating (1-5) and comment required"))
        }
        val revId = "rev_${UUID.randomUUID().toString().take(10)}"
        val path = "reviews/$revId"
        return try {
            db.collection("reviews").document(revId).set(
                mapOf(
                    "productId" to productId,
                    "buyerUserId" to uid,
                    "buyerName" to buyerName.ifBlank { "Buyer" }.take(120),
                    "rating" to rating,
                    "comment" to comment.trim().take(500),
                    "published" to true,
                    "createdAt" to serverTs(),
                    "updatedAt" to serverTs()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveShop(
        existingShopId: String?,
        shopNameBn: String,
        shopNameEn: String,
        district: String,
        description: String,
        imageUri: String
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (shopNameBn.isBlank() || shopNameEn.isBlank()) {
            return Result.failure(IllegalArgumentException("Shop name in Bangla and English required"))
        }
        val shopId = existingShopId ?: "shop_${uid.take(8)}_${UUID.randomUUID().toString().take(4)}"
        val path = "shops/$shopId"
        return try {
            val docRef = db.collection("shops").document(shopId)
            val cleanUri = imageUri.ifBlank { "drawable://img_cat_vegetables" }.take(500)
            if (existingShopId == null) {
                docRef.set(
                    mapOf(
                        "ownerUserId" to uid,
                        "shopNameBn" to shopNameBn.trim().take(120),
                        "shopNameEn" to shopNameEn.trim().take(120),
                        "district" to district.trim().ifBlank { "Dhaka" }.take(80),
                        "description" to description.trim().take(500),
                        "imageUri" to cleanUri,
                        "approvalStatus" to ApprovalStatus.PENDING.name,
                        "sellerCommissionAccepted" to true,
                        "createdAt" to serverTs(),
                        "updatedAt" to serverTs()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "shopNameBn" to shopNameBn.trim().take(120),
                        "shopNameEn" to shopNameEn.trim().take(120),
                        "district" to district.trim().ifBlank { "Dhaka" }.take(80),
                        "description" to description.trim().take(500),
                        "imageUri" to cleanUri,
                        "approvalStatus" to ApprovalStatus.PENDING.name,
                        "sellerCommissionAccepted" to true,
                        "updatedAt" to serverTs()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveSellerProduct(
        existingProduct: ProductEntity?,
        shopId: String,
        nameBn: String,
        nameEn: String,
        category: ProductCategory,
        priceBdt: Double,
        unitLabel: String,
        stockQuantity: Int,
        description: String,
        imageUri: String,
        customCommissionPercent: Double?,
        sellerCommissionAccepted: Boolean,
        settings: PlatformSettingsEntity
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (!sellerCommissionAccepted) {
            return Result.failure(IllegalArgumentException("Seller must accept platform commission policy"))
        }
        if (nameBn.isBlank() || priceBdt <= 0.0 || stockQuantity < 0) {
            return Result.failure(IllegalArgumentException("Valid product name, positive price, and stock required"))
        }
        val clampedOverride = customCommissionPercent?.let { rate ->
            CommissionCalculator.effectiveRate(rate, settings)
        }
        val pid = existingProduct?.id ?: "prod_${UUID.randomUUID().toString().take(8)}"
        val path = "products/$pid"
        return try {
            val docRef = db.collection("products").document(pid)
            val cleanUri = imageUri.ifBlank { category.defaultDrawableKey }.take(500)
            if (existingProduct == null) {
                val payload = mutableMapOf<String, Any>(
                    "shopId" to shopId,
                    "sellerUserId" to uid,
                    "nameBn" to nameBn.trim().take(150),
                    "nameEn" to nameEn.trim().ifBlank { nameBn.trim() }.take(150),
                    "category" to category.name,
                    "priceBdt" to priceBdt,
                    "unitLabel" to unitLabel.trim().ifBlank { "১ কেজি / 1 kg" }.take(60),
                    "stockQuantity" to stockQuantity,
                    "description" to description.trim().take(600),
                    "imageUri" to cleanUri,
                    "sellerCommissionAccepted" to true,
                    "approvalStatus" to ApprovalStatus.PENDING.name,
                    "featured" to false,
                    "createdAt" to serverTs(),
                    "updatedAt" to serverTs()
                )
                if (clampedOverride != null) {
                    payload["commissionPercentOverride"] = clampedOverride
                }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "nameBn" to nameBn.trim().take(150),
                    "nameEn" to nameEn.trim().ifBlank { nameBn.trim() }.take(150),
                    "category" to category.name,
                    "priceBdt" to priceBdt,
                    "unitLabel" to unitLabel.trim().ifBlank { "১ কেজি / 1 kg" }.take(60),
                    "stockQuantity" to stockQuantity,
                    "description" to description.trim().take(600),
                    "imageUri" to cleanUri,
                    "sellerCommissionAccepted" to true,
                    "approvalStatus" to ApprovalStatus.PENDING.name,
                    "updatedAt" to serverTs()
                )
                if (clampedOverride != null) {
                    updates["commissionPercentOverride"] = clampedOverride
                }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun adjustProductStock(productId: String, newStock: Int): Result<Unit> {
        val path = "products/$productId"
        return try {
            requireUserId()
            db.collection("products").document(productId)
                .update(mapOf("stockQuantity" to newStock.coerceAtLeast(0), "updatedAt" to serverTs()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun requestWithdrawal(
        shop: ShopEntity,
        amountBdt: Double,
        payoutMethod: PaymentMethod,
        payoutAccount: String,
        availableBalance: Double
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (amountBdt <= 0.0 || amountBdt > availableBalance) {
            return Result.failure(IllegalArgumentException("Withdrawal exceeds available net balance"))
        }
        if (payoutAccount.trim().length < 5) {
            return Result.failure(IllegalArgumentException("Valid payout account number is required"))
        }
        val wid = "WD_${UUID.randomUUID().toString().take(6).uppercase()}"
        val path = "withdrawals/$wid"
        return try {
            db.collection("withdrawals").document(wid).set(
                mapOf(
                    "sellerUserId" to uid,
                    "shopId" to shop.id,
                    "shopName" to shop.shopNameBn.take(150),
                    "amountBdt" to amountBdt,
                    "payoutMethod" to payoutMethod.name,
                    "payoutAccount" to payoutAccount.trim().take(40),
                    "status" to WithdrawalStatus.PENDING.name,
                    "adminNote" to "",
                    "requestedAt" to serverTs()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // ---- Admin Operations ----

    suspend fun approveShop(shopId: String, status: ApprovalStatus): Result<Unit> {
        val path = "shops/$shopId"
        return try {
            requireUserId()
            db.collection("shops").document(shopId)
                .update(mapOf("approvalStatus" to status.name, "updatedAt" to serverTs()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun approveProduct(productId: String, status: ApprovalStatus): Result<Unit> {
        val path = "products/$productId"
        return try {
            requireUserId()
            db.collection("products").document(productId)
                .update(mapOf("approvalStatus" to status.name, "updatedAt" to serverTs()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProductCommissionByAdmin(
        productId: String,
        newRate: Double?,
        settings: PlatformSettingsEntity
    ): Result<Unit> {
        val path = "products/$productId"
        return try {
            requireUserId()
            val clamped = newRate?.let { rate ->
                CommissionCalculator.effectiveRate(rate, settings)
            }
            val updates = mutableMapOf<String, Any>("updatedAt" to serverTs())
            if (clamped != null) {
                updates["commissionPercentOverride"] = clamped
            } else {
                updates["commissionPercentOverride"] = com.google.firebase.firestore.FieldValue.delete()
            }
            db.collection("products").document(productId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleProductFeatured(productId: String, featured: Boolean): Result<Unit> {
        val path = "products/$productId"
        return try {
            requireUserId()
            db.collection("products").document(productId)
                .update(mapOf("featured" to featured, "updatedAt" to serverTs()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setUserBlocked(userId: String, blocked: Boolean): Result<Unit> {
        val path = "users/$userId"
        return try {
            requireUserId()
            db.collection("users").document(userId)
                .update(mapOf("blocked" to blocked, "updatedAt" to serverTs()))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveWithdrawal(withdrawalId: String, status: WithdrawalStatus, note: String): Result<Unit> {
        val path = "withdrawals/$withdrawalId"
        return try {
            requireUserId()
            db.collection("withdrawals").document(withdrawalId)
                .update(
                    mapOf(
                        "status" to status.name,
                        "adminNote" to note.take(200),
                        "resolvedAt" to serverTs()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun withdrawAdminCommission(
        adminUser: UserEntity,
        amountBdt: Double,
        payoutMethod: PaymentMethod,
        payoutAccount: String,
        note: String,
        availableBalance: Double
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (adminUser.role != UserRole.MAIN_ADMIN) {
            return Result.failure(SecurityException("Only Main Admin can withdraw platform commission"))
        }
        if (payoutMethod == PaymentMethod.COD) {
            return Result.failure(IllegalArgumentException("Select bKash, Nagad, or Rocket for payout"))
        }
        if (amountBdt <= 0.0 || amountBdt > availableBalance) {
            return Result.failure(IllegalArgumentException("Withdrawal exceeds available platform commission"))
        }
        if (payoutAccount.trim().length < 5) {
            return Result.failure(IllegalArgumentException("Valid payout account number is required"))
        }
        val wid = "ADM_WD_${UUID.randomUUID().toString().take(6).uppercase()}"
        val path = "withdrawals/$wid"
        return try {
            db.collection("withdrawals").document(wid).set(
                mapOf(
                    "sellerUserId" to uid,
                    "shopId" to PLATFORM_ADMIN_SHOP_ID,
                    "shopName" to "অ্যাডমিন কমিশন উত্তোলন (Krishi Bazar BD)",
                    "amountBdt" to amountBdt,
                    "payoutMethod" to payoutMethod.name,
                    "payoutAccount" to payoutAccount.trim().take(40),
                    "status" to WithdrawalStatus.APPROVED.name,
                    "adminNote" to note.ifBlank { "Owner Commission Withdrawal" }.take(200),
                    "requestedAt" to serverTs(),
                    "resolvedAt" to serverTs()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun savePlatformSettings(settings: PlatformSettingsEntity): Result<Unit> {
        if (settings.minCommissionPercent < 0 || settings.maxCommissionPercent > 100 ||
            settings.minCommissionPercent > settings.maxCommissionPercent
        ) {
            return Result.failure(IllegalArgumentException("Invalid min/max commission bounds"))
        }
        val clampedDefault = settings.defaultCommissionPercent.coerceIn(
            settings.minCommissionPercent,
            settings.maxCommissionPercent
        )
        val path = "platform_settings/global"
        return try {
            requireUserId()
            db.collection("platform_settings").document("global").set(
                mapOf(
                    "defaultCommissionPercent" to clampedDefault,
                    "minCommissionPercent" to settings.minCommissionPercent,
                    "maxCommissionPercent" to settings.maxCommissionPercent,
                    "sellerDeliveryChargeBdt" to settings.sellerDeliveryChargeBdt,
                    "courierDeliveryChargeBdt" to settings.courierDeliveryChargeBdt,
                    "siteBannerTitleBn" to settings.siteBannerTitleBn.ifBlank { "সরাসরি কৃষকের মাঠ থেকে আপনার ঘরে" }.take(200),
                    "siteBannerTitleEn" to settings.siteBannerTitleEn.ifBlank { "Fresh From Bangladeshi Farms to Your Home" }.take(200),
                    "maintenanceNotice" to settings.maintenanceNotice.take(300),
                    "updatedAt" to serverTs()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun createBackupSnapshot(
        users: List<UserEntity>,
        shops: List<ShopEntity>,
        products: List<ProductEntity>,
        orders: List<OrderEntity>,
        settings: PlatformSettingsEntity
    ): String {
        val root = JSONObject().apply {
            put("app", "Krishi Bazar BD (Cloud Firestore)")
            put("generatedAt", System.currentTimeMillis())
            put("defaultCommissionPercent", settings.defaultCommissionPercent)
            put("usersCount", users.size)
            put("shopsCount", shops.size)
            put("productsCount", products.size)
            put("ordersCount", orders.size)
            put("ordersSummary", JSONArray(orders.map {
                JSONObject().apply {
                    put("id", it.id)
                    put("status", it.orderStatus.name)
                    put("gross", it.grossSubtotal)
                    put("commission", it.realizedCommissionAmount)
                }
            }))
        }
        return root.toString(2)
    }
}
