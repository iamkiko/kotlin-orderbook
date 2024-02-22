package com.example.orderbook.model

import com.example.orderbook.api.dto.OrderDTO
import java.math.BigDecimal

data class OrderAdditionStatus(
    val success: Boolean,
    val message: String,
    val isOrderMatched: Boolean = false,
    val partiallyFilled: Boolean = false,
    val remainingQuantity: BigDecimal = BigDecimal.ZERO,
    val orderDetails: OrderDTO? = null
)
