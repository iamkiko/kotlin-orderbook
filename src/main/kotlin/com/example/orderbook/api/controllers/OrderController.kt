package com.example.orderbook.api.controllers

import com.example.orderbook.service.OrderBookService
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class OrderController(vertx: Vertx, private val orderBookService: OrderBookService) {
    private val router: Router = Router.router(vertx)

    init {
        setupRoutes()
    }

    private fun setupRoutes() {
        router.get("/api/orderbook").handler(this::handleGetOrderBook)
    }

    fun handleGetOrderBook(ctx: RoutingContext) {
        val orderBook = orderBookService.getOrderBookDTO()
        ctx.response()
            .putHeader("Content-Type", "application/json")
            .end(Json.encodePrettily(orderBook))
    }
}
