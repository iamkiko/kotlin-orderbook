package com.example.orderbook.api.dto

import java.math.BigDecimal

data class OrderDTO(
    val side: String,
    val quantity: BigDecimal,
    val price: BigDecimal,
    val currencyPair: String
)
