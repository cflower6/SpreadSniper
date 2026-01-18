package configurations

import java.math.BigInteger

object AppConfig {
    val ethereumRpc: String by lazy { getEnv("ETHEREUM_RPC") }
    val baseRpc: String by lazy { getEnv("BASE_RPC") }
    val loanshotUrl: String by lazy {getEnv("LOANSHOT_URL")}
    val arbitrumRpc: String by lazy { getEnv("ARBITRUM_RPC") }

    val gasCostEstimate: Double by lazy { getEnvOrNull("GAS_COST_ESTIMATE")?.toDoubleOrNull() ?: 0.25 }
    val profitThresholdUSD: Double by lazy { getEnvOrNull("PROFIT_THRESHOLD")?.toDoubleOrNull() ?: 0.5 }
    val pollingIntervalMs: Long by lazy { getEnvOrNull("POLLING_INTERVAL_MS")?.toLongOrNull() ?: 5000L }
    val emailCooldownMs: Long by lazy { getEnvOrNull("EMAIL_COOLDOWN_MS")?.toLongOrNull() ?: (5 * 60 * 1000L) }

    // Gas estimation
    val ethPriceUsd: Double by lazy { getEnvOrNull("ETH_PRICE_USD")?.toDoubleOrNull() ?: 3000.0 }
    val gasLimit: Long by lazy { getEnvOrNull("GAS_LIMIT")?.toLongOrNull() ?: 300_000L }
    val dynamicGasEnabled: Boolean by lazy { getEnvOrNull("DYNAMIC_GAS_ENABLED")?.toBoolean() ?: true }

    // WebSocket configuration
    val baseWsRpc: String? by lazy { getEnvOrNull("BASE_WS_RPC") }
    val useWebSocket: Boolean by lazy { getEnvOrNull("USE_WEBSOCKET")?.toBoolean() ?: false }

    // Trade execution (DANGEROUS - disabled by default)
    val privateKey: String? by lazy { getEnvOrNull("PRIVATE_KEY") }
    val executionEnabled: Boolean by lazy { getEnvOrNull("EXECUTION_ENABLED")?.toBoolean() ?: false }
    val maxSlippagePct: Double by lazy { getEnvOrNull("MAX_SLIPPAGE_PCT")?.toDoubleOrNull() ?: 0.5 }
    val minProfitForExecution: Double by lazy { getEnvOrNull("MIN_PROFIT_EXECUTION")?.toDoubleOrNull() ?: 1.0 }

    val tradeAmount: BigInteger by lazy {
        getEnv("TRADE_AMOUNT").toBigIntegerOrNull() ?: BigInteger("1000000000000000000")
    }

    val emailTo: String by lazy { getEnv("EMAIL_TO") }

    val phoneNumber: String by lazy { getEnv("PHONE_NUMBER") }

    val clientId: String by lazy { getEnv("NOTIFICATION_CLIENT_ID") }
    val clientSecret: String by lazy { getEnv("NOTIFICATION_CLIENT_SECRET") }

    val stableCoins: Set<String> by lazy {
        getEnvOrNull("STABLE_COINS")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            ?: setOf("USDC", "USDT", "DAI")
    }

    private fun getEnv(key: String): String {
        return System.getProperty(key)
            ?: System.getenv(key)
            ?: error("❌ Missing environment variable: $key")
    }

    private fun getEnvOrNull(key: String): String? {
        return System.getProperty(key) ?: System.getenv(key)
    }
}