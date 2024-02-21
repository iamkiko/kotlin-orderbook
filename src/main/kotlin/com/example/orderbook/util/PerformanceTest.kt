package com.example.orderbook.util

import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

fun main() {
    val vertx = Vertx.vertx()
    val client = WebClient.create(vertx, WebClientOptions().apply {
        isKeepAlive = true
        idleTimeout = 10
    })

    val apiUrl = "http://localhost:8085/api/orders/limit"
    val totalRequests = 100000
    val concurrencyLevel = 100

    println("======> Buckle up! Starting to send requests...")

    runBlocking {
        val timeTaken = measureTimeMillis {
            val jobs = List(concurrencyLevel) {
                CoroutineScope(vertx.dispatcher()).launch {
                    repeat(totalRequests / concurrencyLevel) {
                        val response = client.postAbs(apiUrl)
                            .putHeader("Content-Type", "application/json")
                            .sendBuffer(
                                io.vertx.core.buffer.Buffer.buffer(
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
