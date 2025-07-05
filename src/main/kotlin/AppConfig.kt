import java.math.BigInteger

object AppConfig {
    val ethereumRpc: String by lazy { getEnv("ETHEREUM_RPC") }
    val baseRpc: String by lazy { getEnv("BASE_RPC") }

    val dexFeeRate: Double by lazy { getEnv("DEX_FEE_RATE").toDoubleOrNull() ?: 0.003 }
    val gasCostEstimate: Double by lazy { getEnv("GAS_COST_ESTIMATE").toDoubleOrNull() ?: 0.25 }
    val profitThresholdUSD: Double by lazy { getEnv("PROFIT_THRESHOLD").toDoubleOrNull() ?: 0.5 }

    val tradeAmount: BigInteger by lazy {
        getEnv("TRADE_AMOUNT").toBigIntegerOrNull() ?: BigInteger("1000000000000000000")
    }

    private fun getEnv(key: String): String {
        return System.getProperty(key)
            ?: System.getenv(key)
            ?: error("❌ Missing environment variable: $key")
    }
}