package application.orchestrator

import domain.models.Dex
import java.math.BigInteger

data class RoundTripQuote(
    val firstDex: Dex,
    val secondDex: Dex,

    val amountInRaw: BigInteger,
    val intermediateAmountRaw: BigInteger,
    val finalAmountRaw: BigInteger
) {

    val profitRaw: BigInteger
        get() =
            finalAmountRaw - amountInRaw
}