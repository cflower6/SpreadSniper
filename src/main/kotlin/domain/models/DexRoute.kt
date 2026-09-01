package domain.models

import domain.models.registries.Token

data class DexRoute(
    val pool: Pool,
    val tokenIn: Token,
    val tokenOut: Token
) {

    init {

        require(
            tokenIn.chain == pool.chain
        ) {
            "${tokenIn.symbol} is not on ${pool.chain}"
        }

        require(
            tokenOut.chain == pool.chain
        ) {
            "${tokenOut.symbol} is not on ${pool.chain}"
        }

        require(
            pool.contains(
                tokenIn,
                tokenOut
            )
        ) {
            "${tokenIn.symbol}/${tokenOut.symbol} " +
                    "is not supported by pool ${pool.poolAddress}"
        }
    }

    val dex: Dex
        get() =
            pool.dex

    val chain: Chain
        get() =
            pool.chain

    val path: List<String>
        get() =
            listOf(
                tokenIn.address,
                tokenOut.address
            )

    fun reversed(): DexRoute =
        DexRoute(
            pool = pool,
            tokenIn = tokenOut,
            tokenOut = tokenIn
        )
}