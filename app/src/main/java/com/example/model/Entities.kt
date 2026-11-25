package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole,
    val isBlocked: Boolean = false,
    val defaultAddress: String = "",
    val district: String = "Dhaka",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val shopNameBn: String,
    val shopNameEn: String,
    val district: String,
    val phone: String,
    val description: String,
    val imageUri: String,
    val approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,
    val sellerCommissionAccepted: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val sellerUserId: String,
    val nameBn: String,
    val nameEn: String,
    val category: ProductCategory,
    val priceBdt: Double,
    val unitLabel: String,
    val stockQuantity: Int,
    val description: String,
    val imageUri: String,
    val commissionPercentOverride: Double? = null,
    val sellerCommissionAccepted: Boolean = false,
    val approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,
    val featured: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val buyerUserId: String,
    val productId: String,
    val quantity: Int
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val buyerUserId: String,
    val buyerName: String,
    val buyerPhone: String,
    val deliveryAddress: String,
    val district: String,
    val shopId: String,
    val sellerUserId: String,
    val deliveryMethod: DeliveryMethod,
    val deliveryCharge: Double,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val transactionId: String = "",
    val senderAccount: String = "",
    val orderStatus: OrderStatus = OrderStatus.PLACED,
    val grossSubtotal: Double,
    val totalPayable: Double,
    val agreedCommissionTotal: Double,
    val realizedCommissionAmount: Double = 0.0,
    val sellerNetPayout: Double = 0.0,
    val commissionStatus: CommissionStatus = CommissionStatus.PENDING,
    val trackingNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val orderId: String,
    val productId: String,
    val productNameBn: String,
    val productNameEn: String,
    val unitPrice: Double,
    val quantity: Int,
    val appliedCommissionPercent: Double,
    val lineSubtotal: Double,
    val lineCommissionAmount: Double
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productId: String,
    val buyerUserId: String,
    val buyerName: String,
    val rating: Int,
    val comment: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "withdrawals")
data class WithdrawalEntity(
    @PrimaryKey val id: String,
    val sellerUserId: String,
    val shopId: String,
    val shopName: String,
    val amountBdt: Double,
    val payoutMethod: PaymentMethod,
    val payoutAccount: String,
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val adminNote: String = "",
    val requestedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

@Entity(tableName = "platform_settings")
data class PlatformSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val defaultCommissionPercent: Double = 50.0,
    val minCommissionPercent: Double = 5.0,
    val maxCommissionPercent: Double = 60.0,
    val sellerDeliveryChargeBdt: Double = 60.0,
    val courierDeliveryChargeBdt: Double = 110.0,
    val siteBannerTitleBn: String = "সরাসরি কৃষকের মাঠ থেকে আপনার ঘরে",
    val siteBannerTitleEn: String = "Fresh From Bangladeshi Farms to Your Home",
    val maintenanceNotice: String = "",
    val lastBackupAt: Long? = null
)
