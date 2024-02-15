package com.example.orderbook.api.controllers

import com.example.orderbook.api.dto.OrderDTO
import com.example.orderbook.model.Order
import com.example.orderbook.model.OrderSide
import com.example.orderbook.service.OrderBookService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

class OrderController(vertx: Vertx, private val orderBookService: OrderBookService, private val mapper: ObjectMapper) {
    private val router: Router = Router.router(vertx)

    init {
        setupRoutes()
    }

    fun setupRoutes() {
        router.get("/api/orderbook").handler(this::handleGetOrderBook)
        router.post("/api/orders/limit").handler(this::handleAddLimitOrder)
    }

    fun handleGetOrderBook(ctx: RoutingContext) {
        val orderBook = orderBookService.getOrderBookDTO()
        val json = mapper.writeValueAsString(orderBook)
        ctx.response()
            .putHeader("Content-Type", "application/json")
            .end(json)
    }

    fun handleAddLimitOrder(ctx: RoutingContext) {
        try {
            val orderRequest = mapper.readValue(ctx.body().asString(), OrderDTO::class.java)
            val order = Order(
                side = OrderSide.valueOf(orderRequest.side),
                quantity = orderRequest.quantity,
                price = orderRequest.price,
                currencyPair = orderRequest.currencyPair
            )
            val additionStatusToReturnToUser = orderBookService.addOrder(order)
            ctx.response()
                .setStatusCode(201)
                .putHeader("Content-Type", "application/json")
                .end(Json.encodePrettily(additionStatusToReturnToUser))
        } catch (e: IllegalArgumentException) {
            ctx.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end(Json.encodePrettily(mapOf("error" to e.message)))
        } catch (e: MismatchedInputException) {
            ctx.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end(Json.encodePrettily(mapOf("error" to "One of the fields is missing: 'side', 'price', 'quantity' or 'currencyPair'")))
        } catch (e: InternalError) {
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(Json.encodePrettily(mapOf("error" to e.message)))
        }
    }
}
