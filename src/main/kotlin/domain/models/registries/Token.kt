package domain.models.registries

import domain.models.Chain

data class Token(
    val symbol: String,
    val address: String,
    val decimals: Int,
    val chain: Chain
)

object Tokens {

    val WETH_BASE = Token(
        symbol = "WETH",
        address = "0x4200000000000000000000000000000000000006",
        decimals = 18,
        chain = Chain.BASE
    )

    val CBETH_BASE = Token(
        symbol = "cbETH",
        address = "0x2Ae3F1Ec7F1F5012CFEab0185bfc7aa3cf0DEc22",
        decimals = 18,
        chain = Chain.BASE
    )

    val AERO_BASE = Token(
        symbol = "AERO",
        address = "0x940181a94A35A4569E4529A3CDfB74e38FD98631",
        decimals = 18,
        chain = Chain.BASE
    )

    val USDC_BASE = Token(
        symbol = "USDC",
        address = "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913",
        decimals = 6,
        chain = Chain.BASE
    )

    val USDBC_BASE = Token(
        symbol = "USDbC",
        address = "0xd9aAEc86B65D86f6A7B5B1b0c42FFA531710b6CA",
        decimals = 6,
        chain = Chain.BASE
    )

    private val byAddress = listOf(
        WETH_BASE,
        CBETH_BASE,
        AERO_BASE,
        USDC_BASE,
        USDBC_BASE
    ).associateBy {
        it.chain to it.address.lowercase()
    }

    fun byAddress(
        chain: Chain,
        address: String
    ): Token =
        byAddress[chain to address.lowercase()]
            ?: error("Unknown token address $address on $chain")

    fun isKnown(
        chain: Chain,
        address: String
    ): Boolean =
        byAddress.containsKey(
            chain to address.lowercase()
        )
}