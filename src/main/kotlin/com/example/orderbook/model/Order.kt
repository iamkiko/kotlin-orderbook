package com.example.orderbook.model

enum class OrderSide {
    BUY, SELL
}

data class Order(
//    val id: String, TODO(): come back to this e.g. cancelling orders as a stretch goal
    val side: OrderSide,
    val quantity: Double,
    val price: Double,
    val currencyPair: String,
)
