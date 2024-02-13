package com.example.orderbook

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
        router.get("/api/orderbook").handler { context ->
            // TODO: Retrieve and respond with the order book data
            context.response()
                .putHeader("content-type", "application/json")
                .end("Order book data goes here")
        }

        // Limit order
        router.post("/api/orders/limit").handler { context ->
            // TODO: Handle the submission of a limit order
            context.response()
                .putHeader("content-type", "application/json")
                .end("Response for a submitted limit order goes here")
        }

        // Recent trades
        router.get("/api/trades/recent").handler { context ->
            // TODO: Retrieve and respond with recent trades data
            context.response()
                .putHeader("content-type", "application/json")
                .end("Recent trades data goes here")
        }

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
