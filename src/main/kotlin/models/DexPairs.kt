package models

data class DexPair(val buyOn: Dex, val sellOn: Dex, val label: String, val chain: Chain) {
    companion object {
        val BASE_AERO_UNI_WETH = DexPair(Dex.AERODROME_BASE, Dex.UNISWAP_BASE, "Uni Base -> AERO uni base", Dex.AERODROME_BASE.chain)
        val BASE_AERO_UNI_USDBC = DexPair(Dex.AERODROME_BASE_USDBC, Dex.UNISWAP_BASE_USDBC, "Uni Base -> AERO base USDBC", Dex.AERODROME_BASE_USDBC.chain)
        val BASE_AERO_UNI_cbETH = DexPair(Dex.AERODROME_BASE_cbETH, Dex.UNISWAP_BASE_cbETH, "Uni Base -> AERO uni base cbETH", Dex.AERODROME_BASE_cbETH.chain)
        val BASE_AERO_UNI_AERO = DexPair(Dex.AERODROME_BASE_AERO, Dex.UNISWAP_BASE_AERO, "Uni Base -> AERO uni base AERO", Dex.AERODROME_BASE_AERO.chain)
    }
}