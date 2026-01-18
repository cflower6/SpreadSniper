package models

enum class Dex(
    val router: String,
    val path: List<String>,
    val outputDecimals: Int,
    val chain: Chain,
) {
    UNISWAP_MAINNET(
        router = "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D",
        path = listOf(
            "0x6B175474E89094C44Da98b954EedeAC495271d0F", // DAI
            "0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"  // USDC
        ),
        outputDecimals = 6,
        chain = Chain.ETHEREUM
    ),

    AERODROME_BASE(
        router = "0xcF77a3Ba9A5CA399B7c97c74d54e5b1Beb874E43",
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913"// USDC
        ),
        outputDecimals = 6,
        chain = Chain.BASE
    ),

    AERODROME_BASE_USDBC(
        router = "0xcF77a3Ba9A5CA399B7c97c74d54e5b1Beb874E43",
        path = listOf(
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913", // USDC
            "0xd9aAEc86B65D86f6A7B5B1b0c42FFA531710b6CA" // USDBC
        ),
        outputDecimals = 6,
        chain = Chain.BASE
    ),

    AERODROME_BASE_cbETH(
        router = "0xcF77a3Ba9A5CA399B7c97c74d54e5b1Beb874E43",
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x2Ae3F1Ec7F1F5012CFEab0185bfc7aa3cf0DEc22", // cbETH
        ),
        outputDecimals = 6,
        chain = Chain.BASE
    ),

    AERODROME_BASE_AERO(
        router = "0xcF77a3Ba9A5CA399B7c97c74d54e5b1Beb874E43",
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x940181a94A35A4569E4529A3CDfB74e38FD98631", // AERO
        ),
        outputDecimals = 6,
        chain = Chain.BASE
    ),

    UNISWAP_BASE(
        router = "0x4752ba5dbc23f44d87826276bf6fd6b1c372ad24",
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913" // USDC
        ),
        outputDecimals = 6,
        chain = Chain.BASE
    ),

    UNISWAP_BASE_USDBC(
        router = "0x4752ba5dbc23f44d87826276bf6fd6b1c372ad24",
        path = listOf(
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913", // USDC
            "0xd9aAEc86B65D86f6A7B5B1b0c42FFA531710b6CA", // USDBC
        ),
        outputDecimals = 6,
        chain = Chain.BASE
    ),

    UNISWAP_BASE_cbETH(
        router = "0x4752ba5dbc23f44d87826276bf6fd6b1c372ad24",
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x2Ae3F1Ec7F1F5012CFEab0185bfc7aa3cf0DEc22", // cbETH
        ),
        outputDecimals = 6,
        chain = Chain.BASE
    ),

    UNISWAP_BASE_AERO(
        router = "0x4752ba5dbc23f44d87826276bf6fd6b1c372ad24",
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x940181a94A35A4569E4529A3CDfB74e38FD98631", // AERO
        ),
        outputDecimals = 6,
        chain = Chain.BASE
    ),

    SUSHI_MAINNET(
        router = "0xd9e1CE17f2641f24aE83637ab66a2CCA9C378B9F",
        path = listOf(
            "0x6B175474E89094C44Da98b954EedeAC495271d0F",
            "0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        ),
        outputDecimals = 6,
        chain = Chain.ETHEREUM
    ),

    SUSHI_BASE(
        router = "0xCf77A3bA9A5cA399B7C97c74D54e5B1Beb874E43",
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913"  // USDbC                              // USDbC (USDC) on Base
        ),
        outputDecimals = 6,
        chain = Chain.BASE
    ),
}

enum class Chain {
    ETHEREUM,
    BASE,
    ARBITRUM
}