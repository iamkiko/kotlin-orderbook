# Kotlin Orderbook

This app is a simple in-memory order book for placing limit orders with order matching and viewing open orders and recent trades.
The system is designed to simulate basic functionalities of a crypto trading platform using BTCUSDC as the default currency pair.

## Prerequisites
- Kotlin 1.9.0
- JDK 17
- Gradle

## Setup
1. Clone the repository to your local machine.
2. Navigate to the project directory.
3. Run `gradle build` to build the project.
4. Start the application using `gradle run` or run the [App.kt](src/main/kotlin/com/example/orderbook/App.kt) file in IntelliJ

## Components

- **OrderBookService**: Manages the current state of buy and sell orders
- **OrderManager**: Responsible for adding orders to the OrderBook and generating snapshots of the current order book state
- **MatchingEngine**: Matches buy and sell orders based on price and quantity, updates the OrderBook accordingly, and records the trades
- **TradeService**: Records details of matched trades and retrieves information about past trades
- **OrderValidator**: Validates the details of orders before they are processed by the system

## Features

- **Order Management**: Add buy and sell orders with validations to ensure order integrity
- **Trade Matching**: Automatically match orders within the order book based on price and quantity
- **Trade Recording**: Record the details of each trade, including price, quantity, and the taker side
- **Order Book Snapshot**: Retrieve a snapshot of the current state of the order book, including all outstanding orders

### Libraries Used
- Vert.x (Core, Web, Web Client, Config, Auth JWT)
- Jackson for serialization (Kotlin module, Databind, Datatype JSR310)
- JUnit5 for testing
- MockK for mocking in tests
- Dotenv for environment variables
- Coroutines for performance testing

Feel free to peruse the build.gradle for the comprehensive list.

## Usage
By default, the app will run on port 8085, you can change that in the MainVerticle and in application.conf
To secure and manage access to placing limit orders on /api/orders/limit, our application utilizes JWT for authentication.

## Authentication Setup
Before being able to place limit orders on `/api/orders/limit`, you will need to:

1. Make a copy of `.env.example` and rename it to `.env` for your environment variables
2. Generate a symmetric key: run `openssl rand -base64 32` to generate a secure key (however any key will work for the handshake)
3. Add the symmetric key to your `.env` file, for `SYMMETRIC_KEY` (this is used to generate a JWT in the app)
4. Add your desired username/password to the `.env` under `USERNAME` and `PASSWORD` respectively.
5. Login to obtain a bearer token:
- Endpoint: `/api/login`
- Method: POST 
- Body: 
```json
{
  "username": "<YOUR_USERNAME_FROM_.ENV>",
  "password": "<YOUR_PASSWORD_FROM_.ENV>"
}
```

**Limitation:** As this is an in-memory approach, there is no external DB to assert the credentials against.

Use the obtained bearer token in the Authorization header placing limit orders on `/api/orders/limit`.

### Public Endpoints:
- `/api/orderbook` 
- `/api/recent-trades`

### Protected Endpoint
- `/api/orders/limit` requires signing in and including the bearer token in the Authorization header.

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

### Please ensure you have a valid auth token.
Submits a limit order to the order book. The order must specify the side (buy or sell), quantity, price, and currency pair.

**Request Body:**
```json
{
  "side": "BUY or SELL",
  "quantity": "number",
  "price": "number",
  "currencyPair": "BTCUSDC" // default
}
```
**Example Response:**

When order is not matched and added to the order book:
  ```json
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
 ```

When order is matched and fully filled:
  ```json
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
```
When order is matched and partially filled:
  ```json
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

**Example Response:**

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
    }
]
```



## Running Tests

The project includes a suite of tests to validate the functionality of each component. To run the tests:

1. Navigate to the project root directory in your terminal.
2. Run the test suite using your build tool or IDE's test runner.

The tests are categorized into unit tests except the MatchingEngine which is an integration test.

## Performance Testing/Simulation Script

A script to simulate orders en-masse exists in [PerformanceTest.kt](src/main/kotlin/com/example/orderbook/util/PerformanceTest.kt)

**NB:** This will create orders in your orderbook and you will need to re-start the service to clear it.

### Prerequisites

1. You need to be running the app
2. You need a username and password in your `.env`


### Configuration

The performance testing script is configured to target the following API endpoint by default:

- **API Endpoint**: `http://localhost:8085/api/orders/limit`

The default script settings are as follows:

- **Total Requests**: 100,000
- **Concurrency Level**: 100

These settings can be adjusted within the script to simulate different levels of load.

### Running the Performance Test

To run the performance testing script, execute the following command from the root directory of your project:

```shell
./gradlew runPerformanceTest
```
Alternatively, (a) in Intellij, you can locate the Gradle task under 'Other' and run it from there or (b) run the class directly from the file location.

### Interpreting Results
The script will provide the following output upon completion:

* **Total time taken:** The total time, in milliseconds, taken to send all requests and receive responses
* **Average rate:** The average rate of requests per second calculated `(requests / time in seconds)`
* **Request failures:** Any failed requests will be logged with their status code and response body

### Customization
To customize the performance test settings, edit the following variables within the script:

```kotlin
val totalRequests = 100000 // Total number of requests to send
val concurrencyLevel = 100 // Number of concurrent requests
```
Adjust these variables to target different endpoints, change the load level, or modify the concurrency of the requests.
