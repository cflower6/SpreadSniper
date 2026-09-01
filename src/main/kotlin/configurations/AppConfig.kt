package configurations

object AppConfig {
    val httpPort: Int by lazy {
        getEnvOrNull("PORT")
            ?.toIntOrNull()
            ?: getEnvOrNull("HTTP_PORT")
                ?.toIntOrNull()
            ?: 8080
    }

    val ethereumRpc: String by lazy { getEnv("ETHEREUM_RPC") }
    val baseRpc: String by lazy { getEnv("BASE_RPC") }
    val loanshotUrl: String by lazy {getEnv("LOANSHOT_URL")}
    val arbitrumRpc: String by lazy { getEnv("ARBITRUM_RPC") }

    // Redis
    val redisUrl: String by lazy {
        getEnvOrNull("REDIS_URL")
            ?: "redis://localhost:6379"
    }

    val executionClaimTtlSeconds: Long by lazy {
        getEnvOrNull("EXECUTION_CLAIM_TTL_SECONDS")
            ?.toLongOrNull()
            ?: 120L
    }

    // Scanner optimization

    val prefilterMinSpreadBps: Double by lazy {
        getEnvOrNull("PREFILTER_MIN_SPREAD_BPS")
            ?.toDoubleOrNull()
            ?: 5.0
    }

    val maxPriceImpactBps: Double by lazy {
        getEnvOrNull("MAX_PRICE_IMPACT_BPS")
            ?.toDoubleOrNull()
            ?: 100.0
    }

    val probeSizeUsd: String by lazy {
        getEnvOrNull("PROBE_SIZE_USD")
            ?: "10"
    }

    val probeSizeWeth: String by lazy {
        getEnvOrNull("PROBE_SIZE_WETH")
            ?: "0.01"
    }

    val x402Enabled: Boolean by lazy {
        getEnvOrNull("X402_ENABLED")
            ?.toBoolean()
            ?: false
    }

    val x402PriceUsd: String by lazy {
        getEnvOrNull("X402_PRICE_USD")
            ?: "0.01"
    }

    val x402RecipientAddress: String by lazy {
        getEnv("X402_RECIPIENT_ADDRESS")
    }

    val x402Network: String by lazy {
        getEnvOrNull("X402_NETWORK")
            ?: "eip155:8453"
    }

    val x402FacilitatorUrl: String by lazy {
        getEnv("X402_FACILITATOR_URL")
    }

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

    val tradeSizes: List<String> by lazy {
        getEnvOrNull("TRADE_SIZES")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: listOf(
                "0.001",
                "0.005",
                "0.01",
                "0.025",
                "0.05",
                "0.1",
                "0.25",
                "0.5",
                "1.0"
            )
    }

    val discordUrl: String by lazy { getEnv("DISCORD_WEB_HOOK") }

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