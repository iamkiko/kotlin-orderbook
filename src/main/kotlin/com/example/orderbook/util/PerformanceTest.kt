package com.example.orderbook.util

import io.github.cdimascio.dotenv.dotenv
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

fun main() {
    val dotenv = dotenv()
    val vertx = Vertx.vertx()
    val client = WebClient.create(vertx, WebClientOptions().apply {
        isKeepAlive = true
        idleTimeout = 10
    })

    val loginApiUrl = "http://localhost:8085/api/login"
    val apiUrl = "http://localhost:8085/api/orders/limit"
    val totalRequests = 100000
    val concurrencyLevel = 100

    val username = dotenv["USERNAME"] ?: "satoshi"
    val password = dotenv["PASSWORD"] ?: "n@k@m0t0!!"
    var token: String? = null

    // need to obtain a JWT token as /login is an route requiring auth
    runBlocking {
        val loginResponse = client.postAbs(loginApiUrl)
            .putHeader("Content-Type", "application/json")
            .sendBuffer(Buffer.buffer("""{"username": "$username", "password": "$password"}"""))
            .coAwait()

        token = if (loginResponse.statusCode() == 200) {
            loginResponse.bodyAsJsonObject().getString("token")
        } else {
            println("Failed to login and obtain JWT token")
            return@runBlocking
        }
    }

    println("======> Authentication successful....")
    println("======> Buckle up! Starting to send requests...")

    runBlocking {
        val timeTaken = measureTimeMillis {
            val jobs = List(concurrencyLevel) {
                CoroutineScope(vertx.dispatcher()).launch {
                    repeat(totalRequests / concurrencyLevel) {
                        val response = client.postAbs(apiUrl)
                            .putHeader("Content-Type", "application/json")
                            .putHeader("Authorization", "Bearer $token")
                            .sendBuffer(
                                Buffer.buffer(
                                    """
                                                    {
                                                          "side": "BUY",
                                                          "quantity": 5,
                                                          "price": 30000,
                                                          "currencyPair": "BTCUSDC"
                                                    }
                                                """.trimIndent()
                                )
                            ).coAwait()
                        if (response.statusCode() != 201) {
                            println("======> Request failed with status code: ${response.statusCode()}")
                        }
                    }
                }
            }
            jobs.forEach { it.join() }
        }

        val requestsPerSecond = totalRequests / (timeTaken / 1000.0)
        println("======> All requests have been sent")
        println("======> Total time taken for $totalRequests requests: $timeTaken ms")
        println("======> Average rate: $requestsPerSecond requests per second")
    }

    client.close()
    vertx.close()
}
