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
        path = listOf(
            "0x50c5725949a6f0c72e6c4a641f24049a917db0cb", // WETH
            "0xd9a8e14aD03f6f722d13B43Ff5fC3B0f06b6b6Ca"  // USDbC
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
        router = "0x1b02dA8Cb0d097eB8D57A175b88c7D8b47997506",
        path = listOf(
            "0x4200000000000000000000000000000000000006", // WETH
            "0xd9a8e14aD03f6f722d13B43Ff5fC3B0f06b6b6Ca"  // USDbC                              // USDbC (USDC) on Base
        ),
        outputDecimals = 6,
        chain = Chain.BASE
    ),
}

enum class Chain {
    ETHEREUM,
    BASE
}