package com.example.orderbook.api.controllers

import com.example.orderbook.model.OrderBook
import com.example.orderbook.service.*
import io.github.cdimascio.dotenv.dotenv
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.PubSecKeyOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.JWTAuthHandler
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import kotlin.test.assertEquals

@ExtendWith(VertxExtension::class)
class OrderControllerTest {
    private val dotenv = dotenv()
    private lateinit var vertx: Vertx
    private lateinit var jwtAuth: JWTAuth
    private lateinit var webClient: WebClient
    private lateinit var orderBookService: OrderBookService
    private lateinit var tradeService: TradeService
    private lateinit var orderController: OrderController

    @BeforeEach
    fun setUp(vertx: Vertx, vertxTestContext: VertxTestContext) {
        this.vertx = vertx
        this.webClient = WebClient.create(vertx)
        this.tradeService = TradeService()
        val orderBook = OrderBook()
        val orderValidator = OrderValidator()
        val orderManager = OrderManager(orderBook)
        val matchingEngine = MatchingEngine(tradeService, orderBook)
        this.orderBookService = OrderBookService(orderBook, tradeService, orderValidator, orderManager, matchingEngine)

        this.jwtAuth = JWTAuth.create(vertx, JWTAuthOptions().apply {
            addPubSecKey(PubSecKeyOptions()
                .setAlgorithm("HS256")
                .setBuffer(dotenv["SYMMETRIC_KEY"]))
        })

        val jwtAuthHandler = JWTAuthHandler.create(jwtAuth)
        this.orderController = OrderController(vertx, orderBookService, jwtAuthHandler)

        val router = this.orderController.router
        router.route().handler(BodyHandler.create())
        orderController.setupRoutes()

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888, vertxTestContext.succeedingThenComplete())
    }

    // TODO() test user
    private fun generateTestToken(): String {
        return jwtAuth.generateToken(JsonObject().put("sub", "testUser"))
    }

    @Test
    fun `should add order successfully when we submit a limit order`(vertxTestContext: VertxTestContext) {
        val token = generateTestToken()
        // given ... a valid buy order
        val orderDTOJson = JsonObject()
            .put("side", "BUY")
            .put("quantity", 0.9000)
            .put("price", 47777.0000)
            .put("currencyPair", "BTCUSDC")
        val request = webClient.post(8888, "localhost", "/api/orders/limit")
            .putHeader("Content-Type", "application/json")
            .putHeader("Authorization", "Bearer $token")

        // when ... sent to our controller
        val responseFuture = request.sendJsonObject(orderDTOJson)

        // then ... we can assert that it was successful based on API response code (created)
        responseFuture.onComplete(vertxTestContext.succeeding { response ->
            vertxTestContext.verify {
                assertEquals(201, response.statusCode())
                vertxTestContext.completeNow()
            }
        })
    }

    @Test
    fun `should fail to add order when we send invalid data for a limit order, returning a bad request (400)`(
        vertxTestContext: VertxTestContext
    ) {
        val token = generateTestToken()
        // given ... an invalid sell order
        val invalidOrderDTOJson = JsonObject()
            .put("side", "SIDE")
            .put("quantity", 29.9)
            .put("price", 0.0)
            .put("currencyPair", "BTCUSDC")
        val request = webClient.post(8888, "localhost", "/api/orders/limit")
            .putHeader("Content-Type", "application/json")
            .putHeader("Authorization", "Bearer $token")

        // when ... sent to our controller
        val responseFuture = request.sendJsonObject(invalidOrderDTOJson)

        // then ... our controller responds with a bad request (400)
        responseFuture.onComplete(vertxTestContext.succeeding { response ->
            vertxTestContext.verify {
                assertEquals(400, response.statusCode())
                assertTrue(response.bodyAsJsonObject().containsKey("error"))
                vertxTestContext.completeNow()
            }
        })
    }

    @Test
    fun `should fail to add order when we a field is missing for limit orders, returning a bad request (400)`(
        vertxTestContext: VertxTestContext
    ) {
        val token = generateTestToken()
        // given ... an incomplete sell order
        val invalidOrderDTOJson = JsonObject()
            .put("side", "invalid_side")
            .put("quantity", BigDecimal("29.9"))
            .put("currencyPair", "invalid_currency_pair")
        val request = webClient.post(8888, "localhost", "/api/orders/limit")
            .putHeader("Content-Type", "application/json")
            .putHeader("Authorization", "Bearer $token")

        // when ... sent to our controller
        val responseFuture = request.sendJsonObject(invalidOrderDTOJson)

        // then ... our controller responds with a bad request (400)
        responseFuture.onComplete(vertxTestContext.succeeding { response ->
            vertxTestContext.verify {
                assertEquals(400, response.statusCode())
                assertTrue(response.bodyAsJsonObject().containsKey("error"))
                vertxTestContext.completeNow()
            }
        })
    }

    @Test
    fun `should reject unauthorized request to add a limit order`(vertxTestContext: VertxTestContext) {
        // given ... a valid buy order
        val orderDTOJson = JsonObject()
            .put("side", "BUY")
            .put("quantity", 0.9000)
            .put("price", 47777.0000)
            .put("currencyPair", "BTCUSDC")

        // when ... sent to our controller without a jwt token
        val request = webClient.post(8888, "localhost", "/api/orders/limit")
            .putHeader("Content-Type", "application/json")
        val responseFuture = request.sendJsonObject(orderDTOJson)

        // then ... the endpoint correctly returns a 401 unauthorized
        responseFuture.onComplete(vertxTestContext.succeeding { response ->
            vertxTestContext.verify {
                assertEquals(401, response.statusCode())
                vertxTestContext.completeNow()
            }
        })
    }

}
