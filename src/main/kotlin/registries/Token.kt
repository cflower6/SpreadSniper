package registries

data class Token(
    val symbol: String,
    val address: String,
    val decimals: Int
)

object Tokens {
    // Base canonical addresses
    val WETH = Token("WETH", "0x4200000000000000000000000000000000000006", 18)
    val CBETH = Token("cbETH", "0x2Ae3F1Ec7F1F5012CFEab0185bfc7aa3cf0DEc22", 18)
    val AERO = Token("AERO", "0x940181a94A35A4569E4529A3CDfB74e38FD98631", 18)
    val USDC = Token("USDC", "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913", 6)
    val USDBC = Token("USDBC", "0xd9aAEc86B65D86f6A7B5B1b0c42FFA531710b6CA", 6)

    private val byAddr = listOf(WETH, USDC, USDBC, CBETH, AERO).associateBy { it.address.lowercase() }

    fun byAddress(address: String): Token =
        byAddr[address.lowercase()] ?: error("Unknown token address: $address")

    fun isKnown(address: String): Boolean = byAddr.containsKey(address.lowercase())
}
