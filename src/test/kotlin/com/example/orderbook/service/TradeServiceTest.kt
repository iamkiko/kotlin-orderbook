import com.example.orderbook.model.OrderSide
import com.example.orderbook.service.TradeService
import com.example.orderbook.repository.TradeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TradeServiceTest {

    private lateinit var tradeService: TradeService

    @BeforeEach
    fun setUp() {
        TradeRepository.clearTrades() // start off with clean state
        tradeService = TradeService()
    }

    @Test
    fun `should record a trade when a trade occurs successfully`() {
        val quantity = BigDecimal("2")
        val price = BigDecimal("27000")
        val currencyPair = "BTCUSDC"
        val takerSide = OrderSide.BUY
        // given ... a successful trade
        // when ... we attempt to record and store it
        tradeService.recordTrade(price, quantity, currencyPair, takerSide)

        val trades = tradeService.getTradeDTOs()

        // then ... we should be able to correctly retrieve it
        assertEquals(1, trades.size)

        val trade = trades.first()
        assertEquals(price, trade.price)
        assertEquals(quantity, trade.quantity)
        assertEquals(currencyPair, trade.currencyPair)
        assertEquals(takerSide.toString(), trade.takerSide)
    }

    @Test
    fun `should correctly retrieve a list of trades when multiple have occurred`() {
        // given ... multiple trades have occurred
        tradeService.recordTrade(BigDecimal("39000"), BigDecimal("1"), "BTCUSDC", OrderSide.BUY)
        tradeService.recordTrade(BigDecimal("38000"), BigDecimal("2"), "BTCUSDC", OrderSide.BUY)
        tradeService.recordTrade(BigDecimal("22000"), BigDecimal("3"), "BTCUSDC", OrderSide.SELL)

        // when ... we attempt to retrieve them
        val trades = tradeService.getTradeDTOs()

        // then ... we can confirm they are correct
        assertEquals(3, trades.size)
        assertEquals(BigDecimal("39000"), trades[0].price)
        assertEquals(BigDecimal("1"),  trades[0].quantity)
        assertEquals("BUY",  trades[0].takerSide)

        assertEquals(BigDecimal("38000"), trades[1].price)
        assertEquals(BigDecimal("2"), trades[1].quantity)

        assertEquals(BigDecimal("22000"), trades[2].price)
        assertEquals(BigDecimal("3"), trades[2].quantity)
        assertEquals("SELL", trades[2].takerSide)
    }

    @Test
    fun `should retrieve trades in the order they were added`() {
        // given ... multiple trades with successive timestamps
        tradeService.recordTrade(BigDecimal("10000"), BigDecimal("1"), "BTCUSDC", OrderSide.BUY)
        Thread.sleep(100)
        tradeService.recordTrade(BigDecimal("20000"), BigDecimal("2"), "BTCUSDC", OrderSide.SELL)

        // when ... we call the service to retrieve them
        val trades = tradeService.getTradeDTOs()

        val firstTradeTimestamp = trades[0].timestamp
        val secondTradeTimestamp = trades[1].timestamp

        // then ... we can confirm that the order of the trades is correctly captured
        assertTrue(firstTradeTimestamp.isBefore(secondTradeTimestamp))
    }

    @Test
    fun `should ensure each trade recorded has a unique id`() {
        // given ... multiple trades
        tradeService.recordTrade(BigDecimal("10000"), BigDecimal("1"), "BTCUSDC", OrderSide.BUY)
        tradeService.recordTrade(BigDecimal("20000"), BigDecimal("2"), "BTCUSDC", OrderSide.SELL)

        // when ... we call the service to retrieve them

        val trades = tradeService.getTradeDTOs()

        val firstTradeId = trades[0].id
        val secondTradeId = trades[1].id

        // then ... we can confirm that they have unique ids
        assertNotEquals(firstTradeId, secondTradeId)
    }

    @Test
    fun `should correctly determine taker side based on order initiation`() {
        // given ... 2 fulfilled orders
        val sellPrice = BigDecimal("40000")
        val buyPrice = BigDecimal("41000")
        val quantity = BigDecimal("1")
        val currencyPair = "BTCUSDC"

        // when ... we record them
        tradeService.recordTrade(sellPrice, quantity, currencyPair, OrderSide.SELL)
        tradeService.recordTrade(buyPrice, quantity, currencyPair, OrderSide.BUY)

        val trades = tradeService.getTradeDTOs()
        assertEquals(2, trades.size)
        // then ... we can determine that the taker side was the order that accepted the existing order
        assertEquals(OrderSide.BUY.toString(), trades.last().takerSide)
    }

}
