package com.example.orderbook.api.controllers

import com.example.orderbook.model.OrderBook
import com.example.orderbook.service.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(VertxExtension::class)
class OrderBookControllerTest {
    private lateinit var vertx: Vertx
    private lateinit var orderBookService: OrderBookService
    private lateinit var tradeService: TradeService
    private lateinit var orderBookController: OrderBookController

    @BeforeEach
    fun setUp(vertx: Vertx, vertxTestContext: VertxTestContext) {
        this.vertx = vertx
        this.tradeService = TradeService()
        val orderBook = OrderBook()
        val orderValidator = OrderValidator()
        val orderManager = OrderManager(orderBook)
        val matchingEngine = MatchingEngine(tradeService, orderBook)
        this.orderBookService = OrderBookService(orderBook, tradeService, orderValidator, orderManager, matchingEngine)
        val mapper = jacksonObjectMapper();
        this.orderBookController = OrderBookController(vertx, orderBookService, mapper)
        val router = this.orderBookController.router

        orderBookController.setupRoutes()

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888, vertxTestContext.succeedingThenComplete())
    }

    @Test
    fun `should successfully retrieve the order book`(vertxTestContext: VertxTestContext) {
        val webClient = WebClient.create(vertx)
        // given ... an existing orderbook
        // when ... we send a request to the orderbook
        val responseFuture = webClient.get(8888, "localhost", "/api/orderbook")
            .putHeader("content-type", "application/json")
            .send()

        // then ... we can assert that it successfully retrieves it based on the data + response code
        responseFuture.onComplete(vertxTestContext.succeeding { response ->
            vertxTestContext.verify {
                assertEquals(200, response.statusCode())
                val orderBook = response.bodyAsJsonObject()
                Assertions.assertNotNull(orderBook)
                Assertions.assertTrue(orderBook.containsKey("asks") || orderBook.containsKey("bids"))
                vertxTestContext.completeNow()
            }
        })
    }

    @Test
    fun `should not return data sent to non-existent endpoints`(vertxTestContext: VertxTestContext) {
        val webClient = WebClient.create(vertx)
        // given ... a request
        // when ... a request is sent to a non-existent endpoint
        val responseFuture = webClient.get(8888, "localhost", "/api/thisdoesnotexist")
            .putHeader("content-type", "application/json")
            .send()

        // then ... we return the correct status code
        responseFuture.onComplete(vertxTestContext.succeeding { response ->
            vertxTestContext.verify {
                assertEquals(404, response.statusCode())
                vertxTestContext.completeNow()
            }
        })
    }
}
