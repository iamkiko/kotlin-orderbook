package com.example.orderbook.repository

import com.example.orderbook.model.Trade

object TradeRepository {
    private val trades = mutableListOf<Trade>()

    fun addTrade(trade: Trade) {
        trades.add(trade)
    }

    fun getTrades(): List<Trade> = trades.toList()

    fun clearTrades() {
        trades.clear()
    }
}
