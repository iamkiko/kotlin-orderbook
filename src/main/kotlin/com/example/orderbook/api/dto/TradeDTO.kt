package com.example.orderbook.api.dto

import java.math.BigDecimal
import java.time.Instant

data class TradeDTO(
    val id: String,
    val price: BigDecimal,
    val quantity: BigDecimal,
    val currencyPair: String,
    val timestamp: Instant,
    val takerSide: String
)
