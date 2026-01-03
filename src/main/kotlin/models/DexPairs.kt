package models

data class DexPair(val buyOn: Dex, val sellOn: Dex, val label: String) {
    companion object {
        val UNI_SUSHI_ETH = DexPair(Dex.SUSHI_MAINNET, Dex.UNISWAP_MAINNET, "Sushi → Uni (ETH)")
        val UNI_BASE_ETH = DexPair(Dex.UNISWAP_BASE, Dex.UNISWAP_MAINNET, "Uni Base → Uni ETH")
        val UNI_SUSHI_BASE = DexPair(Dex.SUSHI_BASE, Dex.UNISWAP_BASE, "Sushi <UNK> Uni (BAS)")
        val BASE_USDC_WETH = DexPair(Dex.UNISWAP_BASE, Dex.SUSHI_BASE, "Uni Base -> SUSHI base")
        val BASE_AERO_UNI_WETH = DexPair(Dex.AERODROME_BASE, Dex.UNISWAP_BASE, "Uni Base -> AERO uni base")
    }
}