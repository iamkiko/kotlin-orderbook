package com.example.orderbook.service

import com.example.orderbook.model.Order
import com.example.orderbook.model.OrderBook
import java.util.*

class OrderBookService(private val orderBook: OrderBook) {

    private val askComparator = compareBy<Order> { it.price }
    private val bidComparator = compareByDescending<Order> { it.price }

    private val asks = PriorityQueue(askComparator)
    private val bids = PriorityQueue(bidComparator)


    fun getOrderBook(): OrderBook {
        return OrderBook(
            asks = asks,
            bids = bids,
            lastChange = orderBook.lastChange,
            sequenceNumber = orderBook.sequenceNumber
        )
    }

    // TODO() cancel orders, take in an orderId
}
