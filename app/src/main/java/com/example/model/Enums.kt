package com.example.model

enum class AppLanguage(val code: String, val label: String) {
    BANGLA("bn", "বাংলা"),
    ENGLISH("en", "English")
}

enum class UserRole(val titleBn: String, val titleEn: String) {
    MAIN_ADMIN("প্রধান অ্যাডমিন", "Main Admin"),
    SELLER("বিক্রেতা", "Seller"),
    BUYER("ক্রেতা", "Buyer")
}

enum class ProductCategory(
    val titleBn: String,
    val titleEn: String,
    val defaultDrawableKey: String
) {
    VEGETABLES("শাকসবজি", "Vegetables", "drawable://img_cat_vegetables"),
    RICE("চাল", "Rice", "drawable://img_cat_rice_grains"),
    LENTILS("ডাল", "Lentils", "drawable://img_cat_rice_grains"),
    FRUITS("ফলমূল", "Fruits", "drawable://img_cat_fish_fruits"),
    FISH("মাছ", "Fish", "drawable://img_cat_fish_fruits"),
    MEAT("মাংস", "Meat", "drawable://img_cat_fish_fruits"),
    POULTRY_EGGS("মুরগি ও ডিম", "Poultry & Eggs", "drawable://img_cat_fish_fruits"),
    AGRI_GOODS("কৃষিপণ্য", "Agri Goods", "drawable://img_cat_vegetables"),
    OTHERS("অন্যান্য", "Others", "drawable://img_hero_krishi_banner")
}

enum class ApprovalStatus(val titleBn: String, val titleEn: String) {
    PENDING("অপেক্ষমান", "Pending"),
    APPROVED("অনুমোদিত", "Approved"),
    REJECTED("বাতিল", "Rejected")
}

enum class OrderStatus(val titleBn: String, val titleEn: String) {
    PLACED("অর্ডার গৃহীত", "Placed"),
    CONFIRMED("নিশ্চিত", "Confirmed"),
    PACKED("প্যাকেটজাত", "Packed"),
    SHIPPED("পথে আছে", "Shipped"),
    DELIVERED("ডেলিভারি হয়েছে", "Delivered"),
    COMPLETED("সম্পন্ন", "Completed"),
    CANCELLED("বাতিল", "Cancelled"),
    REFUND_REQUESTED("রিফান্ড আবেদন", "Refund Requested"),
    REFUNDED("রিফান্ড সম্পন্ন", "Refunded")
}

enum class PaymentMethod(val titleBn: String, val titleEn: String, val requiresTrxId: Boolean) {
    BKASH("বিকাশ (bKash)", "bKash", true),
    NAGAD("নগদ (Nagad)", "Nagad", true),
    ROCKET("রকেট (Rocket)", "Rocket", true),
    COD("ক্যাশ অন ডেলিভারি", "Cash on Delivery", false)
}

enum class PaymentStatus(val titleBn: String, val titleEn: String) {
    PENDING_VERIFICATION("যাচাই অপেক্ষমান", "Pending Verification"),
    VERIFIED("যাচাইকৃত", "Verified"),
    REFUNDED("রিফান্ডকৃত", "Refunded")
}

enum class DeliveryMethod(val titleBn: String, val titleEn: String) {
    SELLER_DELIVERY("বিক্রেতা ডেলিভারি", "Seller Delivery"),
    COURIER_DELIVERY("কুরিয়ার ডেলিভারি", "Courier Delivery")
}

enum class CommissionStatus(val titleBn: String, val titleEn: String) {
    PENDING("অপেক্ষমান", "Pending Completion"),
    EARNED("অর্জিত", "Earned"),
    ADJUSTED_REVERSED("বাতিল/সমন্বিত", "Adjusted (৳0)")
}

enum class WithdrawalStatus(val titleBn: String, val titleEn: String) {
    PENDING("অনুমোদন অপেক্ষমান", "Pending Approval"),
    APPROVED("পরিশোধিত", "Approved & Paid"),
    REJECTED("প্রত্যাখ্যাত", "Rejected")
}
