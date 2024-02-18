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

        val trades = tradeService.getTrades()

        // then ... we should be able to correctly retrieve it
        assertEquals(1, trades.size)

        with(trades.first()) {
            assertEquals(price, this.price)
            assertEquals(quantity, this.quantity)
            assertEquals(currencyPair, this.currencyPair)
            assertEquals(takerSide, this.takerSide)
        }
    }

    @Test
    fun `should correctly retrieve a list of trades when multiple have occurred`() {
        // given ... multiple trades have occurred
        tradeService.recordTrade(BigDecimal("39000"), BigDecimal("1"), "BTCUSDC", OrderSide.BUY)
        tradeService.recordTrade(BigDecimal("38000"), BigDecimal("2"), "BTCUSDC", OrderSide.BUY)
        tradeService.recordTrade(BigDecimal("39000"), BigDecimal("1"), "BTCUSDC", OrderSide.SELL)

        // when ... we attempt to retrieve them
        val trades = tradeService.getTrades()

        // then ... we can confirm they are correct
        assertEquals(3, trades.size)
        with(trades[0]) {
            assertEquals(BigDecimal("39000"), this.price)
            assertEquals(BigDecimal("1"), this.quantity)
            assertEquals("BTCUSDC", this.currencyPair)
            assertEquals(OrderSide.BUY, this.takerSide)
        }

        with(trades[1]) {
            assertEquals(BigDecimal("38000"), this.price)
            assertEquals(BigDecimal("2"), this.quantity)
            assertEquals("BTCUSDC", this.currencyPair)
            assertEquals(OrderSide.BUY, this.takerSide)
        }

        with(trades[2]) {
            assertEquals(BigDecimal("39000"), this.price)
            assertEquals(BigDecimal("1"), this.quantity)
            assertEquals("BTCUSDC", this.currencyPair)
            assertEquals(OrderSide.SELL, this.takerSide)
        }
    }

    @Test
    fun `should retrieve trades in the order they were added`() {
        // given ... multiple trades with successive timestamps
        tradeService.recordTrade(BigDecimal("10000"), BigDecimal("1"), "BTCUSDC", OrderSide.BUY)
        Thread.sleep(10)
        tradeService.recordTrade(BigDecimal("20000"), BigDecimal("2"), "BTCUSDC", OrderSide.SELL)

        // when ... we call the service to retrieve them
        val trades = tradeService.getTrades()

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

        val trades = tradeService.getTrades()

        val firstTradeId = trades[0].id
        val secondTradeId = trades[1].id

        // then ... we can confirm that they have unique ids
        assertNotEquals(firstTradeId, secondTradeId)
    }
}
