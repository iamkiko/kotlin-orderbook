package com.example.orderbook.api.controllers

import com.example.orderbook.service.OrderBookService
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class OrderBookController(vertx: Vertx, private val orderBookService: OrderBookService, private val mapper: ObjectMapper) {
    val router: Router = Router.router(vertx)

    init {
        setupRoutes()
    }

    fun setupRoutes() {
        router.get("/api/orderbook").handler(this::handleGetOrderBook)
    }

    fun handleGetOrderBook(ctx: RoutingContext) {
        val orderBook = orderBookService.getOrderBookDTO()
        val json = mapper.writeValueAsString(orderBook)
        ctx.response()
            .putHeader("Content-Type", "application/json")
            .end(json)
    }
}
