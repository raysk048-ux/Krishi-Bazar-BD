package com.example.model

import kotlin.math.roundToLong

data class ProductWithShop(
    val product: ProductEntity,
    val shop: ShopEntity?,
    val effectiveCommissionPercent: Double,
    val averageRating: Double,
    val reviewCount: Int
)

data class CartItemDetailed(
    val cartItem: CartItemEntity,
    val product: ProductEntity,
    val shop: ShopEntity?,
    val effectiveCommissionPercent: Double
) {
    val lineSubtotal: Double get() = product.priceBdt * cartItem.quantity
}

data class OrderDetailed(
    val order: OrderEntity,
    val items: List<OrderItemEntity>,
    val shop: ShopEntity?
)

data class SellerFinanceSummary(
    val completedGrossSales: Double,
    val earnedCommissionDeducted: Double,
    val completedNetEarnings: Double,
    val pendingOrderValue: Double,
    val approvedWithdrawn: Double,
    val pendingWithdrawalAmount: Double,
    val availableBalance: Double
)

data class AdminReportSummary(
    val totalUsers: Int,
    val totalShops: Int,
    val pendingShopsCount: Int,
    val pendingProductsCount: Int,
    val pendingWithdrawalsCount: Int,
    val refundRequestedCount: Int,
    val completedOrdersCount: Int,
    val cancelledOrRefundedCount: Int,
    val totalGrossSalesBdt: Double,
    val totalPlatformCommissionBdt: Double,
    val totalAdjustedCommissionBdt: Double,
    val totalSellerPayoutsBdt: Double,
    val adminWithdrawnBdt: Double = 0.0,
    val adminAvailableCommissionBdt: Double = 0.0,
    val categorySalesBreakdown: List<Pair<ProductCategory, Double>>
)

object CommissionCalculator {
    fun effectiveRate(
        productOverride: Double?,
        settings: PlatformSettingsEntity
    ): Double {
        val raw = productOverride ?: settings.defaultCommissionPercent
        return raw.coerceIn(settings.minCommissionPercent, settings.maxCommissionPercent)
    }

    fun calculateLineCommission(lineSubtotal: Double, ratePercent: Double): Double {
        return ((lineSubtotal * (ratePercent / 100.0)) * 100.0).roundToLong() / 100.0
    }

    /**
     * Enforces platform rule:
     * - Commission is ONLY realized when orderStatus == OrderStatus.COMPLETED.
     * - CANCELLED or REFUNDED orders reset realized commission & seller net payout to 0.0 (ADJUSTED_REVERSED).
     */
    fun resolveOrderSettlement(
        order: OrderEntity,
        newStatus: OrderStatus
    ): OrderEntity {
        return when (newStatus) {
            OrderStatus.COMPLETED -> {
                val commission = order.agreedCommissionTotal
                val net = (order.grossSubtotal - commission).coerceAtLeast(0.0)
                order.copy(
                    orderStatus = newStatus,
                    realizedCommissionAmount = commission,
                    sellerNetPayout = net,
                    commissionStatus = CommissionStatus.EARNED,
                    paymentStatus = if (order.paymentMethod == PaymentMethod.COD) PaymentStatus.VERIFIED else order.paymentStatus,
                    updatedAt = System.currentTimeMillis()
                )
            }
            OrderStatus.CANCELLED, OrderStatus.REFUNDED -> {
                order.copy(
                    orderStatus = newStatus,
                    realizedCommissionAmount = 0.0,
                    sellerNetPayout = 0.0,
                    commissionStatus = CommissionStatus.ADJUSTED_REVERSED,
                    paymentStatus = if (newStatus == OrderStatus.REFUNDED) PaymentStatus.REFUNDED else order.paymentStatus,
                    updatedAt = System.currentTimeMillis()
                )
            }
            else -> {
                order.copy(
                    orderStatus = newStatus,
                    realizedCommissionAmount = 0.0,
                    sellerNetPayout = 0.0,
                    commissionStatus = CommissionStatus.PENDING,
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    }
}
