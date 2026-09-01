package domain.models

import domain.models.registries.Token

data class DexPair(
    val routeA: DexRoute,
    val routeB: DexRoute,
    val label: String
) {

    init {
        require(routeA.chain == routeB.chain) {
            "Both routes must belong to the same chain"
        }

        require(routeA.tokenIn == routeB.tokenIn) {
            "Both routes must use the same tokenIn"
        }

        require(routeA.tokenOut == routeB.tokenOut) {
            "Both routes must use the same tokenOut"
        }

        require(routeA.dex != routeB.dex) {
            "Routes must use different DEXes"
        }
    }

    val chain: Chain
        get() = routeA.chain

    val tokenIn: Token
        get() = routeA.tokenIn

    val tokenOut: Token
        get() = routeA.tokenOut
}