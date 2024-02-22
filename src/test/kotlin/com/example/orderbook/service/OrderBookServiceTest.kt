package com.example.orderbook.service

import com.example.orderbook.model.*
import io.mockk.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderBookServiceTest {
    private lateinit var orderBookService: OrderBookService
    private val tradeService = mockk<TradeService>(relaxed = true)
    private val orderValidator = mockk<OrderValidator>()
    private val orderManager = mockk<OrderManager>(relaxed = true)
    private val matchingEngine = mockk<MatchingEngine>(relaxed = true)
    private val orderBook = OrderBook(
        bids = TreeMap<BigDecimal, TreeMap<Instant, Order>>(compareByDescending { it }),
        asks = TreeMap<BigDecimal, TreeMap<Instant, Order>>(compareBy { it }),
        lastUpdated = Instant.now(),
        tradeSequenceNumber = 0L
    )

    @BeforeEach
    fun setUp() {
        every { orderValidator.isValidOrder(any()) } returns true
        orderBookService = OrderBookService(orderBook, tradeService, orderValidator, orderManager, matchingEngine)
    }

    @Test
    fun `should return an empty order book when there are no bids or asks and the service has been initialized`() {
        // given ... an order book is yet to have orders added
        // when ... we request the order book state
        val orderBookSummary = orderBookService.getOrderBookDTO()

        // then ... it should return an order book with empty bids and asks
        assertTrue(orderBookSummary.bids.isEmpty())
        assertTrue(orderBookSummary.asks.isEmpty())
    }

    @Test
    fun `should reject invalid orders`() {
        every { orderValidator.isValidOrder(any()) } returns false

        val invalidOrder = Order(OrderSide.BUY, BigDecimal("-1"), BigDecimal("10000"), "BTCUSDC", Instant.now())
        val result = orderBookService.addOrder(invalidOrder)

        assertFalse(result.success)
        assertEquals(
            "Invalid order details. Please ensure quantity and price are > 0 and that you've submitted a correct currency pair.",
            result.message
        )
    }

    @Test
    fun `should accurately reflect the status for fully matched orders on the taker side`() {
        // given ... a buy and sell order at the same price but with different amounts
        val buyOrder = Order(
            OrderSide.BUY,
            BigDecimal("1.0"),
            BigDecimal("10000"),
            "BTCUSDC",
            Instant.parse("2024-02-18T19:20:00Z")
        )
        val sellOrderTaker = Order(
            OrderSide.SELL,
            BigDecimal("0.5"),
            BigDecimal("10000"),
            "BTCUSDC",
            Instant.parse("2024-02-18T19:30:00Z")
        )

        every { matchingEngine.matchOrders() } returns OrderMatchingStatus(
            isOrderMatched = true,
            totalMatchedQuantity = BigDecimal("0.5"),
            fulfilledPrice = BigDecimal("10000")
        )

        every { orderManager.createAdditionStatus(any(), any()) } answers {
            val order = firstArg<Order>()
            if (order == sellOrderTaker) {
                OrderAdditionStatus(true, "The sell order is fully filled", true, false, BigDecimal.ZERO, null)
            } else {
                OrderAdditionStatus(true, "The buy order is partially filled", true, false, BigDecimal("0.5"), null)
            }
        }

        // when ... we  match the orders
        val partialBuyOrderStatus = orderBookService.addOrder(buyOrder)
        val sellOrderStatus = orderBookService.addOrder(sellOrderTaker)

        // then ... we assert that the taker's status is correctly updated
        assertTrue(sellOrderStatus.success, "The sell order is fully filled")
        assertTrue(partialBuyOrderStatus.success, "The buy order is partially filled")
        assertTrue(sellOrderStatus.isOrderMatched, "The sell order is matched")
        assertEquals(BigDecimal.ZERO, sellOrderStatus.remainingQuantity, "The sell order has no remaining quantity")
        assertEquals(
            BigDecimal("0.5"),
            partialBuyOrderStatus.remainingQuantity,
            "The buy order still has 0.5BTC remaining quantity"
        )
    }


    @Test
    fun `should accurately reflect the status for partial orders on the taker side`() {
        // given ... a buy and sell order at the same price but with different amounts
        val buyOrder = Order(
            OrderSide.BUY,
            BigDecimal("0.5"),
            BigDecimal("10000"),
            "BTCUSDC",
            Instant.parse("2024-02-18T19:20:00Z")
        )
        val sellOrderTaker = Order(
            OrderSide.SELL,
            BigDecimal("1.0"),
            BigDecimal("10000"),
            "BTCUSDC",
            Instant.parse("2024-02-18T19:30:00Z")
        )

        every { matchingEngine.matchOrders() } returns OrderMatchingStatus(
            isOrderMatched = true,
            totalMatchedQuantity = BigDecimal("0.5"),
            fulfilledPrice = BigDecimal("10000")
        )

        every { orderManager.createAdditionStatus(any(), any()) } answers {
            val order = firstArg<Order>()
            if (order == buyOrder) {
                OrderAdditionStatus(true, "The buy order is fully filled", true, false, BigDecimal.ZERO, null)
            } else {
                OrderAdditionStatus(true, "The sell order is partially filled", true, true, BigDecimal("0.5"), null)
            }
        }

        // when ... we  match the orders
        orderBookService.addOrder(buyOrder)
        val buyOrderStatus = orderBookService.addOrder(buyOrder)
        val partialSellStatus = orderBookService.addOrder(sellOrderTaker)


        // then ... we assert that the taker's status is correctly updated to reflect a partial fill
        assertTrue(buyOrderStatus.success, "The buy order is fully filled")
        assertTrue(buyOrderStatus.isOrderMatched, "The buy order is matched")
        assertTrue(partialSellStatus.partiallyFilled, "The sell order is partially filled")
        assertTrue(partialSellStatus.isOrderMatched, "The sell order is matched")
        assertEquals(BigDecimal("0.5"), partialSellStatus.remainingQuantity)
        assertEquals(BigDecimal.ZERO, buyOrderStatus.remainingQuantity, "The buy order has no remaining quantity")
    }

    @Test
    fun `should return current state of the order book when requested`() {
        every { orderManager.addOrder(any()) } answers {
            val order = firstArg<Order>()
            val orderMap = if (order.side == OrderSide.BUY) orderBook.bids else orderBook.asks
            val ordersAtPrice = orderMap.getOrDefault(order.price, TreeMap())
            ordersAtPrice[order.timestamp] = order
            orderMap[order.price] = ordersAtPrice
            orderBook.updateLastUpdated()
            orderBook.tradeSequenceNumber++


            orderBookService = OrderBookService(orderBook, tradeService, orderValidator, orderManager, matchingEngine)

            // given ... an order book exists with valid orders
            val buyOrder = Order(OrderSide.BUY, BigDecimal("0.5"), BigDecimal("49370.0"), "BTCUSDC", Instant.now())
            val sellOrder = Order(OrderSide.SELL, BigDecimal("0.7"), BigDecimal("49475.0"), "BTCUSDC", Instant.now())


            orderBookService.addOrder(buyOrder)
            orderBookService.addOrder(sellOrder)

            // when ... we call the order book to retrieve it
            val orderBookDTO = orderBookService.getOrderBookDTO()

            verify(exactly = 1) { orderManager.addOrder(buyOrder) }
            verify(exactly = 1) { orderManager.addOrder(sellOrder) }
            // then ... it should return the order book with the current orders
            assertEquals(1, orderBookDTO.bids.size)
            assertEquals(1, orderBookDTO.asks.size)
            assertEquals(OrderSide.BUY, enumValueOf<OrderSide>(orderBookDTO.bids.first().side))
            assertEquals(OrderSide.SELL, enumValueOf<OrderSide>(orderBookDTO.asks.first().side))
            assertEquals(BigDecimal("0.5"), orderBookDTO.bids.first().quantity)
            assertEquals(BigDecimal("49370.0"), orderBookDTO.bids.first().price)
            assertEquals(BigDecimal("0.7"), orderBookDTO.asks.first().quantity)
            assertEquals(BigDecimal("49475.0"), orderBookDTO.asks.first().price)
        }
    }
}
