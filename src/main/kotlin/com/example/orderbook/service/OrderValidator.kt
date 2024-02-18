package com.example.orderbook.service

import com.example.orderbook.model.Order
import java.math.BigDecimal

class OrderValidator {
    fun isValidOrder(order: Order): Boolean {
        return order.quantity > BigDecimal.ZERO && order.price > BigDecimal.ZERO && order.currencyPair == "BTCUSDC"
    }
}
