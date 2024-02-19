package com.example.orderbook.service

import com.example.orderbook.model.Order
import com.example.orderbook.model.OrderSide
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderValidatorTest {
    private fun invalidOrdersList() = listOf(
        Arguments.of(Order(OrderSide.BUY, BigDecimal("-1"), BigDecimal("10000"), "BTCUSDC")),
        Arguments.of(Order(OrderSide.SELL, BigDecimal("0"), BigDecimal("15000"), "BTCUSDC")),
        Arguments.of(Order(OrderSide.BUY, BigDecimal("1"), BigDecimal("-10000"), "BTCUSDC")),
        Arguments.of(Order(OrderSide.SELL, BigDecimal("1"), BigDecimal("0"), "BTCUSDC")),
        Arguments.of(Order(OrderSide.BUY, BigDecimal("1"), BigDecimal("10000"), "not_a_currency_pair"))
    )

    private lateinit var orderValidator: OrderValidator


    @BeforeEach
    fun setUp() {
        orderValidator = OrderValidator()
    }

    @Test
    fun `should validate correct order`() {
        // given ... a valid order
        val validOrder = Order(OrderSide.BUY, BigDecimal("1"), BigDecimal("10000"), "BTCUSDC")

        // when ... we verify it is valid
        val validationResult = orderValidator.isValidOrder(validOrder)

        // then ... the validation should pass
        assertTrue(validationResult)
    }

    @ParameterizedTest
    @MethodSource("invalidOrdersList")
    fun `should reject invalid orders`(order: Order) {
        // given ... a stream of invalid orders
        // when ... we verify them individually
        val validationResult = orderValidator.isValidOrder(order)

        // then ... the validation should fail for each fail case
        assertFalse(validationResult)
    }
}
