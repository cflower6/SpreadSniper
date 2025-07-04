package models

enum class Dex(
    val router: String,
    val path: List<String>,
    val outputDecimals: Int
) {
    UNISWAP(
        router = "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D",
        path = listOf(
            "0x6B175474E89094C44Da98b954EedeAC495271d0F", // DAI
            "0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"  // USDC
        ),
        outputDecimals = 6
    ),
    SUSHI(
        router = "0xd9e1CE17f2641f24aE83637ab66a2CCA9C378B9F",
        path = listOf(
            "0x6B175474E89094C44Da98b954EedeAC495271d0F",
            "0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        ),
        outputDecimals = 6
    )
}