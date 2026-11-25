package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import java.security.MessageDigest

@Database(
    entities = [
        UserEntity::class,
        ShopEntity::class,
        ProductEntity::class,
        CartItemEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        ReviewEntity::class,
        WithdrawalEntity::class,
        PlatformSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(KrishiConverters::class)
abstract class KrishiDatabase : RoomDatabase() {
    abstract fun krishiDao(): KrishiDao

    companion object {
        @Volatile
        private var INSTANCE: KrishiDatabase? = null

        fun getInstance(context: Context): KrishiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KrishiDatabase::class.java,
                    "krishi_bazar_bd.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

object PasswordHasher {
    fun hash(rawPassword: String, salt: String = "krishi_bd_v1"): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$rawPassword".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

object KrishiSeedData {
    suspend fun ensureSeeded(dao: KrishiDao): Boolean {
        if (dao.getUserCount() > 0) {
            // Ensure owner admin account reflects owner email & title on existing DBs
            val existingAdmin = dao.getUserById("user_admin_main")
            if (existingAdmin != null && existingAdmin.email != "raysk048@gmail.com") {
                dao.insertUser(
                    existingAdmin.copy(
                        fullName = "প্রধান অ্যাডমিন (আমি নিজে)",
                        email = "raysk048@gmail.com"
                    )
                )
            }
            return false
        }

        val now = System.currentTimeMillis()
        val settings = PlatformSettingsEntity(
            id = 1,
            defaultCommissionPercent = 50.0,
            minCommissionPercent = 5.0,
            maxCommissionPercent = 60.0,
            sellerDeliveryChargeBdt = 60.0,
            courierDeliveryChargeBdt = 110.0,
            siteBannerTitleBn = "সরাসরি কৃষকের মাঠ থেকে আপনার ঘরে",
            siteBannerTitleEn = "Fresh From Bangladeshi Farms to Your Home",
            maintenanceNotice = "",
            lastBackupAt = now - 86_400_000L
        )
        dao.saveSettings(settings)

        val adminId = "user_admin_main"
        val seller1Id = "user_seller_sobuj"
        val seller2Id = "user_seller_padma"
        val seller3Id = "user_seller_bogura"
        val buyerId = "user_buyer_rakib"

        dao.insertUsers(
            listOf(
                UserEntity(
                    id = adminId,
                    fullName = "প্রধান অ্যাডমিন (আমি নিজে)",
                    phone = "01700000001",
                    email = "raysk048@gmail.com",
                    passwordHash = PasswordHasher.hash("admin123"),
                    role = UserRole.MAIN_ADMIN,
                    defaultAddress = "মিরপুর-১০, ঢাকা ১২১৬",
                    district = "Dhaka",
                    createdAt = now - 10_000_000L
                ),
                UserEntity(
                    id = seller1Id,
                    fullName = "করিম উদ্দিন (কৃষক)",
                    phone = "01711223344",
                    email = "sobuj@krishibazar.bd",
                    passwordHash = PasswordHasher.hash("seller123"),
                    role = UserRole.SELLER,
                    defaultAddress = "শেরপুর কৃষি বাজার, বগুড়া",
                    district = "Bogura",
                    createdAt = now - 9_000_000L
                ),
                UserEntity(
                    id = seller2Id,
                    fullName = "সালমা আক্তার",
                    phone = "01811223344",
                    email = "padma@krishibazar.bd",
                    passwordHash = PasswordHasher.hash("seller123"),
                    role = UserRole.SELLER,
                    defaultAddress = "পদ্মা ঘাট, মুন্সীগঞ্জ",
                    district = "Munshiganj",
                    createdAt = now - 8_000_000L
                ),
                UserEntity(
                    id = seller3Id,
                    fullName = "নজরুল ইসলাম",
                    phone = "01911223344",
                    email = "rangpur@krishibazar.bd",
                    passwordHash = PasswordHasher.hash("seller123"),
                    role = UserRole.SELLER,
                    defaultAddress = "পীরগঞ্জ, রংপুর",
                    district = "Rangpur",
                    createdAt = now - 2_000_000L
                ),
                UserEntity(
                    id = buyerId,
                    fullName = "রাকিব হাসান",
                    phone = "01755667788",
                    email = "rakib@krishibazar.bd",
                    passwordHash = PasswordHasher.hash("buyer123"),
                    role = UserRole.BUYER,
                    defaultAddress = "ধানমন্ডি-২৭, রোড নং ১১, ঢাকা",
                    district = "Dhaka",
                    createdAt = now - 7_000_000L
                )
            )
        )

        val shop1Id = "shop_sobuj_bangla"
        val shop2Id = "shop_padma_agro"
        val shop3Id = "shop_rangpur_harvest"

        dao.insertShops(
            listOf(
                ShopEntity(
                    id = shop1Id,
                    ownerUserId = seller1Id,
                    shopNameBn = "সবুজ বাংলা খামার",
                    shopNameEn = "Sobuj Bangla Farm",
                    district = "Bogura",
                    phone = "01711223344",
                    description = "বগুড়ার তাজা শাকসবজি, মিনিকেট চাল ও জৈব সার সরাসরি খামার থেকে।",
                    imageUri = "drawable://img_cat_vegetables",
                    approvalStatus = ApprovalStatus.APPROVED,
                    sellerCommissionAccepted = true,
                    createdAt = now - 8_500_000L
                ),
                ShopEntity(
                    id = shop2Id,
                    ownerUserId = seller2Id,
                    shopNameBn = "পদ্মা এগ্রো ও মৎস্য ভান্ডার",
                    shopNameEn = "Padma Agro & Fisheries",
                    district = "Munshiganj",
                    phone = "01811223344",
                    description = "তাজা রুই-ইলিশ মাছ, রাজশাহীর আম, খাঁটি দেশি মুরগি ও ডিম।",
                    imageUri = "drawable://img_cat_fish_fruits",
                    approvalStatus = ApprovalStatus.APPROVED,
                    sellerCommissionAccepted = true,
                    createdAt = now - 7_500_000L
                ),
                ShopEntity(
                    id = shop3Id,
                    ownerUserId = seller3Id,
                    shopNameBn = "রংপুর কৃষি ভান্ডার",
                    shopNameEn = "Rangpur Krishi Bhandar",
                    district = "Rangpur",
                    phone = "01911223344",
                    description = "রংপুরের হাড়িভাঙ্গা আম ও উন্নত মানের ডাল ও ধান বীজ।",
                    imageUri = "drawable://img_cat_rice_grains",
                    approvalStatus = ApprovalStatus.PENDING,
                    sellerCommissionAccepted = true,
                    createdAt = now - 1_500_000L
                )
            )
        )

        dao.insertProducts(
            listOf(
                ProductEntity(
                    id = "prod_1",
                    shopId = shop1Id,
                    sellerUserId = seller1Id,
                    nameBn = "তাজা লাল শাক ও পালং শাক বান্ডিল",
                    nameEn = "Fresh Red & Spinach Bundle",
                    category = ProductCategory.VEGETABLES,
                    priceBdt = 90.0,
                    unitLabel = "২ কেজি / 2 kg",
                    stockQuantity = 45,
                    description = "সকালবেলা ক্ষেত থেকে তোলা সম্পূর্ণ বিষমুক্ত তাজা শাকসবজি।",
                    imageUri = "drawable://img_cat_vegetables",
                    commissionPercentOverride = null,
                    sellerCommissionAccepted = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    featured = true,
                    createdAt = now - 6_000_000L
                ),
                ProductEntity(
                    id = "prod_2",
                    shopId = shop1Id,
                    sellerUserId = seller1Id,
                    nameBn = "প্রিমিয়াম বগুড়ার মিনিকেট চাল",
                    nameEn = "Premium Bogura Miniket Rice",
                    category = ProductCategory.RICE,
                    priceBdt = 380.0,
                    unitLabel = "৫ কেজি / 5 kg",
                    stockQuantity = 60,
                    description = "বাছাইকৃত সরু ও ঝরঝরে মিনিকেট চাল, কোনো কৃত্রিম পলিশ ছাড়া।",
                    imageUri = "drawable://img_cat_rice_grains",
                    commissionPercentOverride = 35.0,
                    sellerCommissionAccepted = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    featured = true,
                    createdAt = now - 5_500_000L
                ),
                ProductEntity(
                    id = "prod_3",
                    shopId = shop1Id,
                    sellerUserId = seller1Id,
                    nameBn = "দেশি মসুর ডাল (খোসা ছাড়া)",
                    nameEn = "Deshi Red Masoor Lentils",
                    category = ProductCategory.LENTILS,
                    priceBdt = 145.0,
                    unitLabel = "১ কেজি / 1 kg",
                    stockQuantity = 80,
                    description = "খাঁটি দেশি ছোট দানার মসুর ডাল, দ্রুত সেদ্ধ ও সুস্বাদু।",
                    imageUri = "drawable://img_cat_rice_grains",
                    commissionPercentOverride = null,
                    sellerCommissionAccepted = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    featured = false,
                    createdAt = now - 5_000_000L
                ),
                ProductEntity(
                    id = "prod_4",
                    shopId = shop2Id,
                    sellerUserId = seller2Id,
                    nameBn = "রাজশাহীর হিমসাগর আম",
                    nameEn = "Rajshahi Himsagar Mango",
                    category = ProductCategory.FRUITS,
                    priceBdt = 450.0,
                    unitLabel = "৩ কেজি / 3 kg",
                    stockQuantity = 30,
                    description = "ফরমালিন ও কার্বাইড মুক্ত গাছপাকা সুমিষ্ট হিমসাগর আম।",
                    imageUri = "drawable://img_cat_fish_fruits",
                    commissionPercentOverride = 40.0,
                    sellerCommissionAccepted = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    featured = true,
                    createdAt = now - 4_500_000L
                ),
                ProductEntity(
                    id = "prod_5",
                    shopId = shop2Id,
                    sellerUserId = seller2Id,
                    nameBn = "পদ্মার তাজা রুই মাছ",
                    nameEn = "Fresh Padma Rui Fish",
                    category = ProductCategory.FISH,
                    priceBdt = 620.0,
                    unitLabel = "১.৫ কেজি / 1.5 kg",
                    stockQuantity = 18,
                    description = "বরফ দিয়ে সংরক্ষিত নদীর তাজা রুই মাছ, পরিষ্কার করে কাটা।",
                    imageUri = "drawable://img_cat_fish_fruits",
                    commissionPercentOverride = null,
                    sellerCommissionAccepted = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    featured = true,
                    createdAt = now - 4_000_000L
                ),
                ProductEntity(
                    id = "prod_6",
                    shopId = shop2Id,
                    sellerUserId = seller2Id,
                    nameBn = "খাঁটি দেশি গরুর মাংস",
                    nameEn = "Fresh Deshi Beef",
                    category = ProductCategory.MEAT,
                    priceBdt = 780.0,
                    unitLabel = "১ কেজি / 1 kg",
                    stockQuantity = 22,
                    description = "হালাল ও স্বাস্থ্যসম্মত উপায়ে প্রস্তুত হাড়-মাংস মিক্স।",
                    imageUri = "drawable://img_cat_fish_fruits",
                    commissionPercentOverride = 30.0,
                    sellerCommissionAccepted = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    featured = false,
                    createdAt = now - 3_500_000L
                ),
                ProductEntity(
                    id = "prod_7",
                    shopId = shop2Id,
                    sellerUserId = seller2Id,
                    nameBn = "দেশি মুরগি ও লাল ডিম কম্বো",
                    nameEn = "Deshi Chicken & Farm Eggs Combo",
                    category = ProductCategory.POULTRY_EGGS,
                    priceBdt = 540.0,
                    unitLabel = "১টি + ১২ ডিম",
                    stockQuantity = 25,
                    description = "১ কেজি ওজনের দেশি মুরগি এবং ১ ডজন তাজা ফার্মের ডিম।",
                    imageUri = "drawable://img_cat_fish_fruits",
                    commissionPercentOverride = null,
                    sellerCommissionAccepted = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    featured = false,
                    createdAt = now - 3_000_000L
                ),
                ProductEntity(
                    id = "prod_8",
                    shopId = shop1Id,
                    sellerUserId = seller1Id,
                    nameBn = "জৈব ভার্মি কম্পোস্ট ও সবজি বীজ",
                    nameEn = "Organic Vermicompost & Seed Pack",
                    category = ProductCategory.AGRI_GOODS,
                    priceBdt = 260.0,
                    unitLabel = "৫ কেজি প্যাক",
                    stockQuantity = 50,
                    description = "ছাদবাগান ও জমির জন্য কেঁচো সার সাথে ৬ ধরনের সবজি বীজ ফ্রি।",
                    imageUri = "drawable://img_cat_vegetables",
                    commissionPercentOverride = 25.0,
                    sellerCommissionAccepted = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    featured = false,
                    createdAt = now - 2_500_000L
                ),
                ProductEntity(
                    id = "prod_9",
                    shopId = shop1Id,
                    sellerUserId = seller1Id,
                    nameBn = "সুন্দরবনের খাঁটি মধু",
                    nameEn = "Pure Sundarbans Natural Honey",
                    category = ProductCategory.OTHERS,
                    priceBdt = 650.0,
                    unitLabel = "৫০০ গ্রাম / 500 g",
                    stockQuantity = 16,
                    description = "১০০% খাঁটি প্রাকৃতিক মধু, কোনো চিনি বা ভেজাল নেই।",
                    imageUri = "drawable://img_hero_krishi_banner",
                    commissionPercentOverride = null,
                    sellerCommissionAccepted = true,
                    approvalStatus = ApprovalStatus.APPROVED,
                    featured = false,
                    createdAt = now - 1_800_000L
                ),
                ProductEntity(
                    id = "prod_10",
                    shopId = shop1Id,
                    sellerUserId = seller1Id,
                    nameBn = "কাঁচা মরিচ ও দেশি পেঁয়াজ (নতুন ফসল)",
                    nameEn = "Green Chili & Deshi Onion Pack",
                    category = ProductCategory.VEGETABLES,
                    priceBdt = 190.0,
                    unitLabel = "২ কেজি / 2 kg",
                    stockQuantity = 40,
                    description = "বগুড়ার নতুন মৌসুমের ঝাল কাঁচা মরিচ ও দেশি পেঁয়াজ।",
                    imageUri = "drawable://img_cat_vegetables",
                    commissionPercentOverride = 45.0,
                    sellerCommissionAccepted = true,
                    approvalStatus = ApprovalStatus.PENDING,
                    featured = false,
                    createdAt = now - 600_000L
                )
            )
        )

        // Seed 2 realistic orders: 1 COMPLETED (realized 50% / 35% commission) and 1 SHIPPED
        dao.insertOrders(
            listOf(
                OrderEntity(
                    id = "KB-1001",
                    buyerUserId = buyerId,
                    buyerName = "রাকিব হাসান",
                    buyerPhone = "01755667788",
                    deliveryAddress = "ধানমন্ডি-২৭, রোড নং ১১, ঢাকা",
                    district = "Dhaka",
                    shopId = shop1Id,
                    sellerUserId = seller1Id,
                    deliveryMethod = DeliveryMethod.COURIER_DELIVERY,
                    deliveryCharge = 110.0,
                    paymentMethod = PaymentMethod.BKASH,
                    paymentStatus = PaymentStatus.VERIFIED,
                    transactionId = "BKS892341KL",
                    senderAccount = "01755667788",
                    orderStatus = OrderStatus.COMPLETED,
                    grossSubtotal = 560.0,
                    totalPayable = 670.0,
                    agreedCommissionTotal = 223.0,
                    realizedCommissionAmount = 223.0,
                    sellerNetPayout = 337.0,
                    commissionStatus = CommissionStatus.EARNED,
                    trackingNote = "ক্রেতা পণ্য বুঝে পেয়েছেন",
                    createdAt = now - 4_000_000L,
                    updatedAt = now - 2_000_000L
                ),
                OrderEntity(
                    id = "KB-1002",
                    buyerUserId = buyerId,
                    buyerName = "রাকিব হাসান",
                    buyerPhone = "01755667788",
                    deliveryAddress = "ধানমন্ডি-২৭, রোড নং ১১, ঢাকা",
                    district = "Dhaka",
                    shopId = shop2Id,
                    sellerUserId = seller2Id,
                    deliveryMethod = DeliveryMethod.SELLER_DELIVERY,
                    deliveryCharge = 60.0,
                    paymentMethod = PaymentMethod.NAGAD,
                    paymentStatus = PaymentStatus.PENDING_VERIFICATION,
                    transactionId = "NGD441209MN",
                    senderAccount = "01755667788",
                    orderStatus = OrderStatus.DELIVERED,
                    grossSubtotal = 450.0,
                    totalPayable = 510.0,
                    agreedCommissionTotal = 180.0,
                    realizedCommissionAmount = 0.0,
                    sellerNetPayout = 0.0,
                    commissionStatus = CommissionStatus.PENDING,
                    trackingNote = "ডেলিভারি পৌঁছেছে — ক্রেতার নিশ্চিতকরণের অপেক্ষায়",
                    createdAt = now - 1_200_000L,
                    updatedAt = now - 300_000L
                )
            )
        )

        dao.insertOrderItems(
            listOf(
                OrderItemEntity(
                    orderId = "KB-1001",
                    productId = "prod_1",
                    productNameBn = "তাজা লাল শাক ও পালং শাক বান্ডিল",
                    productNameEn = "Fresh Red & Spinach Bundle",
                    unitPrice = 90.0,
                    quantity = 2,
                    appliedCommissionPercent = 50.0,
                    lineSubtotal = 180.0,
                    lineCommissionAmount = 90.0
                ),
                OrderItemEntity(
                    orderId = "KB-1001",
                    productId = "prod_2",
                    productNameBn = "প্রিমিয়াম বগুড়ার মিনিকেট চাল",
                    productNameEn = "Premium Bogura Miniket Rice",
                    unitPrice = 380.0,
                    quantity = 1,
                    appliedCommissionPercent = 35.0,
                    lineSubtotal = 380.0,
                    lineCommissionAmount = 133.0
                ),
                OrderItemEntity(
                    orderId = "KB-1002",
                    productId = "prod_4",
                    productNameBn = "রাজশাহীর হিমসাগর আম",
                    productNameEn = "Rajshahi Himsagar Mango",
                    unitPrice = 450.0,
                    quantity = 1,
                    appliedCommissionPercent = 40.0,
                    lineSubtotal = 450.0,
                    lineCommissionAmount = 180.0
                )
            )
        )

        dao.insertReviews(
            listOf(
                ReviewEntity(
                    productId = "prod_1",
                    buyerUserId = buyerId,
                    buyerName = "রাকিব হাসান",
                    rating = 5,
                    comment = "খুবই তাজা শাকসবজি, সময়মতো ডেলিভারি পেয়েছি!",
                    createdAt = now - 1_500_000L
                ),
                ReviewEntity(
                    productId = "prod_2",
                    buyerUserId = buyerId,
                    buyerName = "রাকিব হাসান",
                    rating = 5,
                    comment = "মিনিকেট চালের মান দারুণ, ভাত ঝরঝরে হয়।",
                    createdAt = now - 1_400_000L
                )
            )
        )

        dao.insertWithdrawal(
            WithdrawalEntity(
                id = "WD-901",
                sellerUserId = seller1Id,
                shopId = shop1Id,
                shopName = "সবুজ বাংলা খামার",
                amountBdt = 200.0,
                payoutMethod = PaymentMethod.BKASH,
                payoutAccount = "01711223344",
                status = WithdrawalStatus.PENDING,
                requestedAt = now - 500_000L
            )
        )
        return true
    }
}
