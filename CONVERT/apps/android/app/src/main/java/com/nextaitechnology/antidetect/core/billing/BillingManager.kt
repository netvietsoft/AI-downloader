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
import com.nextaitechnology.antidetect.core.analytics.FirebaseAnalyticsManager
import com.nextaitechnology.antidetect.feature.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Trình Quản Lý Thanh Toán & Đăng Ký Thuê Bao Google Play (Google Play Billing Manager)
 * Full integration with Google Play Billing Library v7.x
 * Ensures dynamic localized price/currency matching between Offer screen and Google Play Payment Cart.
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
     * Khởi tạo kết nối với Google Play Billing Services
     */
    fun startConnection(onReady: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            _isBillingReady.value = true
            onReady?.invoke()
            queryAvailableProducts()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Google Play Billing setup successful.")
                    _isBillingReady.value = true
                    queryAvailableProducts()
                    queryActivePurchases()
                    onReady?.invoke()
                } else {
                    Log.w(TAG, "Billing setup failed with response code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    _isBillingReady.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected. Retrying...")
                _isBillingReady.value = false
            }
        })
    }

    /**
     * Truy vấn thông tin sản phẩm và giá/tiền tệ trực tiếp từ Google Play Store.
     * Lưu ý quan trọng: BillingClient 8.0.0 cấm kết hợp SUBS và INAPP trong cùng 1 QueryProductDetailsParams
     * (gây ra IllegalArgumentException: All products should be of the same product type).
     * Do đó phải chia thành 2 truy vấn riêng biệt và hợp nhất kết quả.
     */
    fun queryAvailableProducts(onComplete: ((Map<String, ProductDetails>) -> Unit)? = null) {
        if (!billingClient.isReady) {
            startConnection { queryAvailableProducts(onComplete) }
            return
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

            val resultMap = HashMap<String, ProductDetails>(_productDetailsMap.value)

            // 1. Truy vấn các gói thuê bao tuần & năm (SUBS)
            billingClient.queryProductDetailsAsync(subsParams) { subsBillingResult, subsDetailsResult ->
                if (subsBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val list = subsDetailsResult.productDetailsList
                    if (!list.isNullOrEmpty()) {
                        for (item in list) {
                            resultMap[item.productId] = item
                            Log.i(TAG, "Loaded SUBS SKU: ${item.productId}, name: ${item.name}")
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
                            for (item in inAppList) {
                                resultMap[item.productId] = item
                                Log.i(TAG, "Loaded INAPP SKU: ${item.productId}, name: ${item.name}")
                            }
                        }
                    } else {
                        Log.w(TAG, "queryProductDetailsAsync INAPP failed: ${inAppBillingResult.responseCode} - ${inAppBillingResult.debugMessage}")
                    }

                    _productDetailsMap.value = resultMap
                    onComplete?.invoke(resultMap)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in queryAvailableProducts", e)
            onComplete?.invoke(_productDetailsMap.value)
        }
    }

    /**
     * Lấy giá định dạng chính xác từ Google Play (ví dụ: "$2.99", "69.000 ₫", "€2.99")
     */
    fun getFormattedPrice(sku: String): String? {
        val details = _productDetailsMap.value[sku] ?: return null
        return try {
            if (details.productType == BillingClient.ProductType.SUBS) {
                details.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
            } else {
                details.oneTimePurchaseOfferDetails?.formattedPrice
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting price for SKU $sku", e)
            null
        }
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
                    onResult(false, "Sản phẩm đang được đồng bộ từ Google Play. Vui lòng thử lại trong giây lát.")
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
                val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: run {
                    onResult(false, "Không tìm thấy token ưu đãi của gói cước.")
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
                onResult(false, "Không thể mở trang thanh toán Google Play (Mã lỗi: $responseCode)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during executeBillingFlow", e)
            onResult(false, "Lỗi kết nối thanh toán Google Play: ${e.message}")
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
                    onPurchaseResultListener?.invoke(false, "Không có giao dịch nào được ghi nhận.")
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User canceled the purchase.")
                onPurchaseResultListener?.invoke(false, "Bạn đã hủy giao dịch thanh toán.")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.i(TAG, "User already owns this item.")
                settingsRepository.isProUser = true
                onPurchaseResultListener?.invoke(true, "Bạn đã sở hữu gói cước này trước đó!")
            }
            else -> {
                Log.e(TAG, "Purchase error: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                onPurchaseResultListener?.invoke(false, "Lỗi thanh toán: ${billingResult.debugMessage}")
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
                onResult(true, "Khôi phục thành công! Gói cước Google Play của bạn đã được kích hoạt.")
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
                    onResult(true, "Khôi phục thành công! Gói Trọn Đời của bạn đã được kích hoạt.")
                } else {
                    onResult(false, "Không tìm thấy gói cước nào đang hoạt động trên tài khoản Google Play này.")
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
