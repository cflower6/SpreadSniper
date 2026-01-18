package models

enum class Dex(
    val path: List<String>,
    val chain: Chain,
) {

    AERODROME_BASE(
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913"// USDC
        ),
        chain = Chain.BASE
    ),

    AERODROME_BASE_USDBC(
        path = listOf(
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913", // USDC
            "0xd9aAEc86B65D86f6A7B5B1b0c42FFA531710b6CA" // USDBC
        ),
        chain = Chain.BASE
    ),

    AERODROME_BASE_cbETH(
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x2Ae3F1Ec7F1F5012CFEab0185bfc7aa3cf0DEc22", // cbETH
        ),
        chain = Chain.BASE
    ),

    AERODROME_BASE_AERO(
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x940181a94A35A4569E4529A3CDfB74e38FD98631", // AERO
        ),
        chain = Chain.BASE
    ),

    UNISWAP_BASE(
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913" // USDC
        ),
        chain = Chain.BASE
    ),

    UNISWAP_BASE_USDBC(
        path = listOf(
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913", // USDC
            "0xd9aAEc86B65D86f6A7B5B1b0c42FFA531710b6CA", // USDBC
        ),
        chain = Chain.BASE
    ),

    UNISWAP_BASE_cbETH(
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x2Ae3F1Ec7F1F5012CFEab0185bfc7aa3cf0DEc22", // cbETH
        ),
        chain = Chain.BASE
    ),

    UNISWAP_BASE_AERO(
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x940181a94A35A4569E4529A3CDfB74e38FD98631", // AERO
        ),
        chain = Chain.BASE
    ),
}

enum class Chain {
    ETHEREUM,
    BASE,
    ARBITRUM
}