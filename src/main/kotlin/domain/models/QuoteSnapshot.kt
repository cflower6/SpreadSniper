package domain.models

import java.math.BigInteger

data class QuoteSnapshot(
    val dex: Dex,
    val amountOutRaw: BigInteger,
    val feeRate: Double
)