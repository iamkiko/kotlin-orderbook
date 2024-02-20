package com.example.orderbook.api.controllers

import io.vertx.core.Vertx
import com.example.orderbook.service.TradeService
import com.example.orderbook.util.Serializer.jacksonObjectMapper
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

class TradeController(vertx: Vertx, private val tradeService: TradeService) {
    val router: Router = Router.router(vertx)
    private val mapper = jacksonObjectMapper

    init {
        setupRoutes()
    }

    fun setupRoutes() {
        router.route().handler(BodyHandler.create())
        router.get("/api/recent-trades").handler(this::handleGetTrades)
    }

    fun handleGetTrades(ctx: RoutingContext) {
        try {
            val trades = tradeService.getTradeDTOs()
            val json = mapper.writeValueAsString(trades)
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(json)
        } catch (e: Exception) {
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(Json.encodePrettily(mapOf("error" to "An error occurred while fetching trades")))
        }
    }
}
