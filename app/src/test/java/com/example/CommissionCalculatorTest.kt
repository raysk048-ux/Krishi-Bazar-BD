package com.example

import com.example.model.CommissionCalculator
import com.example.model.CommissionStatus
import com.example.model.DeliveryMethod
import com.example.model.OrderEntity
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.PlatformSettingsEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CommissionCalculatorTest {

    private val defaultSettings = PlatformSettingsEntity(
        defaultCommissionPercent = 50.0,
        minCommissionPercent = 5.0,
        maxCommissionPercent = 60.0
    )

    @Test
    fun `effectiveRate uses default 50 percent and clamps overrides within min max bounds`() {
        assertEquals(50.0, CommissionCalculator.effectiveRate(null, defaultSettings), 0.01)
        assertEquals(35.0, CommissionCalculator.effectiveRate(35.0, defaultSettings), 0.01)
        assertEquals(5.0, CommissionCalculator.effectiveRate(1.0, defaultSettings), 0.01)
        assertEquals(60.0, CommissionCalculator.effectiveRate(85.0, defaultSettings), 0.01)
    }

    @Test
    fun `commission is earned only on COMPLETED orders and reversed on CANCELLED or REFUNDED`() {
        val baseOrder = OrderEntity(
            id = "KB-TEST-1",
            buyerUserId = "buyer_1",
            buyerName = "Rakib",
            buyerPhone = "01700000000",
            deliveryAddress = "Dhaka",
            district = "Dhaka",
            shopId = "shop_1",
            sellerUserId = "seller_1",
            deliveryMethod = DeliveryMethod.SELLER_DELIVERY,
            deliveryCharge = 60.0,
            paymentMethod = PaymentMethod.COD,
            paymentStatus = PaymentStatus.PENDING_VERIFICATION,
            orderStatus = OrderStatus.PLACED,
            grossSubtotal = 1000.0,
            totalPayable = 1060.0,
            agreedCommissionTotal = 500.0
        )

        val shipped = CommissionCalculator.resolveOrderSettlement(baseOrder, OrderStatus.SHIPPED)
        assertEquals(0.0, shipped.realizedCommissionAmount, 0.01)
        assertEquals(CommissionStatus.PENDING, shipped.commissionStatus)

        val completed = CommissionCalculator.resolveOrderSettlement(shipped, OrderStatus.COMPLETED)
        assertEquals(500.0, completed.realizedCommissionAmount, 0.01)
        assertEquals(500.0, completed.sellerNetPayout, 0.01)
        assertEquals(CommissionStatus.EARNED, completed.commissionStatus)
        assertEquals(PaymentStatus.VERIFIED, completed.paymentStatus)

        val refunded = CommissionCalculator.resolveOrderSettlement(completed, OrderStatus.REFUNDED)
        assertEquals(0.0, refunded.realizedCommissionAmount, 0.01)
        assertEquals(0.0, refunded.sellerNetPayout, 0.01)
        assertEquals(CommissionStatus.ADJUSTED_REVERSED, refunded.commissionStatus)
        assertEquals(PaymentStatus.REFUNDED, refunded.paymentStatus)
    }
}
