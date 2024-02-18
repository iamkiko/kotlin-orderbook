package com.example.orderbook.api.controllers

import io.vertx.core.Vertx
import com.example.orderbook.service.TradeService
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class TradeController(vertx: Vertx, private val tradeService: TradeService, private val mapper: ObjectMapper) {
    private val router: Router = Router.router(vertx)

    init {
        setupRoutes()
    }

    fun setupRoutes() {
        router.get("/api/trades").handler(this::handleGetTrades)
    }

    fun handleGetTrades(ctx: RoutingContext) {
        try {
            val trades = tradeService.getTrades()
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
