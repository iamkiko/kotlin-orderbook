# Kotlin Orderbook

This app is a simple in-memory order book for placing limit orders with order matching and viewing open orders and recent trades. It's designed to mimic basic functionalities of trading platforms, offering APIs for interacting with the order book, submitting limit orders, and viewing recent trade activities.

The system is designed to simulate key functionalities of a crypto trading platform but only supports BTCUSDC as a currency pair for now.

## Prerequisites
- Kotlin 1.9.0
- Gradle

## Setup
1. Clone the repository to your local machine.
2. Navigate to the project directory.
3. Run `gradle build` to build the project.
4. Start the application using `gradle run`.


## Components

- **OrderBook**: Manages the current state of buy and sell orders.
- **OrderManager**: Responsible for adding orders to the OrderBook and generating snapshots of the current order book state.
- **MatchingEngine**: Matches buy and sell orders based on price and quantity, updates the OrderBook accordingly, and records the trades.
- **TradeService**: Records details of matched trades and retrieves information about past trades.
- **OrderValidator**: Validates the details of orders before they are processed by the system.

## Features

- **Order Management**: Add buy and sell orders with validations to ensure order integrity.
- **Trade Matching**: Automatically match orders within the order book based on price and quantity.
- **Trade Recording**: Record the details of each trade, including price, quantity, and the taker side.
- **Order Book Snapshot**: Retrieve a snapshot of the current state of the order book, including all outstanding orders.

### Libraries Used
The project utilizes various libraries from the Vert.x ecosystem for creating reactive web applications in Kotlin, as well as Jackson for JSON serialization/deserialization, and JUnit, MockK for testing.

- Vert.x (Core, Web, Web Client, Config, Auth JWT)
- Jackson (Kotlin module, Databind, Datatype JSR310)
- JUnit Jupiter API for testing
- MockK for mocking in tests

## Usage
By default, the app will run on port 8085.


## Order Book
### **GET `/api/orderbook`**

  Retrieves the current state of the order book, including all open orders.

  **Example Response:**
  ```json
  {
    "asks": [
        {
            "side": "SELL",
            "quantity": 5,
            "price": 5000,
            "currencyPair": "BTCUSDC",
            "orderCount": 1 // amount of orders at this price point
        }
    ],
    "bids": [
        {
            "side": "BUY",
            "quantity": 5,
            "price": 4000,
            "currencyPair": "BTCUSDC",
            "orderCount": 1 
        }
    ],
    "lastUpdated": "2024-02-20T19:00:00.340939Z", // when the last state update occurred
    "tradeSequenceNumber": 0 // the amount of trades that have occurred
  }
```

## Submit Limit Order
### **POST`/api/orders/limit`**

Submits a limit order to the order book. The order must specify the side (buy or sell), quantity, price, and currency pair.

**Request Body:**
```json
{
  "side": "BUY or SELL",
  "quantity": "number",
  "price": "number",
  "currencyPair": "string"
}
```
**Example Response:**
  ```json
// when not matched
  {
  "success": true,
  "message": "Order added to book, no immediate match found, pending fulfillment.",
  "partiallyFilled": false,
  "remainingQuantity": 5,
  "orderDetails": {
    "side": "SELL",
    "quantity": 5,
    "price": 0,
    "currencyPair": "BTCUSDC"
  },
  "orderMatched": false
}

// when order is fully filled:
{
  "success": true,
  "message": "Order fully filled.",
  "partiallyFilled": false,
  "remainingQuantity": 0,
  "orderDetails": {
    "side": "SELL",
    "quantity": 4,
    "price": 4000,
    "currencyPair": "BTCUSDC"
  },
  "orderMatched": true
}

// when order is partially filled:
{
  "success": true,
  "message": "Order partially filled.",
  "partiallyFilled": true,
  "remainingQuantity": 3,
  "orderDetails": {
    "side": "SELL",
    "quantity": 1,
    "price": 4000,
    "currencyPair": "BTCUSDC"
  },
  "orderMatched": true
}
```

## Recent Trades
### **GET`/api/recent-trades`**

Fetches a list of recent trades executed in the system.

Response:

```json
[
    {
        "id": "3dadca48-11f1-40d1-9847-da918b8b16a7",
        "price": 3000,
        "quantity": 5,
        "currencyPair": "BTCUSDC",
        "timestamp": "2024-02-20T19:00:00.340939Z",
        "takerSide": "SELL"
    },
    {
        "id": "51dba9e6-7ef8-4616-a238-6842701398fd",
        "price": 4000,
        "quantity": 4,
        "currencyPair": "BTCUSDC",
        "timestamp": "2024-02-20T19:00:00.340939Z",
        "takerSide": "BUY"
    },
]
```



## Running Tests

The project includes a suite of tests to validate the functionality of each component. To run the tests:

1. Navigate to the project root directory in your terminal.
2. Run the test suite using your build tool or IDE's test runner.

The tests are categorized into unit tests with the exception of the MatchingEngine which is an integration test.
