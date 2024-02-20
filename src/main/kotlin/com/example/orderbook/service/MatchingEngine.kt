package com.example.orderbook.service

import com.example.orderbook.model.Order
import com.example.orderbook.model.OrderBook
import com.example.orderbook.model.OrderMatchingStatus
import com.example.orderbook.model.OrderSide
import java.math.BigDecimal
import java.time.Instant
import java.util.*


class MatchingEngine(private val tradeService: TradeService, private val orderBook: OrderBook) {

    fun matchOrders(): OrderMatchingStatus {
        var isOrderMatched = false
        var totalMatchedQuantity = BigDecimal.ZERO
        var fulfilledTradePrice = BigDecimal.ZERO

        /* Continuously attempt to match bids and asks until:
           1) There are no more bids (buys) or asks (sells) available on orderbook
           2) A spread exists: best bid is less than the best ask, indicating no overlap and thus no possibility for a match. */

        while (canMatchOrders()) {

            // retrieve the top most item in this stack i.e. top order in book
            val bestBid = orderBook.getBestBid()
            val bestAsk = orderBook.getBestAsk()

            // if price has spread, then don't match or if asks/bids don't exist
            if (bestBid == null || bestAsk == null || bestBid.key < bestAsk.key) break

            val matchResult = performSingleMatch(bestBid, bestAsk)

            if (matchResult != null) {
                isOrderMatched = true
                totalMatchedQuantity += matchResult.matchQuantity
                fulfilledTradePrice = matchResult.fulfilledTradePrice

                updateOrderQuantityAfterMatch(orderBook.bids, bestBid.key, bestBid.value.firstKey(), matchResult.matchQuantity)
                updateOrderQuantityAfterMatch(orderBook.asks, bestAsk.key, bestAsk.value.firstKey(), matchResult.matchQuantity)

                orderBook.updateLastUpdated()
            }
        }

        return OrderMatchingStatus(isOrderMatched, totalMatchedQuantity, fulfilledTradePrice)
    }

    private fun canMatchOrders(): Boolean {
        // exit early if no bids on either side i.e. no matchmaking can occur
        return orderBook.bids.isNotEmpty() && orderBook.asks.isNotEmpty()
    }


    private fun performSingleMatch(
        bestBid: Map.Entry<BigDecimal, TreeMap<Instant, Order>>,
        bestAsk: Map.Entry<BigDecimal, TreeMap<Instant, Order>>
    ): MatchResult? {
        val bidOrderEntry = bestBid.value.firstEntry()
        val askOrderEntry = bestAsk.value.firstEntry()

        if (bidOrderEntry == null || askOrderEntry == null) return null

        // figure out the overlap between asks/bids based on the smaller amount so it can match
        val matchQuantity = bidOrderEntry.value.quantity.min(askOrderEntry.value.quantity)

        // ensure the best price is always used for the trade
        val fulfilledTradePrice = if (bestBid.key >= bestAsk.key) bestAsk.key else bestBid.key

        // determine the taker side based on the timestamps of the orders
        val takerSide = determineTakerSide(bestBid.value, bestAsk.value)

        tradeService.recordTrade(fulfilledTradePrice, matchQuantity, bidOrderEntry.value.currencyPair, takerSide)

        return MatchResult(matchQuantity, fulfilledTradePrice)
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
        val order = ordersAtPrice[orderTime] ?: return // retrieve by timestamp

        val newQuantity = order.quantity - matchQuantity
        if (newQuantity <= BigDecimal.ZERO) {
            ordersAtPrice.remove(orderTime) // fully matched
            if (ordersAtPrice.isEmpty()) {
                orderMap.remove(priceLevel) // remove price level as it's now empty
            }
        } else {
            // quantity at this price level still available
            ordersAtPrice[orderTime] = order.copy(quantity = newQuantity)
        }
    }

    data class MatchResult(
        val matchQuantity: BigDecimal,
        val fulfilledTradePrice: BigDecimal
    )

}
