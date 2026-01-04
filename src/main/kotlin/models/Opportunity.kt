package models

data class Opportunity(
    val chain: String,          // "base"
    val dexBuy: String,
    val dexSell: String,
    val tokenIn: String,
    val tokenOut: String,
    val amountInRaw: String,    // BigInteger.toString()
    val quotedOutBuyRaw: String,
    val quotedOutSellRaw: String,
    val blockNumber: String,
    val ttlMs: Long,
    val minNetProfitUsd: String,
    val dryRun: Boolean = true
)

