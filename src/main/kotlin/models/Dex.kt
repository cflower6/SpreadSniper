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
    UNISWAP_BASE(
        router = "0x4752ba5dbc23f44d87826276bf6fd6b1c372ad24",
//        router = "0x33128a8fC17869897dcE68Ed026d694621f6FDfD",
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913" // USDC
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

    BASE_USDC_WETH(
        router = "0x33128a8fC17869897dcE68Ed026d694621f6FDfD",
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0x833589fCD6eDb6E08f4c7C32D4f71b54bda02913" // USDC
        ),
        outputDecimals = 6,
        chain = Chain.BASE
    )
}

enum class Chain {
    ETHEREUM,
    BASE,
    ARBITRUM
}