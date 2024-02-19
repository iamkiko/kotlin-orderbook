package com.example.orderbook.service

import com.example.orderbook.api.dto.OrderBookDTO
import com.example.orderbook.api.dto.OrderDTO
import com.example.orderbook.api.dto.OrderSummaryDTO
import com.example.orderbook.model.*
import java.math.BigDecimal
import java.time.Instant
import java.util.TreeMap

class OrderManager(
    private val orderBook: OrderBook
) {
    fun addOrder(order: Order) {
        val orderMap = if (order.side == OrderSide.BUY) orderBook.bids else orderBook.asks
        orderMap[order.price] = orderMap.getOrDefault(order.price, TreeMap()).apply {
            this[order.timestamp] = order
        }
        orderBook.updateLastUpdated()
    }

    fun createAdditionStatus(order: Order, matchResult: OrderMatchingStatus): OrderAdditionStatus {
        val originalOrder =
            order.copy(quantity = order.quantity) // we make a copy to accurately return the status to the user

        orderBook.updateLastUpdated()

        val isOrderPartiallyFilled = matchResult.totalMatchedQuantity > BigDecimal.ZERO &&
                matchResult.totalMatchedQuantity < originalOrder.quantity
        val isOrderFullyFilled = matchResult.totalMatchedQuantity >= originalOrder.quantity


        val orderDetailsQuantity = if (isOrderPartiallyFilled) {
            matchResult.totalMatchedQuantity
        } else {
            originalOrder.quantity
        }

        val fulfilledPrice = matchResult.fulfilledPrice

        val orderDetails = OrderDTO(
            side = order.side.toString(),
            quantity = orderDetailsQuantity,
            price = fulfilledPrice,
            currencyPair = order.currencyPair
        )

        val remainingQuantity = if (isOrderPartiallyFilled || isOrderFullyFilled) {
            originalOrder.quantity - matchResult.totalMatchedQuantity
        } else {
            originalOrder.quantity // preserve original quantity for status message
        }

        val message = when {
            isOrderFullyFilled -> "Order fully filled."
            isOrderPartiallyFilled -> "Order partially filled."
            else -> "Order added to book, no immediate match found, pending fulfillment."
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

    fun getOrderBookSnapshot(orderBook: OrderBook): OrderBookDTO {
        val asks = aggregateOrders(orderBook.asks, OrderSide.SELL)
        val bids = aggregateOrders(orderBook.bids, OrderSide.BUY)
        return OrderBookDTO(
            asks = asks,
            bids = bids,
            lastUpdated = orderBook.lastUpdated.toString(),
            tradeSequenceNumber = orderBook.tradeSequenceNumber
        )
    }

    private fun aggregateOrders(
        orders: TreeMap<BigDecimal, TreeMap<Instant, Order>>,
        side: OrderSide
    ): List<OrderSummaryDTO> {
        return orders.map { entry ->
            val priceLevel = entry.key
            val ordersAtPrice = entry.value
            val totalQuantity = ordersAtPrice.values.sumOf { it.quantity }
            val orderCount = ordersAtPrice.size

            OrderSummaryDTO(
                price = priceLevel,
                quantity = totalQuantity,
                orderCount = orderCount,
                side = side.toString(),
                currencyPair = ordersAtPrice.values.first().currencyPair // would need to update this to accommodate other currency pairs
            )
        }
    }
}
