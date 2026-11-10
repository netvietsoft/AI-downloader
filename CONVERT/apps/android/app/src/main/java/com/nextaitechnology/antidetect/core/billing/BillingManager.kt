package com.nextaitechnology.antidetect.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.nextaitechnology.antidetect.R
import com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager
import com.nextaitechnology.antidetect.feature.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Trình Quản Lý Thanh Toán & Đăng Ký Thuê Bao Google Play (Google Play Billing Manager)
 * Full integration with Google Play Billing Library v8.0.0.
 * - Single-flight deduplicated product queries (prevents concurrent IPC race conditions).
 * - Canonical Base Plan & recurring PricingPhase selection so Offers Page and Payment Cart
 *   always share the exact same offerToken, price, and currency.
 * - Automatic retry with exponential backoff on cold start or service disconnect.
 *
 * @author NextAI Technology Core Team
 */
class BillingManager private constructor(private val appContext: Context) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val settingsRepository = SettingsRepository(appContext)

    companion object {
        private const val TAG = "BillingManager"

        // SKU IDs đồng bộ với Google Play Console
        const val SKU_WEEKLY = "weekly_pro"
        const val SKU_YEARLY = "yearly_pro"
        const val SKU_LIFETIME = "lifetime_pro"

        @Volatile
        private var instance: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

    private val _isLoadingProducts = MutableStateFlow(true)
    val isLoadingProducts: StateFlow<Boolean> = _isLoadingProducts.asStateFlow()

    @Volatile
    private var isConnecting = false
    private val pendingReadyCallbacks = mutableListOf<() -> Unit>()

    @Volatile
    private var isQueryingProducts = false
    @Volatile
    private var hasCompletedInitialQuery = false
    private val pendingQueryCallbacks = mutableListOf<(Map<String, ProductDetails>) -> Unit>()

    @Volatile
    private var retryCount = 0
    private val maxAutoRetries = 3

    private var onPurchaseResultListener: ((Boolean, String?) -> Unit)? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        startConnection()
    }

    /**
     * Khởi tạo kết nối với Google Play Billing Services (Thread-safe queue)
     */
    fun startConnection(onReady: (() -> Unit)? = null) {
        synchronized(this) {
            if (billingClient.isReady) {
                _isBillingReady.value = true
                if (onReady != null) {
                    onReady.invoke()
                } else if (_productDetailsMap.value.size < 3) {
                    queryAvailableProducts()
                }
                return
            }

            if (onReady != null) {
                pendingReadyCallbacks.add(onReady)
            }

            if (isConnecting) {
                Log.d(TAG, "BillingClient is already connecting, queued onReady callback.")
                return
            }

            isConnecting = true
        }

        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    val callbacks: List<() -> Unit>
                    synchronized(this@BillingManager) {
                        isConnecting = false
                        callbacks = ArrayList(pendingReadyCallbacks)
                        pendingReadyCallbacks.clear()
                    }

                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.i(TAG, "Google Play Billing setup successful.")
                        _isBillingReady.value = true
                        queryActivePurchases()
                        if (callbacks.isNotEmpty()) {
                            callbacks.forEach { it.invoke() }
                        } else {
                            queryAvailableProducts()
                        }
                    } else {
                        Log.w(TAG, "Billing setup failed: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                        _isBillingReady.value = false
                        _isLoadingProducts.value = false
                        callbacks.forEach { it.invoke() }
                        scheduleAutoRetryIfNeeded()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    synchronized(this@BillingManager) {
                        isConnecting = false
                    }
                    Log.w(TAG, "Billing service disconnected. Scheduling automatic reconnect...")
                    _isBillingReady.value = false
                    scheduleAutoRetryIfNeeded()
                }
            })
        } catch (e: Exception) {
            synchronized(this) {
                isConnecting = false
            }
            Log.e(TAG, "Exception starting BillingClient connection", e)
            _isLoadingProducts.value = false
        }
    }

    private fun scheduleAutoRetryIfNeeded() {
        if (retryCount >= maxAutoRetries || _productDetailsMap.value.size >= 3) return
        retryCount++
        val delayMs = 800L * retryCount
        scope.launch {
            delay(delayMs)
            if (!billingClient.isReady || _productDetailsMap.value.size < 3) {
                Log.i(TAG, "Auto-retrying Billing connection & product query (attempt $retryCount/$maxAutoRetries)...")
                queryAvailableProducts()
            }
        }
    }

    /**
     * Truy vấn thông tin sản phẩm và giá/tiền tệ trực tiếp từ Google Play Store (Single-Flight Deduplicated).
     * Lưu ý quan trọng: BillingClient 8.0.0 cấm kết hợp SUBS và INAPP trong cùng 1 QueryProductDetailsParams.
     * Do đó phải chia thành các truy vấn riêng biệt theo từng ProductType và hợp nhất kết quả.
     */
    fun queryAvailableProducts(onComplete: ((Map<String, ProductDetails>) -> Unit)? = null) {
        if (!billingClient.isReady) {
            if (!hasCompletedInitialQuery) {
                _isLoadingProducts.value = true
            }
            startConnection {
                if (billingClient.isReady) {
                    queryAvailableProducts(onComplete)
                } else {
                    hasCompletedInitialQuery = true
                    _isLoadingProducts.value = false
                    onComplete?.invoke(_productDetailsMap.value)
                }
            }
            return
        }

        synchronized(this) {
            if (onComplete != null) {
                pendingQueryCallbacks.add(onComplete)
            }
            if (isQueryingProducts) {
                Log.d(TAG, "Product query already in flight, coalescing callback.")
                return
            }
            isQueryingProducts = true
            if (!hasCompletedInitialQuery) {
                _isLoadingProducts.value = true
            }
        }

        try {
            val subsProductList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SKU_WEEKLY)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SKU_YEARLY)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

            val inAppProductList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(SKU_LIFETIME)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )

            val subsParams = QueryProductDetailsParams.newBuilder()
                .setProductList(subsProductList)
                .build()

            val inAppParams = QueryProductDetailsParams.newBuilder()
                .setProductList(inAppProductList)
                .build()

            val mergedMap = HashMap<String, ProductDetails>(_productDetailsMap.value)

            // 1. Truy vấn các gói thuê bao tuần & năm (SUBS)
            billingClient.queryProductDetailsAsync(subsParams) { subsBillingResult, subsDetailsResult ->
                if (subsBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val list = subsDetailsResult.productDetailsList
                    if (!list.isNullOrEmpty()) {
                        synchronized(this@BillingManager) {
                            for (item in list) {
                                mergedMap[item.productId] = item
                                Log.i(TAG, "Loaded SUBS SKU: ${item.productId}, price=${extractFormattedPrice(item)}")
                            }
                            _productDetailsMap.value = HashMap(mergedMap)
                        }
                    }
                } else {
                    Log.w(TAG, "queryProductDetailsAsync SUBS failed: ${subsBillingResult.responseCode} - ${subsBillingResult.debugMessage}")
                }

                // 2. Truy vấn gói trọn đời (INAPP)
                billingClient.queryProductDetailsAsync(inAppParams) { inAppBillingResult, inAppDetailsResult ->
                    if (inAppBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val inAppList = inAppDetailsResult.productDetailsList
                        if (!inAppList.isNullOrEmpty()) {
                            synchronized(this@BillingManager) {
                                for (item in inAppList) {
                                    mergedMap[item.productId] = item
                                    Log.i(TAG, "Loaded INAPP SKU: ${item.productId}, price=${extractFormattedPrice(item)}")
                                }
                                _productDetailsMap.value = HashMap(mergedMap)
                            }
                        }
                    } else {
                        Log.w(TAG, "queryProductDetailsAsync INAPP failed: ${inAppBillingResult.responseCode} - ${inAppBillingResult.debugMessage}")
                    }

                    // 3. Nếu có SKU nào chưa tìm thấy ở nhóm mặc định (ví dụ lifetime_pro tạo nhầm ở SUBS hoặc ngược lại),
                    // kiểm tra chéo an toàn trong từng truy vấn đơn lẻ cùng loại
                    val missingLifetimeInInApp = !mergedMap.containsKey(SKU_LIFETIME)
                    if (missingLifetimeInInApp) {
                        val crossSubsParams = QueryProductDetailsParams.newBuilder()
                            .setProductList(
                                listOf(
                                    QueryProductDetailsParams.Product.newBuilder()
                                        .setProductId(SKU_LIFETIME)
                                        .setProductType(BillingClient.ProductType.SUBS)
                                        .build()
                                )
                            )
                            .build()
                        billingClient.queryProductDetailsAsync(crossSubsParams) { crossRes, crossDetails ->
                            if (crossRes.responseCode == BillingClient.BillingResponseCode.OK) {
                                val crossList = crossDetails.productDetailsList
                                if (!crossList.isNullOrEmpty()) {
                                    synchronized(this@BillingManager) {
                                        for (item in crossList) {
                                            mergedMap[item.productId] = item
                                        }
                                        _productDetailsMap.value = HashMap(mergedMap)
                                    }
                                }
                            }
                            finishProductQuery(mergedMap)
                        }
                    } else {
                        finishProductQuery(mergedMap)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in queryAvailableProducts", e)
            finishProductQuery(_productDetailsMap.value)
        }
    }

    private fun finishProductQuery(finalMap: Map<String, ProductDetails>) {
        val callbacks: List<(Map<String, ProductDetails>) -> Unit>
        val snapshot: Map<String, ProductDetails>
        synchronized(this) {
            snapshot = HashMap(finalMap)
            _productDetailsMap.value = snapshot
            isQueryingProducts = false
            hasCompletedInitialQuery = true
            _isLoadingProducts.value = false
            callbacks = ArrayList(pendingQueryCallbacks)
            pendingQueryCallbacks.clear()
        }
        callbacks.forEach { callback ->
            try {
                callback.invoke(snapshot)
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking product query callback", e)
            }
        }
        if (snapshot.isEmpty()) {
            scheduleAutoRetryIfNeeded()
        } else {
            retryCount = 0
        }
    }

    fun hasAnyLoadedProduct(): Boolean = _productDetailsMap.value.isNotEmpty()

    fun hasLoadedAllProducts(): Boolean =
        _productDetailsMap.value.containsKey(SKU_WEEKLY) &&
        _productDetailsMap.value.containsKey(SKU_YEARLY) &&
        _productDetailsMap.value.containsKey(SKU_LIFETIME)

    /**
     * Lấy ProductDetails đã cache cho SKU chỉ định
     */
    fun getProductDetails(sku: String): ProductDetails? {
        return _productDetailsMap.value[sku]
    }

    /**
     * Chọn SubscriptionOfferDetails chuẩn (Ưu tiên Base Plan có offerId == null và có phase trả phí > 0)
     * Sử dụng đồng nhất cho cả hiển thị giá trên Paywall và truyền offerToken vào launchBillingFlow.
     */
    fun getSelectedSubscriptionOffer(details: ProductDetails): ProductDetails.SubscriptionOfferDetails? {
        val offers = details.subscriptionOfferDetails ?: return null
        if (offers.isEmpty()) return null
        // Ưu tiên Base Plan chính gốc (offerId == null) có pricing phase > 0
        return offers.firstOrNull { offer ->
            offer.offerId == null && offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros > 0 }
        } ?: offers.firstOrNull { offer ->
            offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros > 0 }
        } ?: offers.firstOrNull()
    }

    /**
     * Lấy PricingPhase định kỳ trả phí của gói thuê bao (SUBS) để trích xuất giá và currency code.
     * Luôn lấy phase trả phí cuối cùng (lastOrNull { it.priceAmountMicros > 0 }) để không lấy nhầm Free Trial 0đ.
     */
    fun getRecurringPricingPhase(sku: String): ProductDetails.PricingPhase? {
        val details = _productDetailsMap.value[sku] ?: return null
        return extractRecurringPricingPhase(details)
    }

    private fun extractRecurringPricingPhase(details: ProductDetails): ProductDetails.PricingPhase? {
        val offer = getSelectedSubscriptionOffer(details) ?: return null
        val phases = offer.pricingPhases.pricingPhaseList
        return phases.lastOrNull { it.priceAmountMicros > 0 } ?: phases.lastOrNull()
    }

    private fun extractFormattedPrice(details: ProductDetails): String? {
        return try {
            if (details.productType == BillingClient.ProductType.SUBS) {
                extractRecurringPricingPhase(details)?.formattedPrice
            } else {
                details.oneTimePurchaseOfferDetails?.formattedPrice
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting price for ${details.productId}", e)
            null
        }
    }

    /**
     * Lấy giá định dạng chính xác từ Google Play (ví dụ: "$2.99", "69.000 ₫", "2,99 €", "₹249.00")
     */
    fun getFormattedPrice(sku: String): String? {
        val details = _productDetailsMap.value[sku] ?: return null
        return extractFormattedPrice(details)
    }

    /**
     * Lấy mã tiền tệ ISO 4217 từ ProductDetails của SKU (ví dụ: "USD", "VND", "EUR", "INR")
     */
    fun getPriceCurrencyCode(sku: String): String? {
        val details = _productDetailsMap.value[sku] ?: return null
        return try {
            if (details.productType == BillingClient.ProductType.SUBS) {
                extractRecurringPricingPhase(details)?.priceCurrencyCode
            } else {
                details.oneTimePurchaseOfferDetails?.priceCurrencyCode
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Phát hiện mã tiền tệ của tài khoản Google Play từ bất kỳ sản phẩm nào đã tải thành công
     */
    fun getDetectedPlayCurrencyCode(): String? {
        return getPriceCurrencyCode(SKU_YEARLY)
            ?: getPriceCurrencyCode(SKU_WEEKLY)
            ?: getPriceCurrencyCode(SKU_LIFETIME)
    }

    /**
     * Lấy chuỗi giá mẫu đã định dạng từ Google Play để đồng bộ ký hiệu tiền tệ 100% giữa các gói
     */
    fun getDetectedReferenceFormattedPrice(): String? {
        return getFormattedPrice(SKU_YEARLY)
            ?: getFormattedPrice(SKU_WEEKLY)
            ?: getFormattedPrice(SKU_LIFETIME)
    }

    /**
     * Khởi chạy Google Play Billing Flow mở BottomSheet giỏ hàng thanh toán chuẩn của Google
     */
    fun launchBillingFlow(
        activity: Activity,
        sku: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        onPurchaseResultListener = onResult

        if (!billingClient.isReady) {
            startConnection {
                launchBillingFlowInternal(activity, sku, onResult)
            }
            return
        }

        launchBillingFlowInternal(activity, sku, onResult)
    }

    private fun launchBillingFlowInternal(
        activity: Activity,
        sku: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val details = _productDetailsMap.value[sku]
        if (details == null) {
            queryAvailableProducts { map ->
                val retryDetails = map[sku]
                if (retryDetails != null) {
                    executeBillingFlow(activity, retryDetails, onResult)
                } else {
                    Log.w(TAG, "ProductDetails not available for SKU: $sku")
                    onResult(false, appContext.getString(R.string.billing_syncing_msg))
                }
            }
            return
        }

        executeBillingFlow(activity, details, onResult)
    }

    private fun executeBillingFlow(
        activity: Activity,
        details: ProductDetails,
        onResult: (Boolean, String?) -> Unit
    ) {
        try {
            val productDetailsParamsList = if (details.productType == BillingClient.ProductType.SUBS) {
                val selectedOffer = getSelectedSubscriptionOffer(details)
                val offerToken = selectedOffer?.offerToken ?: run {
                    onResult(false, appContext.getString(R.string.billing_token_not_found))
                    return
                }
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            } else {
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            }

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
            if (responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "launchBillingFlow failed with response code: $responseCode")
                onResult(false, appContext.getString(R.string.billing_launch_error, responseCode))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during executeBillingFlow", e)
            onResult(false, appContext.getString(R.string.billing_connection_error, e.message ?: ""))
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                    onPurchaseResultListener?.invoke(true, null)
                } else {
                    onPurchaseResultListener?.invoke(false, appContext.getString(R.string.billing_no_purchase_found))
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User canceled the purchase.")
                onPurchaseResultListener?.invoke(false, appContext.getString(R.string.billing_user_canceled))
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.i(TAG, "User already owns this item.")
                settingsRepository.isProUser = true
                onPurchaseResultListener?.invoke(true, appContext.getString(R.string.billing_item_already_owned))
            }
            else -> {
                Log.e(TAG, "Purchase error: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                onPurchaseResultListener?.invoke(false, appContext.getString(R.string.billing_purchase_failed, billingResult.debugMessage))
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            settingsRepository.isProUser = true
            val plan = when {
                purchase.products.contains(SKU_WEEKLY) -> "weekly"
                purchase.products.contains(SKU_LIFETIME) -> "lifetime"
                else -> "yearly"
            }
            settingsRepository.subscriptionPlan = plan
            FirebaseAnalyticsManager.logSubscriptionPurchased(plan, "google_play_billing")

            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgeParams) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.i(TAG, "Purchase acknowledged successfully: ${purchase.orderId}")
                    } else {
                        Log.w(TAG, "Acknowledge failed: ${result.responseCode} - ${result.debugMessage}")
                    }
                }
            }
        }
    }

    /**
     * Khôi phục gói mua (Restore Purchases)
     */
    fun restorePurchases(onResult: (Boolean, String) -> Unit) {
        if (!billingClient.isReady) {
            startConnection { restorePurchases(onResult) }
            return
        }

        // 1. Kiểm tra Subscription
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { subsResult, subsPurchases ->
            val hasActiveSubs = subsResult.responseCode == BillingClient.BillingResponseCode.OK &&
                    subsPurchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }

            if (hasActiveSubs) {
                val active = subsPurchases.first { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                handlePurchase(active)
                onResult(true, appContext.getString(R.string.billing_restore_success))
                return@queryPurchasesAsync
            }

            // 2. Kiểm tra In-App Purchase (Trọn đời)
            val inAppParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient.queryPurchasesAsync(inAppParams) { inAppResult, inAppPurchases ->
                val hasActiveInApp = inAppResult.responseCode == BillingClient.BillingResponseCode.OK &&
                        inAppPurchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }

                if (hasActiveInApp) {
                    val active = inAppPurchases.first { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    handlePurchase(active)
                    onResult(true, appContext.getString(R.string.billing_restore_lifetime_success))
                } else {
                    onResult(false, appContext.getString(R.string.billing_no_purchase_found))
                }
            }
        }
    }

    /**
     * Kiểm tra trạng thái thuê bao nền khi mở app (Bao gồm cả SUBS và INAPP trọn đời)
     */
    fun queryActivePurchases() {
        if (!billingClient.isReady) return

        try {
            // 1. Kiểm tra Subscription đang hoạt động
            val subsParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient.queryPurchasesAsync(subsParams) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val active = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    for (p in active) {
                        handlePurchase(p)
                    }
                }
            }

            // 2. Kiểm tra In-App Purchases (Gói trọn đời)
            val inAppParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient.queryPurchasesAsync(inAppParams) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val active = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                    for (p in active) {
                        handlePurchase(p)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in queryActivePurchases", e)
        }
    }
}
