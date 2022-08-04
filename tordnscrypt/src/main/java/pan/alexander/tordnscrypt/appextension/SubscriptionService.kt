/*
 * Copyright (c) 2022 FullDive
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pan.alexander.tordnscrypt.appextension

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fulldive.iap.DataWrappers
import com.fulldive.iap.IapConnector
import com.fulldive.iap.PurchaseServiceListener
import com.fulldive.iap.SubscriptionServiceListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pan.alexander.tordnscrypt.MainActivity
import pan.alexander.tordnscrypt.analytics.StatisticHelper
import pan.alexander.tordnscrypt.analytics.TrackerConstants
import pan.alexander.tordnscrypt.main_fragment.MainFragment
import java.util.*

object SubscriptionService {

    private const val proSku = "full_tor_pro_subscription"
    private const val proSkuDiscount = "full_tor_pro_subscription_discount"

    private const val proSkuPurchase = "full_tor_pro_purchase"
    private const val proSkuDiscountPurchase = "full_tor_pro_purchase_discount"

    private const val STATE_PURCHASED = 1
    private const val STATE_PENDING = 2
    private const val STATE_UNDEFINED = 0

    private const val DAYS_IN_YEAR = 365
    private const val MILLES_IN_DAY = 24 * 60 * 60 * 1000

    private val repeatPopupCounts = listOf(2, 5)

    val isConnectedState = MutableStateFlow(false)
    val isProStatusPurchasedState = MutableStateFlow<Boolean>(false)
    val isPopupShowState = MutableStateFlow<Boolean>(false)
    private val subscriptionPrices = mutableMapOf<String, DataWrappers.ProductDetails>()

    private var iapConnector: IapConnector? = null

    suspend fun init(context: Context) {
        iapConnector = IapConnector(
            context = context, // activity / context
            nonConsumableKeys = listOf(
                proSkuPurchase,
                proSkuDiscountPurchase
            ), // pass the list of non-consumables
            consumableKeys = emptyList(), // pass the list of consumables
            subscriptionKeys = listOf(proSku, proSkuDiscount), // pass the list of subscriptions
            enableLogging = true // to enable / disable logging
        )
        isConnectedState.emit(true)
        isProStatusPurchasedState.value = false

        iapConnector?.addPurchaseListener(object : PurchaseServiceListener {
            override fun onPricesUpdated(iapKeyPrices: Map<String, DataWrappers.ProductDetails>) {
                subscriptionPrices.putAll(iapKeyPrices)
            }

            override fun onProductPurchased(purchaseInfo: DataWrappers.PurchaseInfo) {
                when (purchaseInfo.sku) {
                    proSkuPurchase, proSkuDiscountPurchase -> {
                        CoroutineScope(coroutineContext).launch {
                            StatisticHelper.logAction(TrackerConstants.EVENT_BUY_PRO_SUCCESS)
                            isProStatusPurchasedState.value =
                                purchaseInfo.purchaseState == STATE_PURCHASED
                        }
                    }
                }
            }

            override fun onProductRestored(purchaseInfo: DataWrappers.PurchaseInfo) {
                when (purchaseInfo.sku) {
                    proSkuPurchase, proSkuDiscountPurchase -> {
                        val daysAfterPurchase =
                            (Calendar.getInstance().time.time - purchaseInfo.purchaseTime) / MILLES_IN_DAY
                        if (daysAfterPurchase <= DAYS_IN_YEAR) {
                            CoroutineScope(coroutineContext).launch {
                                isProStatusPurchasedState.value =
                                    (purchaseInfo.purchaseState == STATE_PURCHASED)
                            }
                        }
                    }
                }
            }
        })

        iapConnector?.addSubscriptionListener(object : SubscriptionServiceListener {
            override fun onSubscriptionRestored(purchaseInfo: DataWrappers.PurchaseInfo) {
                // will be triggered upon fetching owned subscription upon initialization
                when (purchaseInfo.sku) {
                    proSku, proSkuDiscount -> {
                        CoroutineScope(coroutineContext).launch {
                            isProStatusPurchasedState.value =
                                (purchaseInfo.purchaseState == STATE_PURCHASED)
                        }
                    }
                }
            }

            override fun onSubscriptionPurchased(purchaseInfo: DataWrappers.PurchaseInfo) {
                // will be triggered whenever subscription succeeded
                when (purchaseInfo.sku) {
                    proSku, proSkuDiscount -> {
                        CoroutineScope(coroutineContext).launch {
                            StatisticHelper.logAction(TrackerConstants.EVENT_BUY_PRO_SUCCESS)
                            isProStatusPurchasedState.value =
                                (purchaseInfo.purchaseState == STATE_PURCHASED)
                        }
                    }
                }
            }

            override fun onPricesUpdated(iapKeyPrices: Map<String, DataWrappers.ProductDetails>) {
                subscriptionPrices.putAll(iapKeyPrices)
            }
        })
        handlePromoPopupState(context)
    }

    fun onDestroy() {
        iapConnector?.destroy()
    }

    fun subscribe(activity: Activity) {
        when {
            subscriptionPrices[proSkuDiscount] != null -> {
                iapConnector?.subscribe(activity, proSkuDiscount)
            }
            subscriptionPrices[proSku] != null -> {
                iapConnector?.subscribe(activity, proSku)
            }
        }
    }

    fun purchase(activity: Activity) {
        when {
            subscriptionPrices[proSkuDiscountPurchase] != null -> {
                iapConnector?.purchase(activity, proSkuDiscountPurchase)
            }
            subscriptionPrices[proSkuPurchase] != null -> {
                iapConnector?.purchase(activity, proSkuPurchase)
            }
        }
    }

    fun getProSubscriptionInfo(): ProSubscriptionInfo {
        val (fullPrice, currency) = getSkuPrice(proSku)
        val (discountPrice, _) = getSkuPrice(proSkuDiscount)
        return ProSubscriptionInfo(
            price = fullPrice,
            salePrice = discountPrice,
            currency = currency
        )
    }

    fun init(activity: AppCompatActivity) {
        activity.lifecycleScope.launch {
            init(activity.baseContext)
        }
    }

    fun observeIsPurchasedState(activity: MainActivity) {
        activity.lifecycleScope.launch {
            isProStatusPurchasedState
                .collect { isPurchased ->
                    if (isPurchased) activity.onPurchase()
                }
        }
    }

    fun observeIsPurchasedState(fragment: MainFragment) {
        fragment.lifecycleScope.launch {
            isProStatusPurchasedState
                .collect { isPurchased ->
                    if (isPurchased) fragment.onPurchase()
                }
        }
    }

    private fun getSkuPrice(sku: String): Pair<String, String> {
        return subscriptionPrices[sku]?.let {
            Pair(it.priceAmount.toString(), it.priceCurrencyCode.orEmptyString())
        } ?: Pair("", "")
    }

    private fun handlePromoPopupState(context: Context) {
        val isClosed = AppSettingsService.getIsPromoPopupClosed(context)
        isPopupShowState.value = if (isClosed) {
            val closeCount = AppSettingsService.getPromoCloseStartCounter(context)
            val startCount = AppSettingsService.getCurrentStartCounter(context)
            val diff = startCount - closeCount
            repeatPopupCounts.any { it == diff }
        } else {
            true
        } && !context.isBrowserInstalled()
    }

    fun setClosePopup(context: Context, isClose: Boolean) {
        isPopupShowState.value = !isClose
        if (isClose && !AppSettingsService.getIsPromoPopupClosed(context)) {
            AppSettingsService.setIsPromoPopupClosed(context, true)
        }
    }

    fun observeIsShowPopup(fragment: MainFragment) {
        fragment.lifecycleScope.launch {
            isProStatusPurchasedState
                .combine(isPopupShowState)
                { isPurchased, isShow ->
                    !isPurchased && isShow
                }.collect { isVisible ->
                    fragment.onLimitedOfferVisibilityChanged(isVisible)
                }
        }
    }
}