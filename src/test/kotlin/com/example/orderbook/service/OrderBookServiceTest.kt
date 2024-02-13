package com.example.orderbook.service

import com.example.orderbook.model.Order
import com.example.orderbook.model.OrderBook
import com.example.orderbook.model.OrderSide
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

class OrderBookServiceTest {

    private lateinit var orderBookService: OrderBookService
    private lateinit var orderBook: OrderBook

    @BeforeEach
    fun setUp() {
        orderBook = OrderBook(PriorityQueue(), PriorityQueue(), Instant.parse("2024-02-13T00:00:00.000Z") , 0)
        orderBookService = OrderBookService(orderBook)
    }

    @Test
    fun `should return current state of the order book when requested` () {
        // given ... an order book exists with valid orders
        val buyOrder = Order(OrderSide.BUY, 0.5, 49370.0, "BTCUSDC")
        val sellOrder = Order(OrderSide.SELL, 0.7, 49475.0, "BTCUSDC")
        orderBookService.addOrder(buyOrder)
        orderBookService.addOrder(sellOrder)

        // when ... we call the order book to retrieve it
        orderBookService.matchOrders()


        // then ... it should return the order book with the current orders
        assertEquals(1, orderBook.bids.size)
        assertEquals(1, orderBook.asks.size)
        assertEquals(OrderSide.BUY, orderBook.bids.first().side)
        assertEquals(0.5, orderBook.bids.first().quantity)
        assertEquals(49370.0, orderBook.bids.first().price)
        assertEquals(0.7, orderBook.asks.first().quantity)
        assertEquals(49475.0, orderBook.asks.first().price)
      }

    @Test
    fun `should return an empty order book when there are no bids or asks and the service has been initialized`() {
        // given ... an order book is yet to be initialized
        // when ... we request the order book state
        orderBookService.matchOrders()

        // then ...  it should return an order book with empty bids and asks
        assertTrue(orderBook.bids.isEmpty())
        assertTrue(orderBook.asks.isEmpty())
    }

    @Test
    fun `should match orders and remove them from orderbook when prices match` () {
        // given ... we have buy and sell orders at the same price and amount
        val matchingBuyOrder = Order(OrderSide.BUY, 1.0, 44900.0, "BTCUSDC")
        val matchingSellOrder = Order(OrderSide.SELL, 1.0, 44900.0, "BTCUSDC")
        orderBookService.addOrder(matchingBuyOrder)
        orderBookService.addOrder(matchingSellOrder)

        // when ... we attempt to match the orders
        orderBookService.matchOrders()

        // then ... the order book is empty as we've matched all orders
        assertTrue(orderBook.bids.isEmpty())
        assertTrue(orderBook.asks.isEmpty())
      }

    @Test
    fun `should not remove orders from the order book when they do not match` () {
        // given ... we have buy and sell orders and different amounts
        val buyOrder = Order(OrderSide.BUY, 0.2, 37999.0, "BTCUSDC")
        val sellOrder = Order(OrderSide.SELL, 0.2, 43999.0, "BTCUSDC")
        orderBookService.addOrder(buyOrder)
        orderBookService.addOrder(sellOrder)

        // when ... we attempt to match the orders
        orderBookService.matchOrders()

        // then ... the order book remains unchanged as the buys and sells aren't matched
        assertFalse(orderBook.bids.isEmpty())
        assertFalse(orderBook.asks.isEmpty())
        assertEquals(37999.0, orderBook.bids.first().price)
        assertEquals(43999.0, orderBook.asks.first().price)
      }

    @Test
    fun `should partially match orders and update the remaining quantity correctly` () {
        // given ... a buy and sell order at the same price but with different amounts
        val buyOrder = Order(OrderSide.BUY, 1.0, 40000.0, "BTCUSDC")
        val sellOrder = Order(OrderSide.SELL, 0.5, 40000.0, "BTCUSDC")
        orderBookService.addOrder(buyOrder)
        orderBookService.addOrder(sellOrder)

        // when ... we  match the orders
        orderBookService.matchOrders()

        // then ... we can confirm that the sell order is no longer on the order book but the buy order has only been partially filled
        assertTrue(orderBook.asks.isEmpty())
        assertEquals(0.5, orderBook.bids.first().quantity)
      }

    // TODO(): Add logic to get this test to pass
    @Test
    fun `should add multiple orders and ensure correct sorting` () {
        // given ... multiple orders
        val lowestBuyOrder = Order(OrderSide.BUY, 1.0, 30000.0, "BTCUSDC")
        val lowBuyOrder = Order(OrderSide.BUY, 1.0, 31000.0, "BTCUSDC")
        val highestBuyOrder = Order(OrderSide.BUY, 1.0, 32000.0, "BTCUSDC")

        val lowestSellOrder = Order(OrderSide.BUY, 1.0, 40000.0, "BTCUSDC")
        val lowSellOrder = Order(OrderSide.BUY, 1.0, 41000.0, "BTCUSDC")
        val highestSellOrder = Order(OrderSide.BUY, 1.0, 42000.0, "BTCUSDC")

        // when ... we add the orders to the orderbook
        orderBookService.addOrder(lowestBuyOrder)
        orderBookService.addOrder(lowBuyOrder)
        orderBookService.addOrder(highestBuyOrder)
        orderBookService.addOrder(lowestSellOrder)
        orderBookService.addOrder(lowSellOrder)
        orderBookService.addOrder(highestSellOrder)


        // then ... we can confirm that the highest buy order and the lowest sell order are the first orders around the spread
        assertEquals(highestBuyOrder, orderBook.bids.peek())

      }

    // TODO() add the logic for price-time to get this to pass
    @Test
    fun `should fill orders fully and partially up to a certain price based on price-time priority` () {
        // given ... buy orders which are >= current sell orders
        val sellOrderAtCurrentPrice = Order(OrderSide.SELL, 1.0, 40000.0, "BTCUSDC")
        val sellOrderAtLowestPrice = Order(OrderSide.SELL, 1.0, 39500.0, "BTCUSDC")
        val buyOrderAtCurrentPrice = Order(OrderSide.BUY, 1.5, 40000.0, "BTCUSDC")
        // when ... we add the orders to the order book and match them
        orderBookService.addOrder(sellOrderAtCurrentPrice)
        orderBookService.addOrder(sellOrderAtLowestPrice)
        orderBookService.addOrder(buyOrderAtCurrentPrice)
        orderBookService.matchOrders()
        val asks = orderBookService.getOrderBookDTO().asks
        val bids = orderBookService.getOrderBookDTO().bids
        // then ... we should expect that 1 sell order of 0.5BTC remains on the ask side
        assertEquals(1, asks.size)
        assertEquals(0.5, asks.first().quantity)
        assertEquals(40000.0, asks.first().price)

        assertEquals(0, bids.size)

        assertEquals(1, orderBook.tradeSequenceNumber)
      }
}
