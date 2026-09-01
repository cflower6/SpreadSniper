package domain.models.registries

import domain.interfaces.PoolResolver
import domain.models.Chain
import domain.models.Dex
import domain.models.Pool

class PoolRegistry(
    private val resolvers: List<PoolResolver>
) {

    private val pools =
        mutableMapOf<PoolKey, Pool>()

    fun register(
        chain: Chain,
        dex: Dex,
        tokenA: Token,
        tokenB: Token
    ): Pool? {

        val resolver =
            resolvers.firstOrNull {
                it.dex == dex
            } ?: error(
                "No resolver configured for $dex"
            )

        val pool =
            resolver.resolve(
                chain = chain,
                tokenA = tokenA,
                tokenB = tokenB
            )
                ?: return null

        pools[
            PoolKey.of(
                chain,
                dex,
                tokenA,
                tokenB
            )
        ] = pool

        return pool
    }

    fun require(
        chain: Chain,
        dex: Dex,
        tokenA: Token,
        tokenB: Token
    ): Pool {

        val key =
            PoolKey.of(
                chain,
                dex,
                tokenA,
                tokenB
            )

        return pools[key]
            ?: register(
                chain,
                dex,
                tokenA,
                tokenB
            )
            ?: error(
                "No pool found for " +
                        "$dex ${tokenA.symbol}/${tokenB.symbol} on $chain"
            )
    }
}

private data class PoolKey(
    val chain: Chain,
    val dex: Dex,
    val token0: String,
    val token1: String
) {

    companion object {

        fun of(
            chain: Chain,
            dex: Dex,
            tokenA: Token,
            tokenB: Token
        ): PoolKey {

            val addresses =
                listOf(
                    tokenA.address.lowercase(),
                    tokenB.address.lowercase()
                ).sorted()

            return PoolKey(
                chain = chain,
                dex = dex,
                token0 = addresses[0],
                token1 = addresses[1]
            )
        }
    }
}