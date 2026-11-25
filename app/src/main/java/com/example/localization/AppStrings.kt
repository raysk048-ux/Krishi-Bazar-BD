package com.example.localization

import com.example.model.AppLanguage

object AppStrings {
    fun appTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "কৃষি বাজার বিডি" else "Krishi Bazar BD"
    fun appSubtitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "বাংলাদেশের ডিজিটাল কৃষি মার্কেটপ্লেস" else "Bangladesh Multi-Vendor Agri Marketplace"

    // Navigation Tabs
    fun navHome(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "হোম" else "Home"
    fun navCart(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "কার্ট" else "Cart"
    fun navOrders(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "অর্ডার" else "Orders"
    fun navSeller(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "দোকান" else "Seller"
    fun navAdmin(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "অ্যাডমিন" else "Admin"
    fun navProfile(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "প্রোফাইল" else "Profile"

    // Home
    fun searchHint(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "তাজা শাকসবজি, চাল, মাছ বা দোকান খুঁজুন..." else "Search fresh vegetables, rice, fish or shops..."
    fun categories(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পণ্যের ক্যাটাগরি" else "Categories"
    fun allCategories(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "সবগুলো" else "All"
    fun featuredProducts(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ফিচার্ড পণ্য" else "Featured Products"
    fun newProducts(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "নতুন পণ্য" else "New Harvest Arrivals"
    fun sellerShops(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "অনুমোদিত বিক্রেতার দোকান" else "Verified Seller Shops"
    fun addToCart(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "কার্টে যোগ করুন" else "Add to Cart"
    fun outOfStock(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "স্টক শেষ" else "Out of Stock"
    fun inStock(lang: AppLanguage, qty: Int) = if (lang == AppLanguage.BANGLA) "স্টক: $qty" else "Stock: $qty"
    fun noProductsFound(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "কোনো পণ্য পাওয়া যায়নি" else "No matching products found"

    // Cart & Checkout
    fun emptyCart(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "আপনার কার্ট খালি রয়েছে" else "Your shopping cart is empty"
    fun deliveryAddress(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ডেলিভারি ঠিকানা" else "Delivery Address"
    fun deliveryOption(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ডেলিভারি মাধ্যম" else "Delivery Option"
    fun paymentMethod(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পেমেন্ট মাধ্যম" else "Payment Method"
    fun trxIdLabel(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ট্রানজেকশন আইডি (TrxID)" else "Transaction ID (TrxID)"
    fun senderPhoneLabel(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পেমেন্ট নম্বর (০১XXXXXXXXX)" else "Sender Mobile (01XXXXXXXXX)"
    fun placeOrder(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "অর্ডার নিশ্চিত করুন" else "Place Order"
    fun subtotal(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পণ্যের মূল্য" else "Subtotal"
    fun deliveryCharge(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ডেলিভারি চার্জ" else "Delivery Charge"
    fun totalPayable(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "সর্বমোট মূল্য" else "Total Payable"

    // Orders
    fun confirmDelivery(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ডেলিভারি নিশ্চিত করুন" else "Confirm Delivery"
    fun cancelOrder(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "অর্ডার বাতিল" else "Cancel Order"
    fun requestRefund(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "রিফান্ড আবেদন" else "Request Refund"
    fun writeReview(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "রিভিউ দিন" else "Write Review"
    fun emptyOrders(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "এখনো কোনো অর্ডার নেই" else "No orders yet"

    // Seller
    fun registerShopTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "আপনার বিক্রেতা দোকান খুলুন" else "Create Your Seller Shop"
    fun addProduct(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "নতুন পণ্য যোগ করুন" else "Add New Product"
    fun requestWithdrawal(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "টাকা উত্তোলনের আবেদন" else "Request Withdrawal"
    fun sellerConsentText(lang: AppLanguage, rate: Double) = if (lang == AppLanguage.BANGLA) {
        "আমি এই পণ্যে ${rate.toInt()}% প্ল্যাটফর্ম কমিশন মেনে নিচ্ছি (শুধু সম্পন্ন অর্ডারে প্রযোজ্য)।"
    } else {
        "I agree to the ${rate.toInt()}% platform commission on completed orders for this product."
    }

    // Legal / Policies
    fun privacyPolicyTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "প্রাইভেসি পলিসি" else "Privacy Policy"
    fun termsTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "শর্তাবলী ও নীতিমালা" else "Terms & Conditions"

    fun privacyPolicyBody(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) {
        """
        ১. তথ্য সংগ্রহ: কৃষি বাজার বিডি শুধু অর্ডার ডেলিভারি, বিক্রেতা যাচাই ও পেমেন্ট সমন্বয়ের জন্য আপনার নাম, মোবাইল নম্বর এবং ঠিকানা সংরক্ষণ করে।
        ২. পেমেন্ট সুরক্ষা: বিকাশ, নগদ ও রকেট ট্রানজেকশন আইডি কেবল পেমেন্ট যাচাইয়ের কাজে ব্যবহৃত হয়।
        ৩. তথ্যের গোপনীয়তা: আপনার ব্যক্তিগত তথ্য কোনো তৃতীয় পক্ষের কাছে বিক্রি বা শেয়ার করা হয় না।
        ৪. ডাটা ব্যাকআপ: প্রধান অ্যাডমিন প্রয়োজনীয় রিপোর্ট ও অর্ডার ইতিহাস নিরাপদভাবে ব্যাকআপ রাখতে পারেন।
        """.trimIndent()
    } else {
        """
        1. Data Collection: Krishi Bazar BD stores your name, mobile number, and delivery address strictly to fulfill marketplace orders, verify sellers, and process payments.
        2. Payment Verification: bKash, Nagad, and Rocket transaction IDs are used solely to verify payment settlement.
        3. Privacy Guarantee: Personal details are never sold or shared with unauthorized third parties.
        4. Backup & Retention: Order histories and commission ledgers are protected under role-based access controls and admin backup exports.
        """.trimIndent()
    }

    fun termsBody(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) {
        """
        ১. কমিশন নীতিমালা: প্ল্যাটফর্মের ডিফল্ট কমিশন ৫০% (প্রধান অ্যাডমিন সর্বনিম্ন ও সর্বোচ্চ সীমার মধ্যে পরিবর্তন করতে পারবেন)। শুধু সম্পন্ন (Completed) অর্ডারে কমিশন গণনা হবে।
        ২. বাতিল ও রিফান্ড: অর্ডার বাতিল বা রিফান্ড হলে উক্ত অর্ডারের কমিশন স্বয়ংক্রিয়ভাবে সমন্বয় (৳০) করা হবে।
        ৩. বিক্রেতার দায়বদ্ধতা: বিক্রেতাকে সঠিক ওজন, তাজা পণ্য এবং অনুমোদিত স্টক বজায় রাখতে হবে।
        ৪. অনুমোদন: সকল নতুন দোকান ও পণ্য প্রধান অ্যাডমিনের অনুমোদনের পর ক্রেতাদের জন্য প্রকাশিত হবে।
        """.trimIndent()
    } else {
        """
        1. Commission Policy: Default platform commission is 50%, adjustable within Admin-defined min/max bounds with seller consent. Commission applies exclusively to Completed orders.
        2. Cancellations & Refunds: Cancelled or refunded orders automatically reverse platform commission to ৳0.
        3. Seller Quality: Sellers must supply fresh agricultural produce matching listed weights, prices, and stock counts.
        4. Moderation: All new seller shops and products require Main Admin approval before going live to buyers.
        """.trimIndent()
    }
}
