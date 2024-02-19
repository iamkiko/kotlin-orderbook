package com.example.orderbook.service

import com.example.orderbook.api.dto.OrderBookDTO
import com.example.orderbook.api.dto.OrderDTO
import com.example.orderbook.api.dto.OrderSummaryDTO
import com.example.orderbook.model.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class OrderBookService(
    private val orderBook: OrderBook,
    tradeService: TradeService,
) {
    private val orderValidator = OrderValidator()
    private val orderManager = OrderManager(orderBook)
    private val matchingEngine = MatchingEngine(tradeService, orderBook)

    fun addOrder(order: Order): OrderAdditionStatus {
        // block invalid orders
        if (!orderValidator.isValidOrder(order)) {
            return OrderAdditionStatus(
                success = false,
                message = "Invalid order details. Please ensure quantity and price are > 0 and that you've submitted a correct currency pair.",
                orderDetails = null
            )
        }
        orderManager.addOrder(order)
        val matchResult = matchingEngine.matchOrders(orderBook)
        return orderManager.createAdditionStatus(order, matchResult)
    }

    fun getOrderBookDTO(): OrderBookDTO = orderManager.getOrderBookSnapshot(orderBook)


    // TODO() cancel orders, take in an orderId
}
