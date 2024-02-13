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
        val orderBookDTO = orderBookService.getOrderBookDTO()


        // then ... it should return the order book with the current orders
        assertEquals(1, orderBookDTO.bids.size)
        assertEquals(1, orderBookDTO.asks.size)
        assertEquals("BUY", orderBookDTO.bids.first().side)
        assertEquals(0.5, orderBookDTO.bids.first().quantity)
        assertEquals(49370.0, orderBookDTO.bids.first().price)
        assertEquals(0.7, orderBookDTO.asks.first().quantity)
        assertEquals(49475.0, orderBookDTO.asks.first().price)
      }

    @Test
    fun `should return an empty order book when there are no bids or asks and the service has been initialized`() {
        // given ... an order book is yet to be initialized
        // when ... we request the order book state
        val orderBookDTO = orderBookService.getOrderBookDTO()

        // then ...  it should return an order book with empty bids and asks
        assertTrue(orderBookDTO.bids.isEmpty())
        assertTrue(orderBookDTO.asks.isEmpty())
    }

    @Test
    fun `should match orders and remove them from orderbook when prices match` () {
        // given ... we have buy and sell orders at the same price and amount
        val matchingBuyOrder = Order(OrderSide.BUY, 1.0, 44900.0, "BTCUSDC")
        val matchingSellOrder = Order(OrderSide.SELL, 1.0, 44900.0, "BTCUSDC")
        orderBookService.addOrder(matchingBuyOrder)
        orderBookService.addOrder(matchingSellOrder)
        // when ... we attempt to match the orders
        val orderBookDTO = orderBookService.getOrderBookDTO()

        // then ... the order book is empty as we've matched all orders
        assertTrue(orderBookDTO.bids.isEmpty())
        assertTrue(orderBookDTO.asks.isEmpty())
      }

    @Test
    fun `should not remove orders from the order book when they do not match` () {
        // given ... we have buy and sell orders and different amounts
        val buyOrder = Order(OrderSide.BUY, 0.2, 37999.0, "BTCUSDC")
        val sellOrder = Order(OrderSide.SELL, 0.2, 43999.0, "BTCUSDC")
        orderBookService.addOrder(buyOrder)
        orderBookService.addOrder(sellOrder)
        // when ... we attempt to match the orders
        val orderBookDTO = orderBookService.getOrderBookDTO()

        // then ... the order book remains unchanged as the buys and sells aren't matched
        assertFalse(orderBookDTO.bids.isEmpty())
        assertFalse(orderBookDTO.asks.isEmpty())
        assertEquals(37999.0, orderBookDTO.bids.first().price)
        assertEquals(43999.0, orderBookDTO.asks.first().price)
      }

    @Test
    fun `should partially match orders and update the remaining quantity correctly` () {
        // given ... a buy and sell order at the same price but with different amounts
        val buyOrder = Order(OrderSide.BUY, 1.0, 40000.0, "BTCUSDC")
        val sellOrder = Order(OrderSide.SELL, 0.5, 40000.0, "BTCUSDC")
        orderBookService.addOrder(buyOrder)
        orderBookService.addOrder(sellOrder)

        // when ... we  match the orders
        val orderBookDTO = orderBookService.getOrderBookDTO()

        // then ... we can confirm that the sell order is no longer on the order book but the buy order has only been partially filled
        assertTrue(orderBookDTO.asks.isEmpty())
        assertEquals(0.5, orderBookDTO.bids.first().quantity)
      }

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
}
