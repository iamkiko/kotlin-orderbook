package com.example.orderbook.service

import com.example.orderbook.model.OrderSide
import com.example.orderbook.model.Trade
import com.example.orderbook.repository.TradeRepository
import java.math.BigDecimal
import java.util.*

class TradeService {

    fun recordTrade(price: BigDecimal, quantity: BigDecimal, currencyPair: String, takerSide: OrderSide) {
        val tradeId = UUID.randomUUID().toString()

        val trade = Trade(
            id = tradeId,
            price = price,
            quantity = quantity,
            currencyPair = currencyPair,
            takerSide = takerSide
        )

        TradeRepository.addTrade(trade)
    }

    fun getTrades(): List<Trade> = TradeRepository.getTrades()
}
