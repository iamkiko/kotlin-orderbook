package com.example.orderbook.api.dto

data class OrderDTO(
    val side: String,
    val quantity: Double,
    val price: Double,
    val currencyPair: String
)
