package com.example.orderbook.model

import java.time.Instant
import java.util.*


data class OrderBook(
    val asks: PriorityQueue<Order> = PriorityQueue(compareBy { it.price }),
    val bids: PriorityQueue<Order> = PriorityQueue(compareByDescending { it.price }),
    var lastUpdated: Instant = Instant.now(),
    var tradeSequenceNumber: Long = 0L,
) {
    fun updateLastUpdated() {
        lastUpdated = Instant.now()
        tradeSequenceNumber++
    }
}
