package pan.alexander.tordnscrypt.appextension

data class ProSubscriptionInfo(
    val price: String,
    val salePrice: String,
    val currency: String
)

val EMPTY_SUBSCRIPTION_INFO = ProSubscriptionInfo(
    "",
    "",
    ""
)