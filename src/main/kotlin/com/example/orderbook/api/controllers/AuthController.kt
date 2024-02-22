package com.example.orderbook.api.controllers

import com.example.orderbook.api.dto.LoginDTO
import com.example.orderbook.util.Serializer
import io.github.cdimascio.dotenv.dotenv
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

class AuthController(vertx: Vertx, private val jwtAuth: JWTAuth) {
    val router: Router = Router.router(vertx)
    private val mapper = Serializer.jacksonObjectMapper
    private val dotenv = dotenv()

    init {
        router.route().handler(BodyHandler.create())
        setupRoutes()
    }

    fun setupRoutes() {
        router.post("/api/login").handler(this::handleLogin)
    }

    fun handleLogin(ctx: RoutingContext) {
        try {
            val credentials = mapper.readValue(ctx.body().asString(), LoginDTO::class.java)
            if (dotenv["USERNAME"]  == credentials.username && dotenv["PASSWORD"] == credentials.password) {
                val token = jwtAuth.generateToken(
                    json {
                        obj("sub" to credentials.username, "permissions" to listOf("user"))
                    }, JWTOptions()
                )
                ctx.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encodePrettily(mapOf("token" to token)))
            } else {
                ctx.response()
                    .setStatusCode(401)
                    .putHeader("Content-Type", "application/json")
                    .end(Json.encodePrettily(mapOf("error" to "Invalid credentials!")))
            }
        } catch (e: Exception) {
            ctx.response()
                .setStatusCode(500)
                .putHeader("Content-Type", "application/json")
                .end(Json.encodePrettily(mapOf("error" to "An error occurred on our end.")))
        }
    }
}


