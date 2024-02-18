package com.example.orderbook.api.dto

data class OrderBookDTO(
    val asks: List<OrderSummaryDTO>,
    val bids: List<OrderSummaryDTO>,
    val lastUpdated: String,
    val tradeSequenceNumber: Long
)
