package com.example.orderbook.api.dto

import java.math.BigDecimal

data class OrderBookDTO(
    val asks: List<OrderSummaryDTO>,
    val bids: List<OrderSummaryDTO>,
    val lastUpdated: String,
    val tradeSequenceNumber: Long
)

data class OrderSummaryDTO(
    val side: String,
    val quantity: BigDecimal,
    val price: BigDecimal,
    val currencyPair: String,
    val orderCount: Int // # of orders at this price point
)
