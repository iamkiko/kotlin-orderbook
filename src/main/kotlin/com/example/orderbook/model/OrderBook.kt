package com.example.orderbook.model

import java.math.BigDecimal
import java.time.Instant
import java.util.*


data class OrderBook(
    val bids: TreeMap<BigDecimal, TreeMap<Instant, Order>> = TreeMap(reverseOrder()), // Map of price : orders <Map of time -> order>
    val asks: TreeMap<BigDecimal, TreeMap<Instant, Order>> = TreeMap(),
    var lastUpdated: Instant = Instant.now(),
    var tradeSequenceNumber: Long = 0L,
) {
    fun updateLastUpdated() {
        lastUpdated = Instant.now()
        tradeSequenceNumber++
    }

    fun getBestBid(): Map.Entry<BigDecimal, TreeMap<Instant, Order>>? {
        return bids.firstEntry()
    }

    fun getBestAsk(): Map.Entry<BigDecimal, TreeMap<Instant, Order>>? {
        return asks.firstEntry()
    }
}
