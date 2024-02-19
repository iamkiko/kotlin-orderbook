package com.example.orderbook.service
import com.example.orderbook.api.dto.TradeDTO
import com.example.orderbook.model.Order
import com.example.orderbook.model.OrderBook
import com.example.orderbook.model.OrderSide
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatchingEngineTest {

    private lateinit var tradeService: TradeService
    private lateinit var orderBook: OrderBook
    private lateinit var matchingEngine: MatchingEngine
    private lateinit var orderManager: OrderManager
    private val capturedTrades = mutableListOf<TradeDTO>()


    @BeforeEach
    fun setUp() {
        tradeService = mockk(relaxed = true)
        // this is simply to assert on the best price test
        every {
            tradeService.recordTrade(any(), any(), any(), any())
        } answers {
            val tradeDTO = TradeDTO(
                id = UUID.randomUUID().toString(),
                price = firstArg<BigDecimal>(),
                quantity = secondArg<BigDecimal>(),
                currencyPair = thirdArg<String>(),
                timestamp = Instant.now(),
                takerSide = arg<OrderSide>(3).toString()
            )
            capturedTrades.add(tradeDTO)
        }

        orderBook = OrderBook(
            bids = TreeMap<BigDecimal, TreeMap<Instant, Order>>(),
            asks = TreeMap<BigDecimal, TreeMap<Instant, Order>>(),
            lastUpdated = Instant.now(),
            tradeSequenceNumber = 0L
        )
        matchingEngine = MatchingEngine(tradeService, orderBook)
        orderManager = OrderManager(orderBook)
    }

    @Test
    fun `should completely match buy and sell orders at the same price`() {
        // given ... buy and sell orders at the same price
        val buyOrder = Order(OrderSide.BUY, BigDecimal("1.0"), BigDecimal("10000"), "BTCUSDC", Instant.now())
        val sellOrder = Order(OrderSide.SELL, BigDecimal("1.0"), BigDecimal("10000"), "BTCUSDC", Instant.now())
        orderManager.addOrder(buyOrder)
        orderManager.addOrder(sellOrder)

        // when ... matching orders
        val matchResult = matchingEngine.matchOrders(orderBook)

        // then ... we expect orders to be fully matched
        assertTrue(matchResult.isOrderMatched)
        assertEquals(BigDecimal("1.0"), matchResult.totalMatchedQuantity)
        assertEquals(buyOrder.price, matchResult.fulfilledPrice)
        verify { tradeService.recordTrade(matchResult.fulfilledPrice, matchResult.totalMatchedQuantity, buyOrder.currencyPair, any()) }
    }

    @Test
    fun `should partially match orders when quantities differ`() {
        // given ... a buy order with higher quantity than the sell order
        val buyOrder = Order(OrderSide.BUY, BigDecimal("2.5"), BigDecimal("10000"), "BTCUSDC", Instant.now())
        val sellOrder = Order(OrderSide.SELL, BigDecimal("1.5"), BigDecimal("10000"), "BTCUSDC", Instant.now())
        orderManager.addOrder(buyOrder)
        orderManager.addOrder(sellOrder)

        // when ... matching orders
        val matchResult = matchingEngine.matchOrders(orderBook)
        val firstBidOrder = orderBook.bids.values.first().values.first()

        // then ... we expect orders to be partially matched
        assertTrue(matchResult.isOrderMatched)
        assertEquals(BigDecimal("1.5"), matchResult.totalMatchedQuantity)
        assertTrue(orderBook.asks.isEmpty())

        // and ... outstanding bid in book to be updated accordingly
        assertEquals(BigDecimal("1.0"), firstBidOrder.quantity)
    }

    @Test
    fun `should not match orders when there is a price discrepancy (spread)`() {
        // given ... buy and sell orders with no overlapping prices
        val buyOrder = Order(OrderSide.BUY, BigDecimal("1.3"), BigDecimal("9000"), "BTCUSDC", Instant.now())
        val sellOrder = Order(OrderSide.SELL, BigDecimal("1.4"), BigDecimal("10000"), "BTCUSDC", Instant.now())
        orderManager.addOrder(buyOrder)
        orderManager.addOrder(sellOrder)

        // when ... matching orders
        val matchResult = matchingEngine.matchOrders(orderBook)
        val firstBidOrder = orderBook.bids.values.first().values.first()
        val firstAskOrder = orderBook.asks.values.first().values.first()

        // then ... it should not match
        assertFalse(matchResult.isOrderMatched)
        assertEquals(BigDecimal.ZERO, matchResult.totalMatchedQuantity)
        assertEquals(BigDecimal("1.3"), firstBidOrder.quantity)
        assertEquals(BigDecimal("1.4"), firstAskOrder.quantity)
    }

    @Test
    fun `order book should update correctly after orders are matched`() {
        // given ... buy and sell orders at the same price (2 total)
        val buyOrder = Order(OrderSide.BUY, BigDecimal("1.0"), BigDecimal("10000"), "BTCUSDC", Instant.now())
        val sellOrder = Order(OrderSide.SELL, BigDecimal("1.0"), BigDecimal("10000"), "BTCUSDC", Instant.now())
        orderManager.addOrder(buyOrder)
        orderManager.addOrder(sellOrder)

        // when ... matching orders
        matchingEngine.matchOrders(orderBook)

        // then ... we can verify the matched orders are removed and the order book is empty
        assertTrue(orderBook.bids.isEmpty() || orderBook.asks.isEmpty())
        assertFalse(orderBook.bids.containsKey(buyOrder.price))
        assertFalse(orderBook.asks.containsKey(sellOrder.price))
    }

    @Test
    fun `should fill orders fully and partially up to a certain price based on price priority`() {
        // given ... buy orders which are >= current sell orders
        val sellOrderAtCurrentPrice = Order(OrderSide.SELL, BigDecimal("1.0"), BigDecimal("40000.0"), "BTCUSDC", Instant.now())
        val sellOrderAtLowestPrice = Order(OrderSide.SELL, BigDecimal("1.0"), BigDecimal("39500.0"), "BTCUSDC", Instant.now())
        val buyOrderAtCurrentPrice = Order(OrderSide.BUY, BigDecimal("1.5"), BigDecimal("40000.0"), "BTCUSDC", Instant.now())

        // when ... we add the orders to the order book and match them
        orderManager.addOrder(sellOrderAtCurrentPrice)
        orderManager.addOrder(sellOrderAtLowestPrice)
        orderManager.addOrder(buyOrderAtCurrentPrice)
        matchingEngine.matchOrders(orderBook)

        val orderBookSnapshot = orderManager.getOrderBookSnapshot(orderBook)

        // then ... we should expect that 1 sell order of 0.5BTC @ 40000 remains on the ask side
        assertEquals(1, orderBookSnapshot.asks.size)
        assertEquals(BigDecimal("0.5"), orderBookSnapshot.asks.first().quantity)
        assertEquals(BigDecimal("40000.0"), orderBookSnapshot.asks.first().price)

        // and ...no further buy orders exist
        assertTrue(orderBookSnapshot.bids.isEmpty())
    }

    @Test
    fun `should execute trades at best available price`() {
        // given ... matching buy and sell orders with different prices
        val highPriceBuyOrder = Order(OrderSide.BUY, BigDecimal("1.0"), BigDecimal("45000.0"), "BTCUSDC", Instant.now())
        val lowPriceSellOrder = Order(OrderSide.SELL, BigDecimal("1.0"), BigDecimal("44000.0"), "BTCUSDC", Instant.now())

    // when ... we add orders to the order book and match them
        orderManager.addOrder(highPriceBuyOrder)
        orderManager.addOrder(lowPriceSellOrder)
        matchingEngine.matchOrders(orderBook)

        // then ... the buy order is executed at the lowest available price
        assertFalse(capturedTrades.isEmpty())
        val lastTrade = capturedTrades.last()
        assertEquals(BigDecimal("44000.0"), lastTrade.price)
    }

}
