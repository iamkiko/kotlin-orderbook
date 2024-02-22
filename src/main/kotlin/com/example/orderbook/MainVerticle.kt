package com.example.orderbook

import com.example.orderbook.api.controllers.AuthController
import com.example.orderbook.api.controllers.OrderBookController
import com.example.orderbook.api.controllers.OrderController
import com.example.orderbook.api.controllers.TradeController
import com.example.orderbook.model.OrderBook
import com.example.orderbook.service.*
import io.github.cdimascio.dotenv.dotenv
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.ext.auth.PubSecKeyOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.JWTAuthHandler


class MainVerticle : AbstractVerticle() {
    private val dotenv = dotenv()

    override fun start(promise: Promise<Void>) {
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create()) // to parse incoming requests

        val symmetricKey = dotenv["SYMMETRIC_KEY"]
        val jwtAuth = JWTAuth.create(vertx, JWTAuthOptions().apply {
            addPubSecKey(
                PubSecKeyOptions()
                    .setAlgorithm("HS256")
                    .setBuffer(symmetricKey)
            )
        })

        val jwtAuthHandler = JWTAuthHandler.create(jwtAuth)

        val orderBook = OrderBook()
        val tradeService = TradeService()
        val orderValidator = OrderValidator()
        val orderManager = OrderManager(orderBook)
        val matchingEngine = MatchingEngine(tradeService, orderBook)

        val orderBookService = OrderBookService(orderBook, tradeService, orderValidator, orderManager, matchingEngine)

        val authController = AuthController(vertx, jwtAuth)
        val tradeController = TradeController(vertx, tradeService)
        val orderBookController = OrderBookController(vertx, orderBookService)
        val orderController = OrderController(vertx, orderBookService, jwtAuthHandler)

        router.get("/api/orderbook").handler(orderBookController::handleGetOrderBook)
        router.get("/api/recent-trades").handler(tradeController::handleGetTrades)
        router.post("/api/orders/limit").handler(jwtAuthHandler).handler(orderController::handleAddLimitOrder)
        router.post("/api/login").handler(authController::handleLogin)

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8085)
            .onSuccess { server ->
                println("HTTP server started on port ${server.actualPort()}")
                promise.complete()
            }
            .onFailure { throwable ->
                println("Failed to start HTTP server: ${throwable.message}")
                promise.fail(throwable)
            }


        router.get("/healthcheck").handler { routingContext ->
            val response = routingContext.response()
            response.putHeader("Content-Type", "application/json")
                .end("""{"status": "up"}""")
        }
    }
}
