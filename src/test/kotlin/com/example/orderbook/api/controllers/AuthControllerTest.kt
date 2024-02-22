package com.example.orderbook.api.controllers

import io.github.cdimascio.dotenv.dotenv
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.PubSecKeyOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(VertxExtension::class)
class AuthControllerTest {
    private val dotenv = dotenv()
    private lateinit var vertx: Vertx
    private lateinit var webClient: WebClient
    private lateinit var jwtAuth: JWTAuth
    private lateinit var authController: AuthController

    @BeforeEach
    fun setUp(vertx: Vertx, vertxTestContext: VertxTestContext) {
        this.vertx = vertx
        this.webClient = WebClient.create(vertx)
        this.jwtAuth = JWTAuth.create(vertx, JWTAuthOptions().apply {
            addPubSecKey(
                PubSecKeyOptions()
                .setAlgorithm("HS256")
                .setBuffer(dotenv["SYMMETRIC_KEY"]))
        })


        this.authController = AuthController(vertx, jwtAuth)
        val router = this.authController.router
        router.route().handler(BodyHandler.create())
        authController.setupRoutes()

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8888, vertxTestContext.succeedingThenComplete())
    }

    @Test
    fun `should return a token when a user with valid credentials logs in`(vertxTestContext: VertxTestContext) {
        // given ... a valid login
        val loginJson = JsonObject().put("username", "satoshi").put("password", "halfinney@09!")
        val request = webClient.post(8888, "localhost", "/api/login")
            .putHeader("Content-Type", "application/json")

        // when ... sent to our controller
        val responseFuture = request.sendJsonObject(loginJson)

        // then ... we can assert that it was successful based on API response code (created)
        responseFuture.onComplete(vertxTestContext.succeeding { response ->
            vertxTestContext.verify {
                assertEquals(200, response.statusCode())
                assertTrue(response.bodyAsJsonObject().containsKey("token"))
                vertxTestContext.completeNow()
            }
        })
    }

    @Test
    fun `should return 401 when a user with invalid credentials logs in`(vertxTestContext: VertxTestContext) {
        // given ... an invalid login
        val loginJson = JsonObject().put("username", "invalid_user").put("password", "wrong_password")
        val request = webClient.post(8888, "localhost", "/api/login")
            .putHeader("Content-Type", "application/json")

        // when ... sent to our controller
        val responseFuture = request.sendJsonObject(loginJson)

        // then ... we can assert that our endpoint returns a 401 unauthorized
        responseFuture.onComplete(vertxTestContext.succeeding { response ->
            vertxTestContext.verify {
                assertEquals(401, response.statusCode())
                vertxTestContext.completeNow()
            }
        })
    }
}
