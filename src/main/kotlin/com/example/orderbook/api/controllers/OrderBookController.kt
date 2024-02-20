package com.example.orderbook.api.controllers

import com.example.orderbook.service.OrderBookService
import com.example.orderbook.util.Serializer.jacksonObjectMapper
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

class OrderBookController(vertx: Vertx, private val orderBookService: OrderBookService) {
    val router: Router = Router.router(vertx)
    private val mapper = jacksonObjectMapper

        init {
        router.route().handler(BodyHandler.create())
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
