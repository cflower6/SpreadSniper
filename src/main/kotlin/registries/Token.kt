package registries

data class Token(
    val symbol: String,
    val address: String,
    val decimals: Int
)

object Tokens {
    // Base canonical addresses
    val WETH = Token("WETH", "0x3548029694fbb241d45fb24ba0cd9c9d4e745f16", 18)
    val USDC = Token("USDC", "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913", 6)

    private val byAddr = listOf(WETH, USDC).associateBy { it.address.lowercase() }

    fun byAddress(address: String): Token =
        byAddr[address.lowercase()] ?: error("Unknown token address: $address")

    fun isKnown(address: String): Boolean = byAddr.containsKey(address.lowercase())
}
