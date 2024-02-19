package com.example.orderbook.service

import com.example.orderbook.model.Order
import com.example.orderbook.model.OrderBook
import com.example.orderbook.model.OrderMatchingStatus
import com.example.orderbook.model.OrderSide
import java.math.BigDecimal
import java.time.Instant
import java.util.*


class MatchingEngine(private val tradeService: TradeService, private val orderBook: OrderBook) {
    fun matchOrders(orderBook: OrderBook): OrderMatchingStatus {
        var isOrderMatched = false
        var totalMatchedQuantity = BigDecimal.ZERO
        var fulfilledTradePrice = BigDecimal.ZERO

        // TODO(): Can this ever not break and be a bug?
        while (true) {
            // exit early if no bids on either side i.e. no matchmaking can occur
            if (orderBook.bids.isEmpty() || orderBook.asks.isEmpty()) break

            // TODO(): Put this logic into the OrderBook Model/Domain
            // retrieve the top most item in this stack i.e. top order in book
            val bestBid = orderBook.bids.firstEntry()
            val bestAsk = orderBook.asks.firstEntry()

            // if price has spread, then don't match or if asks/bids don't exist
            if (bestBid == null || bestAsk == null || bestBid.key < bestAsk.key) break

            val bidOrderEntry = bestBid.value.firstEntry()
            val askOrderEntry = bestAsk.value.firstEntry()

            if (bidOrderEntry == null || askOrderEntry == null) break

            // figure out the overlap between asks/bids based on the smaller amount so it can match
            val matchQuantity = bidOrderEntry.value.quantity.min(askOrderEntry.value.quantity)

            // ensure the best price is always used for the trade
            fulfilledTradePrice = if (bestBid.key >= bestAsk.key) bestAsk.key else bestBid.key

            val takerSide = determineTakerSide(bestBid.value, bestAsk.value)

            isOrderMatched = true
            totalMatchedQuantity += matchQuantity

            tradeService.recordTrade(fulfilledTradePrice, matchQuantity, bidOrderEntry.value.currencyPair, takerSide)

            updateOrderQuantityAfterMatch(orderBook.bids, bestBid.key, bidOrderEntry.key, matchQuantity)
            updateOrderQuantityAfterMatch(orderBook.asks, bestAsk.key, askOrderEntry.key, matchQuantity)

            orderBook.updateLastUpdated()
        }

        return OrderMatchingStatus(isOrderMatched, totalMatchedQuantity, fulfilledTradePrice)
    }

    private fun determineTakerSide(bidOrders: TreeMap<Instant, Order>, askOrders: TreeMap<Instant, Order>): OrderSide {
        val earliestBidTimestamp = bidOrders.firstKey()
        val earliestAskTimestamp = askOrders.firstKey()

        return if (earliestBidTimestamp.isBefore(earliestAskTimestamp)) OrderSide.SELL else OrderSide.BUY
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
}
