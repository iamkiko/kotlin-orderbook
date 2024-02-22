package com.example.orderbook.service

import com.example.orderbook.api.dto.TradeDTO
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

    fun getTradeDTOs(): List<TradeDTO> = TradeRepository.getTrades().map { trade ->
        TradeDTO(
            id = trade.id,
            price = trade.price,
            quantity = trade.quantity,
            currencyPair = trade.currencyPair,
            timestamp = trade.timestamp,
            takerSide = trade.takerSide.toString()
        )
    }
}
