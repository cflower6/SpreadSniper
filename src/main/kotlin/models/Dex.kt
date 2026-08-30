package models

import interfaces.DexQuoter
import services.AerodromeQuoterService
import services.UniV2QuoterService

enum class Dex(
    val path: List<String>,
    val chain: Chain,
    val dexName: String,
) {
    AERODROME_BASE(
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913"// USDC
        ),
        chain = Chain.BASE,
        dexName = "AERODROME",
    ),

    AERODROME_BASE_USDBC(
        path = listOf(
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913", // USDC
            "0xd9aAEc86B65D86f6A7B5B1b0c42FFA531710b6CA" // USDBC
        ),
        chain = Chain.BASE,
        dexName = "AERODROME",
    ),

    AERODROME_BASE_cbETH(
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x2Ae3F1Ec7F1F5012CFEab0185bfc7aa3cf0DEc22", // cbETH
        ),
        chain = Chain.BASE,
        dexName = "AERODROME",
    ),

    AERODROME_BASE_AERO(
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x940181a94A35A4569E4529A3CDfB74e38FD98631", // AERO
        ),
        chain = Chain.BASE,
        dexName = "AERODROME",
    ),

    UNISWAP_BASE(
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913" // USDC
        ),
        chain = Chain.BASE,
        dexName = "UNISWAP",
    ),

    UNISWAP_BASE_USDBC(
        path = listOf(
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913", // USDC
            "0xd9aAEc86B65D86f6A7B5B1b0c42FFA531710b6CA", // USDBC
        ),
        chain = Chain.BASE,
        dexName = "UNISWAP",
    ),

    UNISWAP_BASE_cbETH(
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x2Ae3F1Ec7F1F5012CFEab0185bfc7aa3cf0DEc22", // cbETH
        ),
        chain = Chain.BASE,
        dexName = "UNISWAP",
    ),

    UNISWAP_BASE_AERO(
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x940181a94A35A4569E4529A3CDfB74e38FD98631", // AERO
        ),
        chain = Chain.BASE,
        dexName = "UNISWAP",
    ),
}

enum class Chain {
    ETHEREUM,
    BASE,
    ARBITRUM
}

/**
 *     UNISWAP_V3("0x2626664c2603336E57B271c5C0b26F421741e481", 0.003),
 *     AERODROME("0xcF77a3Ba9A5CA399B7c97c74d54e5b1Beb874E43", 0.002),
 *     BASESWAP("0x327Df1E6de05895d2ab08513aaDD9313Fe505d86", 0.0025),
 *     SUSHISWAP("0x6BDED42c6DA8FBf0d2bA55B2fa120C5e0c8D7891", 0.003),
 *     SWAPBASED("0xaaa3b1F1bd7BCc97fD1917c18ADE665C5D31F066", 0.003)
 */
fun createQuoters(): List<DexQuoter> {
    // more up-to-date router
    val aeroQuoter = AerodromeQuoterService(
        name = "AERODROME",
        router = "0xcF77a3Ba9A5CA399B7c97c74d54e5b1Beb874E43",
        factory = "0x420dd381b31aef6683db6b902084cb0ffece40da",
        stable = false
    )

    val uniV2QuoterService = UniV2QuoterService(
        name = "UNIV2",
        router = "0x4752ba5dbc23f44d87826276bf6fd6b1c372ad24"
    )

    return listOf(aeroQuoter, uniV2QuoterService)
}