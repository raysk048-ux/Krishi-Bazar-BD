package com.example.data

import androidx.room.TypeConverter
import com.example.model.ApprovalStatus
import com.example.model.CommissionStatus
import com.example.model.DeliveryMethod
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.ProductCategory
import com.example.model.UserRole
import com.example.model.WithdrawalStatus

class KrishiConverters {
    @TypeConverter fun fromUserRole(v: UserRole): String = v.name
    @TypeConverter fun toUserRole(v: String): UserRole = UserRole.entries.firstOrNull { it.name == v } ?: UserRole.BUYER

    @TypeConverter fun fromCategory(v: ProductCategory): String = v.name
    @TypeConverter fun toCategory(v: String): ProductCategory = ProductCategory.entries.firstOrNull { it.name == v } ?: ProductCategory.OTHERS

    @TypeConverter fun fromApproval(v: ApprovalStatus): String = v.name
    @TypeConverter fun toApproval(v: String): ApprovalStatus = ApprovalStatus.entries.firstOrNull { it.name == v } ?: ApprovalStatus.PENDING

    @TypeConverter fun fromOrderStatus(v: OrderStatus): String = v.name
    @TypeConverter fun toOrderStatus(v: String): OrderStatus = OrderStatus.entries.firstOrNull { it.name == v } ?: OrderStatus.PLACED

    @TypeConverter fun fromPaymentMethod(v: PaymentMethod): String = v.name
    @TypeConverter fun toPaymentMethod(v: String): PaymentMethod = PaymentMethod.entries.firstOrNull { it.name == v } ?: PaymentMethod.COD

    @TypeConverter fun fromPaymentStatus(v: PaymentStatus): String = v.name
    @TypeConverter fun toPaymentStatus(v: String): PaymentStatus = PaymentStatus.entries.firstOrNull { it.name == v } ?: PaymentStatus.PENDING_VERIFICATION

    @TypeConverter fun fromDeliveryMethod(v: DeliveryMethod): String = v.name
    @TypeConverter fun toDeliveryMethod(v: String): DeliveryMethod = DeliveryMethod.entries.firstOrNull { it.name == v } ?: DeliveryMethod.SELLER_DELIVERY

    @TypeConverter fun fromCommissionStatus(v: CommissionStatus): String = v.name
    @TypeConverter fun toCommissionStatus(v: String): CommissionStatus = CommissionStatus.entries.firstOrNull { it.name == v } ?: CommissionStatus.PENDING

    @TypeConverter fun fromWithdrawalStatus(v: WithdrawalStatus): String = v.name
    @TypeConverter fun toWithdrawalStatus(v: String): WithdrawalStatus = WithdrawalStatus.entries.firstOrNull { it.name == v } ?: WithdrawalStatus.PENDING
}
