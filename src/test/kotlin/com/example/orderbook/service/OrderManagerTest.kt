package com.example.orderbook.service

import com.example.orderbook.model.Order
import com.example.orderbook.model.OrderBook
import com.example.orderbook.model.OrderMatchingStatus
import com.example.orderbook.model.OrderSide
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class OrderManagerTest {
    private lateinit var orderManager: OrderManager
    private lateinit var orderBook: OrderBook

    @BeforeEach
    fun setUp() {
        orderBook = OrderBook(
            bids = TreeMap<BigDecimal, TreeMap<Instant, Order>>(reverseOrder()),
            asks = TreeMap<BigDecimal, TreeMap<Instant, Order>>(),
            lastUpdated = Instant.parse("2024-02-19T00:00:00.000Z"),
            tradeSequenceNumber = 0L
        )
        orderManager = OrderManager(orderBook)
    }

    @Test
    fun `should add a new buy order to the order book on the bids map`() {
        // given ... a new buy order
        val order = Order(OrderSide.BUY, BigDecimal("1.0"), BigDecimal("10000"), "BTCUSDC")

        // when ... we add the order to the order book
        orderManager.addOrder(order)

        // then ... the order book should have the new order in the bids
        assertTrue(orderBook.bids.containsKey(order.price))
        assertEquals(order, orderBook.bids[order.price]?.get(order.timestamp))
    }

    @Test
    fun `should add a new sell order to the order book on the asks map`() {
        // given ... a new sell order
        val order = Order(OrderSide.SELL, BigDecimal("2.0"), BigDecimal("20000"), "BTCUSDC")

        // when ... we add the order to the order book
        orderManager.addOrder(order)

        // then ... the order book should have the new order in the asks
        assertTrue(orderBook.asks.containsKey(order.price))
        assertEquals(order, orderBook.asks[order.price]?.get(order.timestamp))
    }

    @Test
    fun `should fully fill an order and update the status when an order can be fully matched`() {
        // given ... an outstanding order that can be 100% matched
        val order = Order(OrderSide.BUY, BigDecimal("1.0"), BigDecimal("10000"), "BTCUSDC")
        val matchResult = OrderMatchingStatus(true, BigDecimal("1.0"), BigDecimal("10000"))

        // when ... the order addition status is created
        val status = orderManager.createAdditionStatus(order, matchResult)

        // then ... we can confirm that the order was fully filled and status updated
        assertTrue(status.success)
        assertEquals("Order fully filled.", status.message)
        assertTrue(status.isOrderMatched)
        assertFalse(status.partiallyFilled)
        assertTrue(status.remainingQuantity.compareTo(BigDecimal.ZERO) == 0) // TODO: is there a better way to compare the big decimal 0?
        assertNotNull(status.orderDetails)
        assertEquals(order.side.toString(), status.orderDetails?.side)
        assertEquals(matchResult.totalMatchedQuantity, status.orderDetails?.quantity)
        assertEquals(matchResult.fulfilledPrice, status.orderDetails?.price)
    }

    @Test
    fun `should partially fill an order and update the status when an order can only be partially matched`() {
        // given ... an outstanding order that can be partially matched
        val order = Order(OrderSide.BUY, BigDecimal("2.0"), BigDecimal("10000"), "BTCUSDC")
        val matchResult = OrderMatchingStatus(true, BigDecimal("1.0"), BigDecimal("10000"))

        // when ... the order addition status is created
        val status = orderManager.createAdditionStatus(order, matchResult)

        // then ... we can confirm that the order was partially filled and status updated
        assertTrue(status.success)
        assertEquals("Order partially filled.", status.message)
        assertTrue(status.isOrderMatched)
        assertTrue(status.partiallyFilled)
        assertEquals(BigDecimal("1.0"), status.remainingQuantity)
        assertNotNull(status.orderDetails)
    }

    @Test
    fun `should add an unmatched order to the orderbook without filling it`() {
        // given ... an outstanding order that cannot be immediately matched
        val order = Order(OrderSide.BUY, BigDecimal("1.0"), BigDecimal("10000"), "BTCUSDC")
        val matchResult = OrderMatchingStatus(false, BigDecimal.ZERO, BigDecimal.ZERO)

        // when ... the order addition status is created
        val status = orderManager.createAdditionStatus(order, matchResult)

        // then ... we can confirm that the order appears on the order book without being filled
        assertTrue(status.success)
        assertEquals("Order added to book, no immediate match found, pending fulfillment.", status.message)
        assertFalse(status.isOrderMatched)
        assertFalse(status.partiallyFilled)
        assertEquals(order.quantity, status.remainingQuantity)
    }

    @Test
    fun `should aggregate orders correctly`() {
        // given ... 2 buy orders at the same price and 1 sell order
        val buyOrder1 = Order(OrderSide.BUY, BigDecimal("1.0"), BigDecimal("10000"), "BTCUSDC", Instant.parse("2024-02-18T19:30:50Z"))
        val buyOrder2 = Order(OrderSide.BUY, BigDecimal("3.0"), BigDecimal("10000"), "BTCUSDC", Instant.parse("2024-02-18T19:30:20Z"))
        val buyOrder3 = Order(OrderSide.BUY, BigDecimal("2.0"), BigDecimal("10000"), "BTCUSDC", Instant.now())
        val sellOrder = Order(OrderSide.SELL, BigDecimal("1.0"), BigDecimal("20000"), "BTCUSDC", Instant.now())

        // when ... added to the orderbook and a snapshot of the current state is retrieved
        orderManager.addOrder(buyOrder1)
        orderManager.addOrder(buyOrder2)
        orderManager.addOrder(buyOrder3)
        orderManager.addOrder(sellOrder)
        val snapshot = orderManager.getOrderBookSnapshot(orderBook)

        // then ... we can confirm the state of the orderbook is as expected
        assertEquals(1, snapshot.bids.size, "There should be one aggregated bid")
        assertEquals(1, snapshot.asks.size, "There should be one ask")
        assertEquals(BigDecimal("6.0"), snapshot.bids.first().quantity, "The aggregated bid quantity should be 3.0")
        assertEquals(BigDecimal("10000"), snapshot.bids.first().price, "The bid price should be 10000")
        assertEquals(BigDecimal("20000"), snapshot.asks.first().price, "The ask price should be 20000")
    }

    @Test
    fun `should add multiple orders and ensure correct sorting`() {
        // given ... buy and sell orders added in a non-sorted order
        val middlePriceBuyOrder = Order(OrderSide.BUY, BigDecimal("0.5"), BigDecimal("29000.0"), "BTCUSDC")
        val highestPriceBuyOrder = Order(OrderSide.BUY, BigDecimal("1.2"), BigDecimal("35000.0"), "BTCUSDC")
        val lowestPriceBuyOrder = Order(OrderSide.BUY, BigDecimal("4.0"), BigDecimal("22000.0"), "BTCUSDC")
        val lowestPriceSellOrder = Order(OrderSide.SELL, BigDecimal("3.0"), BigDecimal("36000.0"), "BTCUSDC")
        val middlePriceSellOrder = Order(OrderSide.SELL, BigDecimal("1.1"), BigDecimal("41000.0"), "BTCUSDC")
        val highestPriceSellOrder = Order(OrderSide.SELL, BigDecimal("1.0"), BigDecimal("48000.0"), "BTCUSDC")

        // when ... we add the orders to the orderbook
        orderManager.addOrder(middlePriceBuyOrder)
        orderManager.addOrder(highestPriceBuyOrder)
        orderManager.addOrder(lowestPriceBuyOrder)
        orderManager.addOrder(lowestPriceSellOrder)
        orderManager.addOrder(middlePriceSellOrder)
        orderManager.addOrder(highestPriceSellOrder)

        // then ... we can verify the entire order of the buy and sell queues
        val buyOrderPrices = orderBook.bids.keys.toList()
        val sellOrderPrices = orderBook.asks.keys.toList()

        val expectedBuyOrderPrices =
            listOf(highestPriceBuyOrder.price, middlePriceBuyOrder.price, lowestPriceBuyOrder.price)
        val expectedSellOrderPrices =
            listOf(lowestPriceSellOrder.price, middlePriceSellOrder.price, highestPriceSellOrder.price)
        assertEquals(expectedBuyOrderPrices, buyOrderPrices)
        assertEquals(expectedSellOrderPrices, sellOrderPrices)
        // and ... we can confirm that the highest buy order and the lowest sell order are the first orders around the spread
        assertEquals(highestPriceBuyOrder.price, orderBook.bids.firstKey())
        assertEquals(lowestPriceSellOrder.price, orderBook.asks.firstKey())
    }

    @Test
    fun `should return an empty snapshot for an empty order book`() {
        // given ... an empty order book

        // when ... a snapshot of the current state is retrieved
        val snapshot = orderManager.getOrderBookSnapshot(orderBook)

        // then ... we can confirm the snapshot is empty
        assertTrue(snapshot.bids.isEmpty())
        assertTrue(snapshot.asks.isEmpty())
    }
}
