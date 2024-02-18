package com.example.orderbook.api.controllers

import io.vertx.core.Vertx
import com.example.orderbook.service.TradeService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class TradeController(vertx: Vertx, private val tradeService: TradeService) {
    private val router: Router = Router.router(vertx)
    private val mapper: ObjectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    init {
        setupRoutes()
    }

    fun setupRoutes() {
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
