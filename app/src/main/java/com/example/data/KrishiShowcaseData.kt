package com.example.data

import com.example.model.AppLanguage
import com.example.model.ApprovalStatus
import com.example.model.CommissionCalculator
import com.example.model.PlatformSettingsEntity
import com.example.model.ProductCategory
import com.example.model.ProductEntity
import com.example.model.ProductWithShop
import com.example.model.ReviewEntity
import com.example.model.ShopEntity
import com.example.viewmodel.KrishiUiState

/**
 * Pure static showcase catalog shown when no Firebase user is signed in yet,
 * so anyone can always enter and browse the marketplace without executing
 * unauthenticated Firestore queries.
 */
object KrishiShowcaseData {
    val settings = PlatformSettingsEntity()

    val shops: List<ShopEntity> = listOf(
        ShopEntity(
            id = "shop_sobuj_bangla",
            ownerUserId = "showcase_1",
            shopNameBn = "সবুজ বাংলা খামার",
            shopNameEn = "Sobuj Bangla Farm",
            district = "Bogura",
            phone = "",
            description = "বগুড়ার তাজা শাকসবজি, মিনিকেট চাল ও জৈব সার সরাসরি খামার থেকে।",
            imageUri = "drawable://img_cat_vegetables",
            approvalStatus = ApprovalStatus.APPROVED,
            sellerCommissionAccepted = true
        ),
        ShopEntity(
            id = "shop_padma_agro",
            ownerUserId = "showcase_2",
            shopNameBn = "পদ্মা এগ্রো ও মৎস্য ভান্ডার",
            shopNameEn = "Padma Agro & Fisheries",
            district = "Munshiganj",
            phone = "",
            description = "তাজা রুই-ইলিশ মাছ, রাজশাহীর আম, খাঁটি দেশি মুরগি ও ডিম।",
            imageUri = "drawable://img_cat_fish_fruits",
            approvalStatus = ApprovalStatus.APPROVED,
            sellerCommissionAccepted = true
        )
    )

    val products: List<ProductEntity> = listOf(
        ProductEntity(
            id = "prod_1",
            shopId = "shop_sobuj_bangla",
            sellerUserId = "showcase_1",
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
            featured = true
        ),
        ProductEntity(
            id = "prod_2",
            shopId = "shop_sobuj_bangla",
            sellerUserId = "showcase_1",
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
            featured = true
        ),
        ProductEntity(
            id = "prod_3",
            shopId = "shop_sobuj_bangla",
            sellerUserId = "showcase_1",
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
            featured = false
        ),
        ProductEntity(
            id = "prod_4",
            shopId = "shop_padma_agro",
            sellerUserId = "showcase_2",
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
            featured = true
        ),
        ProductEntity(
            id = "prod_5",
            shopId = "shop_padma_agro",
            sellerUserId = "showcase_2",
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
            featured = true
        ),
        ProductEntity(
            id = "prod_6",
            shopId = "shop_padma_agro",
            sellerUserId = "showcase_2",
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
            featured = false
        ),
        ProductEntity(
            id = "prod_7",
            shopId = "shop_padma_agro",
            sellerUserId = "showcase_2",
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
            featured = false
        ),
        ProductEntity(
            id = "prod_8",
            shopId = "shop_sobuj_bangla",
            sellerUserId = "showcase_1",
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
            featured = false
        ),
        ProductEntity(
            id = "prod_9",
            shopId = "shop_sobuj_bangla",
            sellerUserId = "showcase_1",
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
            featured = false
        )
    )

    val reviews: List<ReviewEntity> = listOf(
        ReviewEntity(
            id = 1L,
            productId = "prod_1",
            buyerUserId = "showcase_buyer",
            buyerName = "রাকিব হাসান",
            rating = 5,
            comment = "খুবই তাজা শাকসবজি, সময়মতো ডেলিভারি পেয়েছি!"
        ),
        ReviewEntity(
            id = 2L,
            productId = "prod_2",
            buyerUserId = "showcase_buyer",
            buyerName = "রাকিব হাসান",
            rating = 5,
            comment = "মিনিকেট চালের মান দারুণ, ভাত ঝরঝরে হয়।"
        )
    )

    fun buildGuestUiState(
        language: AppLanguage,
        searchQuery: String,
        selectedCategory: ProductCategory?
    ): KrishiUiState {
        val shopsMap = shops.associateBy { it.id }
        val reviewsMap = reviews.groupBy { it.productId }
        val enriched = products.map { prod ->
            val revs = reviewsMap[prod.id].orEmpty()
            ProductWithShop(
                product = prod,
                shop = shopsMap[prod.shopId],
                effectiveCommissionPercent = CommissionCalculator.effectiveRate(prod.commissionPercentOverride, settings),
                averageRating = if (revs.isEmpty()) 0.0 else revs.map { it.rating }.average(),
                reviewCount = revs.size
            )
        }
        val q = searchQuery.trim().lowercase()
        val filtered = enriched.filter { pws ->
            val catOk = selectedCategory == null || pws.product.category == selectedCategory
            val qOk = q.isEmpty() ||
                pws.product.nameBn.lowercase().contains(q) ||
                pws.product.nameEn.lowercase().contains(q) ||
                (pws.shop?.shopNameBn?.lowercase()?.contains(q) == true) ||
                (pws.shop?.shopNameEn?.lowercase()?.contains(q) == true)
            catOk && qOk
        }
        return KrishiUiState(
            isReady = true,
            language = language,
            currentUser = null,
            settings = settings,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            allShops = shops,
            approvedShops = shops,
            allProducts = enriched,
            approvedProducts = filtered,
            featuredProducts = enriched.filter { it.product.featured },
            allReviews = reviews
        )
    }
}
