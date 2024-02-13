package com.example.orderbook.service

import com.example.orderbook.api.dto.OrderBookDTO
import com.example.orderbook.api.dto.OrderDTO
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

    fun getOrderBookDTO(): OrderBookDTO {
        // converting the PriorityQueue to a list for DTO and API consumption
        val askList = orderBook.asks.map { order ->
            OrderDTO(
                side = order.side.toString(),
                quantity = order.quantity,
                price = order.price,
                currencyPair = order.currencyPair
            )
        }.toList()

        val bidList = orderBook.bids.map { order ->
            OrderDTO(
                side = order.side.toString(),
                quantity = order.quantity,
                price = order.price,
                currencyPair = order.currencyPair
            )
        }.toList()

        return OrderBookDTO(
            asks = askList,
            bids = bidList,
            lastChange = orderBook.lastChange.toString(),
            sequenceNumber = orderBook.sequenceNumber
        )
    }


    // TODO() cancel orders, take in an orderId
}
