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
        val sellOrder = Order(OrderSide.SELL, 0.5, 49475.0, "BTCUSDC")
        orderBook.bids.add(buyOrder)
        orderBook.asks.add(sellOrder)

        // when ... we call the order book to retrieve it
        val currentOrderBook = orderBookService.getOrderBook()


        // then ... it should return the order book with the current orders
        assertTrue(currentOrderBook.bids.contains(buyOrder))
        assertTrue(currentOrderBook.asks.contains(sellOrder))
      }

    @Test
    fun `should return an empty order book when there are no bids or asks and the service has been initialized`() {
        // given ... an order book is yet to be initialized
        // when ... we request the order book state
        val currentOrderBook = orderBookService.getOrderBook()

        // then ...  it should return an order book with empty bids and asks
        assertTrue(currentOrderBook.bids.isEmpty())
        assertTrue(currentOrderBook.asks.isEmpty())
    }

}
