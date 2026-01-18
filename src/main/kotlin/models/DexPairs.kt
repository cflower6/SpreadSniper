package models

data class DexPair(val buyOn: Dex, val sellOn: Dex, val label: String) {
    companion object {
        val UNI_SUSHI_ETH = DexPair(Dex.SUSHI_MAINNET, Dex.UNISWAP_MAINNET, "Sushi → Uni (ETH)")
        val UNI_BASE_ETH = DexPair(Dex.UNISWAP_BASE, Dex.UNISWAP_MAINNET, "Uni Base → Uni ETH")
        val UNI_SUSHI_BASE = DexPair(Dex.SUSHI_BASE, Dex.UNISWAP_BASE, "Sushi <UNK> Uni (BAS)")
        val BASE_USDC_WETH = DexPair(Dex.UNISWAP_BASE, Dex.SUSHI_BASE, "Uni Base -> SUSHI base")
        val BASE_AERO_UNI_WETH = DexPair(Dex.AERODROME_BASE, Dex.UNISWAP_BASE, "Uni Base -> AERO uni base")
        val BASE_AERO_UNI_USDBC = DexPair(Dex.AERODROME_BASE_USDBC, Dex.UNISWAP_BASE_USDBC, "Uni Base -> AERO base USDBC")
        val BASE_AERO_UNI_ebETH = DexPair(Dex.AERODROME_BASE_cbETH, Dex.UNISWAP_BASE_cbETH, "Uni Base -> AERO uni base cbETH")
        val BASE_AERO_UNI_CBETH = DexPair(Dex.AERODROME_BASE_AERO, Dex.UNISWAP_BASE_AERO, "Uni Base -> AERO uni base AERO")
    }
}