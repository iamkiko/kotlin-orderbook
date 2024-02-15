package com.example.orderbook.model

import java.math.BigDecimal

data class OrderMatchingStatus(val isOrderMatched: Boolean, val totalMatchedQuantity: BigDecimal)
