package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.ApprovalStatus
import com.example.model.CartItemEntity
import com.example.model.OrderEntity
import com.example.model.OrderItemEntity
import com.example.model.PlatformSettingsEntity
import com.example.model.ProductEntity
import com.example.model.ReviewEntity
import com.example.model.ShopEntity
import com.example.model.UserEntity
import com.example.model.WithdrawalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KrishiDao {
    // Users
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:identifier) OR phone = :identifier LIMIT 1")
    suspend fun findUserByEmailOrPhone(identifier: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("UPDATE users SET isBlocked = :blocked WHERE id = :userId")
    suspend fun setUserBlocked(userId: String, blocked: Boolean)

    @Query("UPDATE users SET defaultAddress = :address, district = :district WHERE id = :userId")
    suspend fun updateUserAddress(userId: String, address: String, district: String)

    // Shops
    @Query("SELECT * FROM shops ORDER BY createdAt DESC")
    fun observeAllShops(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE ownerUserId = :userId LIMIT 1")
    fun observeShopByOwner(userId: String): Flow<ShopEntity?>

    @Query("SELECT * FROM shops WHERE id = :shopId LIMIT 1")
    suspend fun getShopById(shopId: String): ShopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertShops(shops: List<ShopEntity>)

    @Query("UPDATE shops SET approvalStatus = :status WHERE id = :shopId")
    suspend fun updateShopApproval(shopId: String, status: ApprovalStatus)

    // Products
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQuantity = :stock WHERE id = :productId")
    suspend fun updateProductStock(productId: String, stock: Int)

    @Query("UPDATE products SET approvalStatus = :status WHERE id = :productId")
    suspend fun updateProductApproval(productId: String, status: ApprovalStatus)

    @Query("UPDATE products SET featured = :featured WHERE id = :productId")
    suspend fun updateProductFeatured(productId: String, featured: Boolean)

    // Cart
    @Query("SELECT * FROM cart_items WHERE buyerUserId = :buyerUserId ORDER BY id DESC")
    fun observeCartItems(buyerUserId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE buyerUserId = :buyerUserId AND productId = :productId LIMIT 1")
    suspend fun getCartItem(buyerUserId: String, productId: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE id = :cartItemId")
    suspend fun updateCartQuantity(cartItemId: Long, quantity: Int)

    @Query("DELETE FROM cart_items WHERE id = :cartItemId")
    suspend fun deleteCartItem(cartItemId: Long)

    @Query("DELETE FROM cart_items WHERE buyerUserId = :buyerUserId")
    suspend fun clearCart(buyerUserId: String)

    // Orders & Order Items
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM order_items")
    fun observeAllOrderItems(): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    // Reviews
    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    fun observeAllReviews(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReviews(reviews: List<ReviewEntity>)

    // Withdrawals
    @Query("SELECT * FROM withdrawals ORDER BY requestedAt DESC")
    fun observeAllWithdrawals(): Flow<List<WithdrawalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: WithdrawalEntity)

    @Update
    suspend fun updateWithdrawal(withdrawal: WithdrawalEntity)

    // Platform Settings
    @Query("SELECT * FROM platform_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<PlatformSettingsEntity?>

    @Query("SELECT * FROM platform_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): PlatformSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: PlatformSettingsEntity)

    // Counts for seed check
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}
