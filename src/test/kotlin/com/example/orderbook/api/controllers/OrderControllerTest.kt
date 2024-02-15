package com.example.orderbook.api.controllers

import com.example.orderbook.model.OrderBook
import com.example.orderbook.service.OrderBookService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals


@ExtendWith(VertxExtension::class)
class OrderControllerTest {
    private lateinit var vertx: Vertx
    private lateinit var orderBookService: OrderBookService
    private lateinit var orderController: OrderController


    @BeforeEach
    fun setup(vertx: Vertx, vertxTestContext: VertxTestContext) {
        this.vertx = vertx
        this.orderBookService = OrderBookService(OrderBook())
        val mapper = jacksonObjectMapper();
        this.orderController = OrderController(vertx, orderBookService, mapper)

        val router = Router.router(vertx)
        orderController.setupRoutes()

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888, vertxTestContext.succeedingThenComplete())
    }

    @Test
    fun `should successfully retrieve the order book`(vertxTestContext: VertxTestContext) {
        val webClient = WebClient.create(vertx)
        // given ... an existing orderbook
        // when ... we send a request to the orderbook
        val responseFuture = webClient.get(8085, "localhost", "/api/orderbook")
            .putHeader("content-type", "application/json")
            .send()

        // then ... we can assert that it successfully retrieves it based on the data + response code
        responseFuture.onComplete(vertxTestContext.succeeding { response ->
            vertxTestContext.verify {
                assertEquals(200, response.statusCode())
                val orderBook = response.bodyAsJsonObject()
                assertNotNull(orderBook)
                assertTrue(orderBook.containsKey("asks") || orderBook.containsKey("bids"))
                vertxTestContext.completeNow()
            }
        })
    }


    @Test
    fun `should add order successfully when we submit a limit order`(vertxTestContext: VertxTestContext) {
        val webClient = WebClient.create(vertx)
        // given ... a valid buy order
        val orderDTO = JsonObject()
            .put("side", "BUY")
            .put("quantity", 0.9)
            .put("price", 47777.0)
            .put("currencyPair", "BTCUSDC")
        val request = webClient.post(8085, "localhost", "/api/orders/limit")
            .putHeader("content-type", "application/json")

        // when ... sent to our controller
        val responseFuture = request.sendJsonObject(orderDTO)

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
        val webClient = WebClient.create(vertx)
        // given ... an invalid sell order
        val invalidOrderDTO = JsonObject()
            .put("side", "SIDE")
            .put("quantity", 29.9)
            .put("price", 0.0)
            .put("currencyPair", "BTCUSDC")
        val request = webClient.post(8085, "localhost", "/api/orders/limit")
            .putHeader("content-type", "application/json")

        // when ... sent to our controller
        val responseFuture = request.sendJsonObject(invalidOrderDTO)

        // then ... our controller responds with a bad request (400)
        responseFuture.onComplete(vertxTestContext.succeeding { response ->
            vertxTestContext.verify {
                assertEquals(400, response.statusCode())
                assertTrue(response.bodyAsJsonObject().containsKey("error"))
                vertxTestContext.completeNow()
            }
        })
    }

    // TODO(): Is this necessary?
    @Test
    fun `should fail to add order when we send incomplete data for limit orders, returning a bad request (400)`(
        vertxTestContext: VertxTestContext
    ) {
        val webClient = WebClient.create(vertx)
        // given ... an incomplete sell order
        val invalidOrderDTO = JsonObject()
            .put("side", "SIDE")
            .put("quantity", 29.9)
            .put("currencyPair", "BTCUSDC")
        val request = webClient.post(8085, "localhost", "/api/orders/limit")
            .putHeader("content-type", "application/json")

        // when ... sent to our controller
        val responseFuture = request.sendJsonObject(invalidOrderDTO)

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
    fun `should not return data sent to non-existent endpoints`(vertxTestContext: VertxTestContext) {
        val webClient = WebClient.create(vertx)
        // given ... a request
        // when ... a request is sent to a non-existent endpoint
        val responseFuture = webClient.get(8085, "localhost", "/api/thisdoesnotexist")
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
