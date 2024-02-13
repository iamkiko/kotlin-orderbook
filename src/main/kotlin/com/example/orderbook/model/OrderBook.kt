package com.example.orderbook.model

import java.time.Instant
import java.util.*


data class OrderBook(
    val asks: PriorityQueue<Order> =  PriorityQueue(compareBy { it.price }),
    val bids: PriorityQueue<Order> = PriorityQueue(compareByDescending { it.price }),
    var lastChange: Instant = Instant.now(),
    var sequenceNumber: Long = 0L,
) {
    fun updateLastChange() {
        lastChange = Instant.now()
        sequenceNumber++
    }
}
