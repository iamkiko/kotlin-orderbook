# Kotlin Orderbook

This project implements a simplified trading system, focusing on creating and matching buy and sell orders, recording trades, and maintaining an in-memory order book. 
The system is designed to simulate key functionalities of a crypto trading platform but only supports BTCUSDC as a currency pair for now.

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

## Setup

To set up the project locally, follow these steps:

1. Clone the repository to your local machine.
2. Ensure you have a compatible Kotlin development environment set up.
3. Build the project using your preferred IDE or build tool (e.g., IntelliJ IDEA, Gradle).

## Running Tests

The project includes a comprehensive suite of tests to validate the functionality of each component. To run the tests:

1. Navigate to the project root directory in your terminal.
2. Run the test suite using your build tool or IDE's test runner.

The tests are categorized into unit and integration tests, focusing on individual components and their interactions, respectively.

## Usage

