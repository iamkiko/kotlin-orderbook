package com.example.orderbook.model

enum class OrderSide {
    BUY, SELL
}

data class Order(
//    val orderId: String, TODO(): come back to this e.g. cancelling orders as a stretch goal
    val side: OrderSide,
    val quantity: String, //TODO() Double?
    val price: String, //TODO() Double?
    val currencyPair: String,
    val orderCount: Int = 1
) {
    companion object {
        val askComparator: Comparator<Order> = compareBy { it.price } // lowest sell price is first in queue to be matched
        val bidComparator: Comparator<Order> = compareByDescending { it.price } // highest buy price is first in queue to be matched
    }
}
