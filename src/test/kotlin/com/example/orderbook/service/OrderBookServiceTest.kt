package com.example.orderbook.service
import com.example.orderbook.api.dto.OrderDTO
import com.example.orderbook.model.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
        orderBookService = OrderBookService(orderBook, tradeService)
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
        assertEquals("Invalid order details. Please ensure quantity and price are > 0 and that you've submitted a correct currency pair.", result.message)
    }

    @Test
    fun `should accurately reflect the status for fully matched orders on the taker side`() {
        // given ... a buy and sell order at the same price but with different amounts
        val buyOrder = Order(OrderSide.BUY, BigDecimal("1.0"), BigDecimal("10000"), "BTCUSDC",  Instant.parse("2024-02-18T19:20:00Z"))
        val sellOrderTaker = Order(OrderSide.SELL, BigDecimal("0.5"), BigDecimal("10000"), "BTCUSDC",Instant.parse("2024-02-18T19:30:00Z"))

        // when ... we  match the orders
        val partialBuyOrderStatus = orderBookService.addOrder(buyOrder)
        val sellOrderStatus = orderBookService.addOrder(sellOrderTaker)
        val orderBookDTO = orderBookService.getOrderBookDTO()


        // then ... we assert that the taker's status is correctly updated
        assertTrue(sellOrderStatus.success, "The sell order is fully filled")
        assertTrue(partialBuyOrderStatus.success, "The buy order is partially filled")
        assertTrue(sellOrderStatus.isOrderMatched, "The sell order is matched")

        // and ... 0.5BTC on the buy order to remain
        assertEquals(BigDecimal("0.5"), orderBookDTO.bids.first().quantity)
    }


    @Test
    fun `should accurately reflect the status for partial orders on the taker side`() {
        // given ... a buy and sell order at the same price but with different amounts
        val buyOrder = Order(OrderSide.BUY, BigDecimal("0.5"), BigDecimal("10000"), "BTCUSDC", Instant.parse("2024-02-18T19:20:00Z"))
        val sellOrderTaker = Order(OrderSide.SELL, BigDecimal("1.0"), BigDecimal("10000"), "BTCUSDC", Instant.parse("2024-02-18T19:30:00Z"))

        // when ... we  match the orders
        orderBookService.addOrder(buyOrder)
        val partialSellStatus = orderBookService.addOrder(sellOrderTaker)
        val orderBookDTO = orderBookService.getOrderBookDTO()


        // then ... we assert that the taker's status is correctly updated to reflect a partial fill
        assertTrue(partialSellStatus.success)
        assertTrue(partialSellStatus.partiallyFilled)
        assertEquals(BigDecimal("0.5"), partialSellStatus.remainingQuantity)
        assertTrue(partialSellStatus.isOrderMatched)

        // and ... bids to be empty/exhausted
        assertEquals(0, orderBookDTO.bids.size)
    }

    @Test
    fun `should return current state of the order book when requested`() {
        // given ... an order book exists with valid orders
        val buyOrder = Order(OrderSide.BUY, BigDecimal("0.5"), BigDecimal("49370.0"), "BTCUSDC", Instant.now())
        val sellOrder = Order(OrderSide.SELL, BigDecimal("0.7"), BigDecimal("49475.0"), "BTCUSDC", Instant.now())
        orderBookService.addOrder(buyOrder)
        orderBookService.addOrder(sellOrder)

        // when ... we call the order book to retrieve it
        val orderBookDTO = orderBookService.getOrderBookDTO()

        // then ... it should return the order book with the current orders
        Assertions.assertEquals(1, orderBookDTO.bids.size)
        Assertions.assertEquals(1, orderBookDTO.asks.size)
        Assertions.assertEquals(OrderSide.BUY, enumValueOf<OrderSide>(orderBookDTO.bids.first().side))
        Assertions.assertEquals(OrderSide.SELL, enumValueOf<OrderSide>(orderBookDTO.asks.first().side))
        Assertions.assertEquals(BigDecimal("0.5"), orderBookDTO.bids.first().quantity)
        Assertions.assertEquals(BigDecimal("49370.0"), orderBookDTO.bids.first().price)
        Assertions.assertEquals(BigDecimal("0.7"), orderBookDTO.asks.first().quantity)
        Assertions.assertEquals(BigDecimal("49475.0"), orderBookDTO.asks.first().price)
    }
}
