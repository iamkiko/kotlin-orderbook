package com.example.orderbook.service

import com.example.orderbook.api.dto.OrderBookDTO
import com.example.orderbook.api.dto.OrderDTO
import com.example.orderbook.api.dto.OrderSummaryDTO
import com.example.orderbook.model.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class OrderBookService(private val orderBook: OrderBook, private val tradeService: TradeService) {


    fun addOrder(order: Order): OrderAdditionStatus {
        // block invalid orders
        if (!isValidOrder(order)) {
            return OrderAdditionStatus(
                success = false,
                message = "Invalid order details. Please ensure quantity and price are > 0 and that you've submitted a correct currency pair.",
                orderDetails = null
            )
        }

        val orderMap = if (order.side == OrderSide.BUY) {
            orderBook.bids
        } else {
            orderBook.asks
        }

        // aggregate the orders by price level
        // TODO(): rewrite this
        orderMap[order.price] = orderMap.getOrDefault(order.price, TreeMap()).apply {
            this[order.timestamp] = order
        }

        val originalOrder =
            order.copy(quantity = order.quantity) // we make a copy to accurately return the status to the user

        orderBook.updateLastUpdated()

        val matchResult = matchOrders()

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

    fun matchOrders(): OrderMatchingStatus {
        var isOrderMatched = false
        var totalMatchedQuantity = BigDecimal.ZERO
        var fulfiledTradePrice = BigDecimal.ZERO

        // TODO(): Can this ever not break and be a bug?
        while (true) {
            // exit early if no bids on either side i.e. no matchmaking can occur
            if (orderBook.bids.isEmpty() || orderBook.asks.isEmpty()) break

            // retrieve the top most item in this stack i.e. top order in book
            val bestBid = orderBook.bids.firstEntry() // TODO(): Put this logic into the OrderBook Model/Domain
            val bestAsk = orderBook.asks.firstEntry()

            // if price has spread, then don't match or if asks/bids don't exist
            if (bestBid.key < bestAsk.key) break

            val bidOrderEntry = bestBid.value.firstEntry()
            val askOrderEntry = bestAsk.value.firstEntry()

            // figure out the overlap between asks/bids based on the smaller amount so it can match
            val matchQuantity = bidOrderEntry.value.quantity.min(askOrderEntry.value.quantity)

            val takerSide = if (bidOrderEntry.value.timestamp.isBefore(askOrderEntry.value.timestamp)) {
                OrderSide.BUY
            } else {
                OrderSide.SELL
            }

            isOrderMatched = true
            totalMatchedQuantity += matchQuantity

            fulfiledTradePrice = if (takerSide == OrderSide.BUY) bestBid.key else bestAsk.key // determine which side initiated the order to get price.
            tradeService.recordTrade(fulfiledTradePrice, matchQuantity, bidOrderEntry.value.currencyPair, takerSide)

            updateOrderQuantityAfterMatch(orderBook.bids, bestBid.key, bidOrderEntry.key, matchQuantity)
            updateOrderQuantityAfterMatch(orderBook.asks, bestAsk.key, askOrderEntry.key, matchQuantity)

            orderBook.updateLastUpdated()
        }

        return OrderMatchingStatus(isOrderMatched, totalMatchedQuantity, fulfiledTradePrice)
    }

    private fun isValidOrder(order: Order): Boolean {
        return order.quantity > BigDecimal.ZERO && order.price > BigDecimal.ZERO && order.currencyPair == "BTCUSDC"
    }

    private fun updateOrderQuantityAfterMatch(
        orderMap: TreeMap<BigDecimal, TreeMap<Instant, Order>>,
        priceLevel: BigDecimal,
        orderTime: Instant,
        matchQuantity: BigDecimal
    ) {
        val ordersAtPrice = orderMap[priceLevel] ?: return
        val order = ordersAtPrice[orderTime] ?: return

        val newQuantity = order.quantity - matchQuantity
        if (newQuantity <= BigDecimal.ZERO) {
            ordersAtPrice.remove(orderTime)
            if (ordersAtPrice.isEmpty()) {
                orderMap.remove(priceLevel)
            }
        } else {
            ordersAtPrice[orderTime] = order.copy(quantity = newQuantity)
        }
    }

    fun getOrderBookDTO(): OrderBookDTO {
        val asks = aggregateOrders(orderBook.asks, OrderSide.SELL)
        val bids = aggregateOrders(orderBook.bids, OrderSide.BUY)

        return OrderBookDTO(
            asks = asks,
            bids = bids,
            lastUpdated = orderBook.lastUpdated.toString(),
            tradeSequenceNumber = orderBook.tradeSequenceNumber
        )
    }

    private fun aggregateOrders(orders: TreeMap<BigDecimal, TreeMap<Instant, Order>>, side: OrderSide): List<OrderSummaryDTO> {
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

    // TODO() cancel orders, take in an orderId
}
