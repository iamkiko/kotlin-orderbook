package com.example.orderbook.model

import com.example.orderbook.api.dto.OrderDTO

data class OrderAdditionStatus(
        val success: Boolean,
        val message: String,
        val isOrderMatched: Boolean = false,
        val partiallyFilled: Boolean = false,
        val remainingQuantity: Double = 0.0,
        val orderDetails: OrderDTO? = null
    )
