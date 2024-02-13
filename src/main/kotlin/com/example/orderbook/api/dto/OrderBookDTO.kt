package com.example.orderbook.api.dto

data class OrderBookDTO(
    val asks: List<OrderDTO>,
    val bids: List<OrderDTO>,
    val lastUpdated: String,
    val tradeSequenceNumber: Long
)
