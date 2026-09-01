package domain.models

import domain.models.registries.Token

data class Pool(
    val dex: Dex,
    val chain: Chain,

    val token0: Token,
    val token1: Token,

    val poolAddress: String,

    val stable: Boolean? = null
) {
    fun contains(
        tokenA: Token,
        tokenB: Token
    ): Boolean =
        (token0 == tokenA && token1 == tokenB) ||
                (token0 == tokenB && token1 == tokenA)
}