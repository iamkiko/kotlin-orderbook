package com.example.orderbook.service

import com.example.orderbook.api.dto.OrderBookDTO
import com.example.orderbook.api.dto.OrderDTO
import com.example.orderbook.model.*
import java.math.BigDecimal
import kotlin.math.min

class OrderBookService(private val orderBook: OrderBook) {

    fun addOrder(order: Order): OrderAdditionStatus {
        // block invalid orders
        if (!isValidOrder(order)) {
            return OrderAdditionStatus(
                success = false,
                message = "Invalid order details. Please ensure quantity and price are > 0 and that you've submitted a correct currency pair.",
                orderDetails = null
            )
        }

        val originalQuantity = order.quantity
        val originalOrder =
            order.copy(quantity = originalQuantity) // we make a copy to accurately return the status to the user

        when (order.side) {
            OrderSide.BUY -> orderBook.bids.offer(originalOrder)
            OrderSide.SELL -> orderBook.asks.offer(originalOrder)
        }
        orderBook.updateLastUpdated()

        val matchResult = matchOrders()

        val isOrderPartiallyFilled = matchResult.totalMatchedQuantity > BigDecimal.ZERO &&
                matchResult.totalMatchedQuantity < originalOrder.quantity
        val isOrderFullyFilled = matchResult.totalMatchedQuantity >= originalOrder.quantity


        val message = when {
            isOrderFullyFilled -> "Order fully filled."
            isOrderPartiallyFilled -> "Order partially filled."
            else -> "Order added to book, no immediate match found, pending fulfillment."
        }

        val orderDetailsQuantity = if (isOrderPartiallyFilled) {
            matchResult.totalMatchedQuantity
        }
         else {
             originalQuantity
        }

        val orderDetails = OrderDTO(
            side = order.side.toString(),
            quantity = orderDetailsQuantity,
            price = order.price,
            currencyPair = order.currencyPair
        )

        val remainingQuantity = if (isOrderPartiallyFilled || isOrderFullyFilled) {
            originalQuantity - matchResult.totalMatchedQuantity
        } else {
            originalQuantity
        }

        return OrderAdditionStatus(
            success = true,
            message = message,
            isOrderMatched = matchResult.isOrderMatched,
            partiallyFilled = isOrderPartiallyFilled,
            remainingQuantity = remainingQuantity,
            orderDetails = orderDetails
        )
    }

    fun matchOrders(): OrderMatchingStatus {
        var isOrderMatched = false
        var totalMatchedQuantity = BigDecimal.ZERO

        while (true) {
            val topBid = orderBook.bids.peek() // retrieve the top most item in this stack i.e. top order in book
            val topAsk = orderBook.asks.peek()

            // if price has spread, then don't match or if asks/bids don't exist
            if (topBid == null || topAsk == null || topBid.price < topAsk.price) break

            val tradeQuantity = topBid.quantity.min(topAsk.quantity)
            topBid.quantity = topBid.quantity.subtract(tradeQuantity)
            topAsk.quantity = topAsk.quantity.subtract(tradeQuantity)

            totalMatchedQuantity += tradeQuantity

            if (topBid.quantity <= BigDecimal.ZERO) {
                orderBook.bids.poll() // order has been fulfilled, remove from book
            }

            if (topAsk.quantity <= BigDecimal.ZERO) {
                orderBook.asks.poll() // order has been fulfilled, remove from book
            }

            isOrderMatched = true

            // TODO(): will need to record this trade to be able to add it to the trade history
            orderBook.updateLastUpdated()
        }

        return OrderMatchingStatus(isOrderMatched, totalMatchedQuantity)
    }

    private fun isValidOrder(order: Order): Boolean {
        return order.quantity > BigDecimal.ZERO && order.price > BigDecimal.ZERO && order.currencyPair == "BTCUSDC"
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

    private fun Order.toDTO() = OrderDTO(
        side = this.side.toString(),
        quantity = this.quantity,
        price = this.price,
        currencyPair = this.currencyPair
    )

    // TODO() trade history, store in data structure when trade is successful

    // TODO() cancel orders, take in an orderId
}
