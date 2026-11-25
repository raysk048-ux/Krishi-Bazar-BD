package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppPreferences
import com.example.data.KrishiDatabase
import com.example.data.KrishiRepository
import com.example.model.DeliveryMethod
import com.example.model.OrderStatus
import com.example.model.PaymentMethod
import com.example.model.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KrishiRepositoryTest {

    private lateinit var db: KrishiDatabase
    private lateinit var repository: KrishiRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, KrishiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = KrishiRepository(db, AppPreferences(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `everyone can register, order, realize commission on completion, and admin can withdraw`() = runTest {
        repository.initializeDatabaseAndSession()

        // 1. New buyer registers
        val regResult = repository.registerUser(
            fullName = "সুমাইয়া আক্তার",
            phone = "01799887766",
            email = "sumaiya@example.com",
            password = "pass1234",
            role = UserRole.BUYER,
            district = "Rajshahi",
            address = "সাহেব বাজার, রাজশাহী"
        )
        assertTrue(regResult.isSuccess)
        val buyer = regResult.getOrThrow()

        // 2. Add to cart and place COD order for prod_1 (price 90, 50% commission)
        assertTrue(repository.addToCart(buyer.id, "prod_1", 2).isSuccess)
        val cart = repository.observeCart(buyer.id).first()
        val orderIds = repository.placeOrdersFromCart(
            buyer = buyer,
            cartItems = cart,
            deliveryMethod = DeliveryMethod.SELLER_DELIVERY,
            paymentMethod = PaymentMethod.COD,
            transactionId = "",
            senderAccount = "",
            deliveryAddress = buyer.defaultAddress,
            district = buyer.district
        ).getOrThrow()
        assertEquals(1, orderIds.size)

        // 3. Complete order -> realizes 50% commission = 90.0 on 180.0 subtotal
        val orderId = orderIds.first()
        assertTrue(repository.updateOrderStatus(orderId, OrderStatus.COMPLETED).isSuccess)
        val completedOrder = db.krishiDao().getOrderById(orderId)!!
        assertEquals(90.0, completedOrder.realizedCommissionAmount, 0.01)

        // 4. Main Admin withdraws 50.0 commission; over-balance fails
        val admin = db.krishiDao().getUserById("user_admin_main")!!
        val failWithdraw = repository.withdrawAdminCommission(
            adminUser = admin,
            amountBdt = 5000.0,
            payoutMethod = PaymentMethod.BKASH,
            payoutAccount = "01700000001",
            note = "Too much",
            availableBalance = 313.0
        )
        assertTrue(failWithdraw.isFailure)

        val okWithdraw = repository.withdrawAdminCommission(
            adminUser = admin,
            amountBdt = 100.0,
            payoutMethod = PaymentMethod.BKASH,
            payoutAccount = "01700000001",
            note = "Owner payout",
            availableBalance = 313.0
        )
        assertTrue(okWithdraw.isSuccess)

        // 5. Admin login requires valid password and supports rotation
        assertTrue(repository.login("raysk048@gmail.com", "wrongpass").isFailure)
        assertTrue(repository.login("raysk048@gmail.com", "admin123").isSuccess)
        assertTrue(repository.updateAdminPassword(admin.id, "KrishiOwner#2026").isSuccess)
        assertTrue(repository.login("raysk048@gmail.com", "admin123").isFailure)
        assertTrue(repository.login("raysk048@gmail.com", "KrishiOwner#2026").isSuccess)
    }
}
