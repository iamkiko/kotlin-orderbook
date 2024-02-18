package com.example.orderbook.model

import java.math.BigDecimal
import java.time.Instant
import java.util.*


data class Trade(
    val id: String = UUID.randomUUID().toString(),
    val price: BigDecimal,
    val quantity: BigDecimal,
    val currencyPair: String,
    val timestamp: Instant = Instant.now(),
    val takerSide: OrderSide,
)
