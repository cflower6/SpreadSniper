package domain.models.registries

import domain.models.Chain
import domain.models.Dex
import domain.models.DexPair
import domain.models.DexRoute

object DexPairs {

    fun create(
        poolRegistry: PoolRegistry
    ): List<DexPair> {
        val wethUsdc =
            DexPair(
                routeA = DexRoute(
                    pool = poolRegistry.require(
                        chain = Chain.BASE,
                        dex = Dex.AERODROME,
                        tokenA = Tokens.WETH_BASE,
                        tokenB = Tokens.USDC_BASE
                    ),
                    tokenIn = Tokens.WETH_BASE,
                    tokenOut = Tokens.USDC_BASE
                ),

                routeB = DexRoute(
                    pool = poolRegistry.require(
                        chain = Chain.BASE,
                        dex = Dex.UNISWAP,
                        tokenA = Tokens.WETH_BASE,
                        tokenB = Tokens.USDC_BASE
                    ),
                    tokenIn = Tokens.WETH_BASE,
                    tokenOut = Tokens.USDC_BASE
                ),

                label = "Aerodrome vs Uniswap WETH/USDC"
            )

        val usdcUsdbc =
            DexPair(
                routeA = DexRoute(
                    pool = poolRegistry.require(
                        chain = Chain.BASE,
                        dex = Dex.AERODROME,
                        tokenA = Tokens.USDC_BASE,
                        tokenB = Tokens.USDBC_BASE
                    ),
                    tokenIn = Tokens.USDC_BASE,
                    tokenOut = Tokens.USDBC_BASE
                ),

                routeB = DexRoute(
                    pool = poolRegistry.require(
                        chain = Chain.BASE,
                        dex = Dex.UNISWAP,
                        tokenA = Tokens.USDC_BASE,
                        tokenB = Tokens.USDBC_BASE
                    ),
                    tokenIn = Tokens.USDC_BASE,
                    tokenOut = Tokens.USDBC_BASE
                ),

                label = "Aerodrome vs Uniswap USDC/USDbC"
            )

        val wethCbeth =
            DexPair(
                routeA = DexRoute(
                    pool = poolRegistry.require(
                        chain = Chain.BASE,
                        dex = Dex.AERODROME,
                        tokenA = Tokens.WETH_BASE,
                        tokenB = Tokens.CBETH_BASE
                    ),
                    tokenIn = Tokens.WETH_BASE,
                    tokenOut = Tokens.CBETH_BASE
                ),

                routeB = DexRoute(
                    pool = poolRegistry.require(
                        chain = Chain.BASE,
                        dex = Dex.UNISWAP,
                        tokenA = Tokens.WETH_BASE,
                        tokenB = Tokens.CBETH_BASE
                    ),
                    tokenIn = Tokens.WETH_BASE,
                    tokenOut = Tokens.CBETH_BASE
                ),

                label = "Aerodrome vs Uniswap WETH/cbETH"
            )

        val wethAero =
            DexPair(
                routeA = DexRoute(
                    pool = poolRegistry.require(
                        chain = Chain.BASE,
                        dex = Dex.AERODROME,
                        tokenA = Tokens.WETH_BASE,
                        tokenB = Tokens.AERO_BASE
                    ),
                    tokenIn = Tokens.WETH_BASE,
                    tokenOut = Tokens.AERO_BASE
                ),

                routeB = DexRoute(
                    pool = poolRegistry.require(
                        chain = Chain.BASE,
                        dex = Dex.UNISWAP,
                        tokenA = Tokens.WETH_BASE,
                        tokenB = Tokens.AERO_BASE
                    ),
                    tokenIn = Tokens.WETH_BASE,
                    tokenOut = Tokens.AERO_BASE
                ),

                label = "Aerodrome vs Uniswap WETH/AERO"
            )

        return listOf(
            wethUsdc,
            usdcUsdbc,
            wethCbeth,
            wethAero
        )
    }
}