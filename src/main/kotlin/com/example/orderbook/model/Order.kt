package com.example.orderbook.model

import java.math.BigDecimal
import java.time.Instant

enum class OrderSide {
    BUY, SELL
}

data class Order(
//    val id: String, TODO(): come back to this e.g. cancelling orders as a stretch goal
    val side: OrderSide,
    var quantity: BigDecimal,
    val price: BigDecimal,
    val currencyPair: String,
    val timestamp: Instant = Instant.now()
)
