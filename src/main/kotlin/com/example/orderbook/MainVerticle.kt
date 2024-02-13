package com.example.orderbook

import com.example.orderbook.api.controllers.OrderController
import com.example.orderbook.model.OrderBook
import com.example.orderbook.service.OrderBookService
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.ext.web.Router


class MainVerticle : AbstractVerticle() {

    override fun start(promise: Promise<Void>) {
        val router = Router.router(vertx)

        router.get("/healthcheck").handler { routingContext ->
            val response = routingContext.response()
            response.putHeader("content-type", "application/json")
                .end("""{"status": "up"}""")
        }

        // Orderbook
        val orderBookService = OrderBookService(OrderBook())
        val orderController = OrderController(vertx, orderBookService)
        router.get("/api/orderbook").handler(orderController::handleGetOrderBook)


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
    }

}
