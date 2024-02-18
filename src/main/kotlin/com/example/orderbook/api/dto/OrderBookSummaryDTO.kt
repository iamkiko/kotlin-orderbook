package com.example.orderbook.api.dto

import java.math.BigDecimal

data class OrderSummaryDTO(
    val side: String,
    val quantity: BigDecimal,
    val price: BigDecimal,
    val currencyPair: String,
    val orderCount: Int // # of orders at this price point
)
