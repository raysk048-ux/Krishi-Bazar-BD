package com.example.firebase

import androidx.annotation.Keep
import com.example.model.ApprovalStatus
import com.example.model.CartItemEntity
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

private inline fun <reified T : Enum<T>> safeEnum(raw: String?, default: T): T {
    if (raw.isNullOrBlank()) return default
    return enumValues<T>().firstOrNull { it.name == raw } ?: default
}

@Keep
@IgnoreExtraProperties
data class FsUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val role: String = UserRole.BUYER.name,
    val blocked: Boolean = false,
    val defaultAddress: String = "",
    val district: String = "Dhaka",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): UserEntity = UserEntity(
        id = userId.ifBlank { id },
        fullName = fullName,
        phone = phone,
        email = email,
        passwordHash = "",
        role = safeEnum(role, UserRole.BUYER),
        isBlocked = blocked,
        defaultAddress = defaultAddress,
        district = district,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

@Keep
@IgnoreExtraProperties
data class FsShop(
    @DocumentId val id: String = "",
    val ownerUserId: String = "",
    val shopNameBn: String = "",
    val shopNameEn: String = "",
    val district: String = "Dhaka",
    val description: String = "",
    val imageUri: String = "",
    val approvalStatus: String = ApprovalStatus.PENDING.name,
    val sellerCommissionAccepted: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): ShopEntity = ShopEntity(
        id = id,
        ownerUserId = ownerUserId,
        shopNameBn = shopNameBn,
        shopNameEn = shopNameEn,
        district = district,
        phone = "",
        description = description,
        imageUri = imageUri,
        approvalStatus = safeEnum(approvalStatus, ApprovalStatus.PENDING),
        sellerCommissionAccepted = sellerCommissionAccepted,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

@Keep
@IgnoreExtraProperties
data class FsProduct(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val sellerUserId: String = "",
    val nameBn: String = "",
    val nameEn: String = "",
    val category: String = ProductCategory.VEGETABLES.name,
    val priceBdt: Double = 0.0,
    val unitLabel: String = "",
    val stockQuantity: Long = 0L,
    val description: String = "",
    val imageUri: String = "",
    val commissionPercentOverride: Double? = null,
    val sellerCommissionAccepted: Boolean = false,
    val approvalStatus: String = ApprovalStatus.PENDING.name,
    val featured: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): ProductEntity = ProductEntity(
        id = id,
        shopId = shopId,
        sellerUserId = sellerUserId,
        nameBn = nameBn,
        nameEn = nameEn,
        category = safeEnum(category, ProductCategory.OTHERS),
        priceBdt = priceBdt,
        unitLabel = unitLabel,
        stockQuantity = stockQuantity.toInt(),
        description = description,
        imageUri = imageUri,
        commissionPercentOverride = commissionPercentOverride,
        sellerCommissionAccepted = sellerCommissionAccepted,
        approvalStatus = safeEnum(approvalStatus, ApprovalStatus.PENDING),
        featured = featured,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

@Keep
@IgnoreExtraProperties
data class FsCartItem(
    @DocumentId val id: String = "",
    val buyerUserId: String = "",
    val productId: String = "",
    val quantity: Long = 1L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): CartItemEntity = CartItemEntity(
        id = (productId.ifBlank { id }).hashCode().toLong(),
        buyerUserId = buyerUserId,
        productId = productId.ifBlank { id },
        quantity = quantity.toInt()
    )
}

@Keep
@IgnoreExtraProperties
data class FsOrderItem(
    val productId: String = "",
    val productNameBn: String = "",
    val productNameEn: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Long = 1L,
    val appliedCommissionPercent: Double = 50.0,
    val lineSubtotal: Double = 0.0,
    val lineCommissionAmount: Double = 0.0
) {
    fun toDomain(orderId: String): OrderItemEntity = OrderItemEntity(
        id = "${orderId}_$productId".hashCode().toLong(),
        orderId = orderId,
        productId = productId,
        productNameBn = productNameBn,
        productNameEn = productNameEn,
        unitPrice = unitPrice,
        quantity = quantity.toInt(),
        appliedCommissionPercent = appliedCommissionPercent,
        lineSubtotal = lineSubtotal,
        lineCommissionAmount = lineCommissionAmount
    )

    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId,
        "productNameBn" to productNameBn,
        "productNameEn" to productNameEn,
        "unitPrice" to unitPrice,
        "quantity" to quantity,
        "appliedCommissionPercent" to appliedCommissionPercent,
        "lineSubtotal" to lineSubtotal,
        "lineCommissionAmount" to lineCommissionAmount
    )
}

@Keep
@IgnoreExtraProperties
data class FsOrder(
    @DocumentId val id: String = "",
    val buyerUserId: String = "",
    val buyerName: String = "",
    val buyerPhone: String = "",
    val deliveryAddress: String = "",
    val district: String = "Dhaka",
    val shopId: String = "",
    val sellerUserId: String = "",
    val deliveryMethod: String = DeliveryMethod.SELLER_DELIVERY.name,
    val deliveryCharge: Double = 0.0,
    val paymentMethod: String = PaymentMethod.COD.name,
    val paymentStatus: String = PaymentStatus.PENDING_VERIFICATION.name,
    val transactionId: String = "",
    val senderAccount: String = "",
    val orderStatus: String = OrderStatus.PLACED.name,
    val grossSubtotal: Double = 0.0,
    val totalPayable: Double = 0.0,
    val agreedCommissionTotal: Double = 0.0,
    val realizedCommissionAmount: Double = 0.0,
    val sellerNetPayout: Double = 0.0,
    val commissionStatus: String = CommissionStatus.PENDING.name,
    val trackingNote: String = "",
    val items: List<FsOrderItem> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomainOrder(): OrderEntity = OrderEntity(
        id = id,
        buyerUserId = buyerUserId,
        buyerName = buyerName,
        buyerPhone = buyerPhone,
        deliveryAddress = deliveryAddress,
        district = district,
        shopId = shopId,
        sellerUserId = sellerUserId,
        deliveryMethod = safeEnum(deliveryMethod, DeliveryMethod.SELLER_DELIVERY),
        deliveryCharge = deliveryCharge,
        paymentMethod = safeEnum(paymentMethod, PaymentMethod.COD),
        paymentStatus = safeEnum(paymentStatus, PaymentStatus.PENDING_VERIFICATION),
        transactionId = transactionId,
        senderAccount = senderAccount,
        orderStatus = safeEnum(orderStatus, OrderStatus.PLACED),
        grossSubtotal = grossSubtotal,
        totalPayable = totalPayable,
        agreedCommissionTotal = agreedCommissionTotal,
        realizedCommissionAmount = realizedCommissionAmount,
        sellerNetPayout = sellerNetPayout,
        commissionStatus = safeEnum(commissionStatus, CommissionStatus.PENDING),
        trackingNote = trackingNote,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
        updatedAt = updatedAt?.toDate()?.time ?: System.currentTimeMillis()
    )

    fun toDomainItems(): List<OrderItemEntity> = items.map { it.toDomain(id) }
}

@Keep
@IgnoreExtraProperties
data class FsReview(
    @DocumentId val id: String = "",
    val productId: String = "",
    val buyerUserId: String = "",
    val buyerName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val published: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): ReviewEntity = ReviewEntity(
        id = id.hashCode().toLong(),
        productId = productId,
        buyerUserId = buyerUserId,
        buyerName = buyerName,
        rating = rating.toInt(),
        comment = comment,
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

@Keep
@IgnoreExtraProperties
data class FsWithdrawal(
    @DocumentId val id: String = "",
    val sellerUserId: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val amountBdt: Double = 0.0,
    val payoutMethod: String = PaymentMethod.BKASH.name,
    val payoutAccount: String = "",
    val status: String = WithdrawalStatus.PENDING.name,
    val adminNote: String = "",
    val requestedAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null
) {
    fun toDomain(): WithdrawalEntity = WithdrawalEntity(
        id = id,
        sellerUserId = sellerUserId,
        shopId = shopId,
        shopName = shopName,
        amountBdt = amountBdt,
        payoutMethod = safeEnum(payoutMethod, PaymentMethod.BKASH),
        payoutAccount = payoutAccount,
        status = safeEnum(status, WithdrawalStatus.PENDING),
        adminNote = adminNote,
        requestedAt = requestedAt?.toDate()?.time ?: System.currentTimeMillis(),
        resolvedAt = resolvedAt?.toDate()?.time
    )
}

@Keep
@IgnoreExtraProperties
data class FsPlatformSettings(
    @DocumentId val id: String = "",
    val defaultCommissionPercent: Double = 50.0,
    val minCommissionPercent: Double = 5.0,
    val maxCommissionPercent: Double = 60.0,
    val sellerDeliveryChargeBdt: Double = 60.0,
    val courierDeliveryChargeBdt: Double = 110.0,
    val siteBannerTitleBn: String = "সরাসরি কৃষকের মাঠ থেকে আপনার ঘরে",
    val siteBannerTitleEn: String = "Fresh From Bangladeshi Farms to Your Home",
    val maintenanceNotice: String = "",
    val updatedAt: Timestamp? = null
) {
    fun toDomain(): PlatformSettingsEntity = PlatformSettingsEntity(
        id = 1,
        defaultCommissionPercent = defaultCommissionPercent,
        minCommissionPercent = minCommissionPercent,
        maxCommissionPercent = maxCommissionPercent,
        sellerDeliveryChargeBdt = sellerDeliveryChargeBdt,
        courierDeliveryChargeBdt = courierDeliveryChargeBdt,
        siteBannerTitleBn = siteBannerTitleBn,
        siteBannerTitleEn = siteBannerTitleEn,
        maintenanceNotice = maintenanceNotice,
        lastBackupAt = updatedAt?.toDate()?.time
    )
}

fun serverTs(): Any = FieldValue.serverTimestamp()
