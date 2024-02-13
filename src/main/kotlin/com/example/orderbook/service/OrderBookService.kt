package com.example.orderbook.service

import com.example.orderbook.api.dto.OrderBookDTO
import com.example.orderbook.api.dto.OrderDTO
import com.example.orderbook.model.Order
import com.example.orderbook.model.OrderBook
import com.example.orderbook.model.OrderSide
import kotlin.math.min

class OrderBookService(private val orderBook: OrderBook) {

    fun addOrder(order: Order) {
        when(order.side) {
            OrderSide.BUY -> orderBook.bids.offer(order)
            OrderSide.SELL -> orderBook.asks.offer(order)
        }
        orderBook.updateLastUpdated()
        matchOrders()
    }

    fun matchOrders() {
        while (true) {
            val topBid = orderBook.bids.peek() // get the top most item in this stack i.e. top order
            val topAsk = orderBook.asks.peek()

            // if price has spread, then don't match or if asks/bids don't exist
            if (topBid == null || topAsk == null || topBid.price < topAsk.price) break

           val tradeQuantity = min(topBid.quantity, topAsk.quantity)
            topBid.quantity -= tradeQuantity
            topAsk.quantity -= tradeQuantity

            if (topBid.quantity <= 0.0) {
                orderBook.bids.poll()
            }

            if (topAsk.quantity <= 0.0) {
                orderBook.asks.poll()
            }

            // TODO(): will need to record this trade to be able to add it to the trade history
            orderBook.updateLastUpdated()
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
            lastUpdated = orderBook.lastUpdated.toString(),
            tradeSequenceNumber = orderBook.tradeSequenceNumber
        )
    }

    // TODO() trade history, store in data structure when trade is successful

    // TODO() cancel orders, take in an orderId
}
