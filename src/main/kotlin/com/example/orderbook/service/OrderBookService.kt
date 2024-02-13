package com.example.orderbook.service

import com.example.orderbook.api.dto.OrderBookDTO
import com.example.orderbook.api.dto.OrderDTO
import com.example.orderbook.model.Order
import com.example.orderbook.model.OrderBook
import com.example.orderbook.model.OrderSide
import java.util.*

class OrderBookService(private val orderBook: OrderBook) {

    fun addOrder(order: Order) {
        when(order.side) {
            OrderSide.BUY -> orderBook.bids.offer(order)
            OrderSide.SELL -> orderBook.asks.offer(order)
        }
        orderBook.updateLastChange()
        matchOrders()
    }

    private fun matchOrders() {
        while (true) {
            val topBid = orderBook.bids.peek() // get the top most item in this stack i.e. top order
            val topAsk = orderBook.asks.peek()

            // if price has spread, then don't match
            if (topBid == null || topAsk == null || topBid.price < topAsk.price) break

            orderBook.bids.poll() // remove the top most item in this stack e.g top order when fulfilled
            orderBook.asks.poll()

            orderBook.updateLastChange()
        }
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
