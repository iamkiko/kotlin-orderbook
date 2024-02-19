//package com.example.orderbook.service
//
//import com.example.orderbook.model.Order
//import com.example.orderbook.model.OrderBook
//import com.example.orderbook.model.OrderSide
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.TestInstance
//import org.junit.jupiter.params.ParameterizedTest
//import org.junit.jupiter.params.provider.Arguments
//import org.junit.jupiter.params.provider.MethodSource
//import java.math.BigDecimal
//import java.time.Instant
//import java.util.*
//
//
//class OrderBookServiceTest {
//    private lateinit var orderBookService: OrderBookService
//    private lateinit var orderBook: OrderBook
//    private lateinit var tradeService: TradeService
//
//    @BeforeEach
//    fun setUp() {
//        orderBook = OrderBook(
//            TreeMap<BigDecimal, TreeMap<Instant, Order>>(reverseOrder()),
//            TreeMap<BigDecimal, TreeMap<Instant, Order>>(),
//            Instant.parse("2024-02-13T00:00:00.000Z"),
//            0
//        )
//        tradeService = TradeService()
//        orderBookService = OrderBookService(orderBook, tradeService)
//    }
//
//    @Test
//    fun `should return current state of the order book when requested`() {
//        // given ... an order book exists with valid orders
//        val buyOrder = Order(OrderSide.BUY, BigDecimal("0.5"), BigDecimal("49370.0"), "BTCUSDC", Instant.now())
//        val sellOrder = Order(OrderSide.SELL, BigDecimal("0.7"), BigDecimal("49475.0"), "BTCUSDC", Instant.now())
//        orderBookService.addOrder(buyOrder)
//        orderBookService.addOrder(sellOrder)
//
//        // when ... we call the order book to retrieve it
//        orderBookService.matchOrders()
//        val orderBookSummary = orderBookService.getOrderBookDTO()
//
//
//        // then ... it should return the order book with the current orders
//        assertEquals(1, orderBookSummary.bids.size)
//        assertEquals(1, orderBookSummary.asks.size)
//        assertEquals(OrderSide.BUY, enumValueOf<OrderSide>(orderBookSummary.bids.first().side))
//        assertEquals(OrderSide.SELL, enumValueOf<OrderSide>(orderBookSummary.asks.first().side))
//        assertEquals(BigDecimal("0.5"), orderBookSummary.bids.first().quantity)
//        assertEquals(BigDecimal("49370.0"), orderBookSummary.bids.first().price)
//        assertEquals(BigDecimal("0.7"), orderBookSummary.asks.first().quantity)
//        assertEquals(BigDecimal("49475.0"), orderBookSummary.asks.first().price)
//    }
//
//    @Test
//    fun `should return an empty order book when there are no bids or asks and the service has been initialized`() {
//        // given ... an order book is yet to be initialized
//        // when ... we request the order book state
//        orderBookService.matchOrders()
//
//        // then ...  it should return an order book with empty bids and asks
//        assertTrue(orderBook.bids.isEmpty())
//        assertTrue(orderBook.asks.isEmpty())
//    }
//
//    @Test
//    fun `should match orders and remove them from orderbook when prices match`() {
//        // given ... we have buy and sell orders at the same price and amount
//        val matchingBuyOrder = Order(OrderSide.BUY, BigDecimal("1.0"), BigDecimal("44900.0"), "BTCUSDC")
//        val matchingSellOrder = Order(OrderSide.SELL, BigDecimal("1.0"), BigDecimal("44900.0"), "BTCUSDC")
//        orderBookService.addOrder(matchingBuyOrder)
//        orderBookService.addOrder(matchingSellOrder)
//
//        // when ... we attempt to match the orders
//        orderBookService.matchOrders()
//
//        // then ... the order book is empty as we've matched all orders
//        assertTrue(orderBook.bids.isEmpty())
//        assertTrue(orderBook.asks.isEmpty())
//    }
//
//    @Test
//    fun `should not remove orders from the order book when they do not match`() {
//        // given ... we have buy and sell orders and different amounts
//        val buyOrder = Order(OrderSide.BUY, BigDecimal("0.2"), BigDecimal("37999.0"), "BTCUSDC")
//        val sellOrder = Order(OrderSide.SELL, BigDecimal("0.2"), BigDecimal("43999.0"), "BTCUSDC")
//        orderBookService.addOrder(buyOrder)
//        orderBookService.addOrder(sellOrder)
//
//        // when ... we attempt to match the orders
//        orderBookService.matchOrders()
//        val orderBookSummary = orderBookService.getOrderBookDTO()
//
//        // then ... the order book remains unchanged as the buys and sells aren't matched
//        assertFalse(orderBook.bids.isEmpty())
//        assertFalse(orderBook.asks.isEmpty())
//        assertEquals(BigDecimal("37999.0"), orderBookSummary.bids.first().price)
//        assertEquals(BigDecimal("43999.0"), orderBookSummary.asks.first().price)
//    }
//
//    @Test
//    fun `should partially match orders and update the remaining quantity correctly`() {
//        // given ... a buy and sell order at the same price but with different amounts
//        val buyOrder = Order(OrderSide.BUY, BigDecimal("1.0"), BigDecimal("40000.0"), "BTCUSDC")
//        val sellOrder = Order(OrderSide.SELL, BigDecimal("0.5"), BigDecimal("40000.0"), "BTCUSDC")
//
//        orderBookService.addOrder(buyOrder)
//        orderBookService.addOrder(sellOrder)
//
//        // when ... we  match the orders
//        orderBookService.matchOrders()
//        val orderBookSummary = orderBookService.getOrderBookDTO()
//
//        // then ... we can confirm that the sell order is no longer on the order book but the buy order has only been partially filled
//        assertTrue(orderBook.asks.isEmpty())
//        assertEquals(BigDecimal("0.5"), orderBookSummary.bids.first().quantity)
//    }
//
//    @Test
//    fun `should add multiple orders and ensure correct sorting`() {
//        // given ... buy and sell orders added in a non-sorted order
//        val middlePriceBuyOrder = Order(OrderSide.BUY, BigDecimal("0.5"), BigDecimal("29000.0"), "BTCUSDC")
//        val highestPriceBuyOrder = Order(OrderSide.BUY, BigDecimal("1.2"), BigDecimal("35000.0"), "BTCUSDC")
//        val lowestPriceBuyOrder = Order(OrderSide.BUY, BigDecimal("4.0"), BigDecimal("22000.0"), "BTCUSDC")
//        val lowestPriceSellOrder = Order(OrderSide.SELL, BigDecimal("3.0"), BigDecimal("36000.0"), "BTCUSDC")
//        val middlePriceSellOrder = Order(OrderSide.SELL, BigDecimal("1.1"), BigDecimal("41000.0"), "BTCUSDC")
//        val highestPriceSellOrder = Order(OrderSide.SELL, BigDecimal("1.0"), BigDecimal("48000.0"), "BTCUSDC")
//
//        // when ... we add the orders to the orderbook
//        orderBookService.addOrder(middlePriceBuyOrder)
//        orderBookService.addOrder(highestPriceBuyOrder)
//        orderBookService.addOrder(lowestPriceBuyOrder)
//        orderBookService.addOrder(lowestPriceSellOrder)
//        orderBookService.addOrder(middlePriceSellOrder)
//        orderBookService.addOrder(highestPriceSellOrder)
//
//        // then ... we can verify the entire order of the buy and sell queues
//        val buyOrderPrices = orderBook.bids.keys.toList()
//        val sellOrderPrices = orderBook.asks.keys.toList()
//
//        val expectedBuyOrderPrices =
//            listOf(highestPriceBuyOrder.price, middlePriceBuyOrder.price, lowestPriceBuyOrder.price)
//        val expectedSellOrderPrices =
//            listOf(lowestPriceSellOrder.price, middlePriceSellOrder.price, highestPriceSellOrder.price)
//        assertEquals(expectedBuyOrderPrices, buyOrderPrices)
//        assertEquals(expectedSellOrderPrices, sellOrderPrices)
//        // and ... we can confirm that the highest buy order and the lowest sell order are the first orders around the spread
//        assertEquals(highestPriceBuyOrder.price, orderBook.bids.firstKey())
//        assertEquals(lowestPriceSellOrder.price, orderBook.asks.firstKey())
//    }
//
//    @Test
//    fun `should fill orders fully and partially up to a certain price based on price priority`() {
//        // given ... buy orders which are >= current sell orders
//        val sellOrderAtCurrentPrice = Order(OrderSide.SELL, BigDecimal("1.0"), BigDecimal("40000.0"), "BTCUSDC")
//        val sellOrderAtLowestPrice = Order(OrderSide.SELL, BigDecimal("1.0"), BigDecimal("39500.0"), "BTCUSDC")
//        val buyOrderAtCurrentPrice = Order(OrderSide.BUY, BigDecimal("1.5"), BigDecimal("40000.0"), "BTCUSDC")
//
//        // when ... we add the orders to the order book and match them
//        orderBookService.addOrder(sellOrderAtCurrentPrice)
//        orderBookService.addOrder(sellOrderAtLowestPrice)
//        orderBookService.addOrder(buyOrderAtCurrentPrice)
//        orderBookService.matchOrders()
//        val asks = orderBookService.getOrderBookDTO().asks
//        val bids = orderBookService.getOrderBookDTO().bids
//
//        // then ... we should expect that 1 sell order of 0.5BTC @ 40000 remains on the ask side
//        assertEquals(1, asks.size)
//        assertEquals(BigDecimal("0.5"), asks.first().quantity)
//        assertEquals(BigDecimal("40000.0"), asks.first().price)
//
//        // and ...no further buy orders exist
//        assertEquals(0, bids.size)
//    }
//
//    @Test
//    fun `should execute trades at best available price`() {
//        // given ... matching buy and sell orders with different prices
//        val highPriceBuyOrder = Order(OrderSide.BUY, BigDecimal("1.0"), BigDecimal("45000.0"), "BTCUSDC")
//        val lowPriceSellOrder = Order(OrderSide.SELL, BigDecimal("1.0"), BigDecimal("44000.0"), "BTCUSDC")
//
//        // when ... we add orders to the order book and match them
//        orderBookService.addOrder(highPriceBuyOrder)
//        orderBookService.addOrder(lowPriceSellOrder)
//
//        val tradesAfterMatching = tradeService.getTradeDTOs()
//        val lastTrade = tradesAfterMatching.last()
//
//        // then ... the buy order is executed at the lowest available price
//        assertNotNull(lastTrade)
//        assertEquals(BigDecimal("44000.0"), lastTrade.price)
//    }
//
//}
