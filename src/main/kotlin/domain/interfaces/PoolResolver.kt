package domain.interfaces

import domain.models.Chain
import domain.models.Dex
import domain.models.Pool
import domain.models.registries.Token

interface PoolResolver {

    val dex: Dex

    fun resolve(
        chain: Chain,
        tokenA: Token,
        tokenB: Token
    ): Pool?
}