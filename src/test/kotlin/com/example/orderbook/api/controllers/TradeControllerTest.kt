package com.example.orderbook.api.controllers

import com.example.orderbook.api.dto.TradeDTO
import com.example.orderbook.model.OrderSide
import com.example.orderbook.service.TradeService
import io.mockk.every
import io.mockk.mockk
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@ExtendWith(VertxExtension::class)
class TradeControllerTest {
    private lateinit var vertx: Vertx
    private lateinit var tradeService: TradeService
    private lateinit var tradeController: TradeController

    @BeforeEach
    fun setUp(vertx: Vertx, vertxTestContext: VertxTestContext) {
        this.vertx = vertx
        this.tradeService = mockk()
        this.tradeController = TradeController(vertx, tradeService)

        every { tradeService.getTradeDTOs() } returns listOf(
            TradeDTO(
                id = UUID.randomUUID().toString(),
                price = BigDecimal("30000"),
                quantity = BigDecimal("1"),
                currencyPair = "BTCUSDC",
                timestamp = Instant.now(),
                takerSide = OrderSide.BUY.toString()
            ),
            TradeDTO(
                id = UUID.randomUUID().toString(),
                price = BigDecimal("35000"),
                quantity = BigDecimal("2"),
                currencyPair = "BTCUSDC",
                timestamp = Instant.now(),
                takerSide = OrderSide.SELL.toString()
            )
        )

        val router = this.tradeController.router
        tradeController.setupRoutes()

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888, vertxTestContext.succeedingThenComplete())
    }

    @Test
    fun `should successfully retrieve recent trades`(vertxTestContext: VertxTestContext) {
        val webClient = WebClient.create(vertx)
        // given ... we have 2 trades in the recent trades as mocked

        // when ... we request the recent trades
        // then ... we can assert we have successfully retrieved them
        webClient.get(8888, "localhost", "/api/recent-trades")
            .send(vertxTestContext.succeeding { response ->
                vertxTestContext.verify {
                    assertEquals(200, response.statusCode())
                    val trades = response.bodyAsJsonArray()
                    assertNotNull(trades)
                    assertEquals(2, trades.size())
                    vertxTestContext.completeNow()
                }
            })
    }

}
