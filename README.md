# Investec Money Coach

A Spring Boot backend application that integrates with the Investec Programmable Banking / Account Information API to retrieve account data, balances, and transactions for building a personal finance coaching experience.

The long-term goal of this project is to create an **AI Family Money Coach** that helps users understand their real available money, protect bill money, track savings goals, and receive practical financial guidance based on their own transaction behaviour.

---

## Project Vision

Most banking apps show a balance, but they do not always show the user's **real safe-to-spend amount**.

For example:

> "You have R35,000 available, but after your expected bills and savings goal, only R7,500 is safe to spend."

This project aims to build a money coaching layer on top of banking data using the Investec Developer APIs.

---

## Current Features

The project currently supports:

- Spring Boot backend API
- Investec API configuration using environment variables
- OAuth2 client credentials authentication with Investec
- Access token validation endpoint
- Fetching Investec accounts
- Fetching account balances
- Clean layered structure using:
  - Controller
  - Service
  - API client
  - DTOs
  - Configuration properties

---

## Completed Milestones

### Milestone 1: Spring Boot application setup

- Created a new Spring Boot backend project
- Added basic application configuration
- Confirmed the backend runs locally

### Milestone 2: Investec authentication

- Added Investec API credentials through environment variables
- Implemented OAuth2 token retrieval
- Confirmed successful Bearer token generation
- Verified API scopes for accounts, balances, and transactions

### Milestone 3: Fetch Investec accounts

- Integrated with the Investec Account Information API
- Added endpoint to retrieve linked Investec accounts
- Confirmed account data is returned successfully

### Milestone 4: Fetch Investec account balance

- Added endpoint to retrieve balance for a selected account
- Uses the Investec account ID as a path variable
- Reuses the OAuth token flow for authenticated balance requests

---

## Tech Stack

- Java
- Spring Boot
- Maven
- REST APIs
- Investec Developer API
- OAuth2 Client Credentials Flow
- Postman for API testing

---

## Project Structure

```text
src/main/java/za/co/byteservices/moneycoach
│
├── client
│   └── InvestecApiClient.java
│
├── config
│   └── InvestecApiProperties.java
│
├── controller
│   └── InvestecController.java
│
├── dto
│   ├── InvestecAccountResponse.java
│   ├── InvestecBalanceResponse.java
│   ├── InvestecTokenResponse.java
│   ├── InvestecTransactionResponse.java
│   └── SafeToSpendResponse.java
│
├── model
│   └── SpendingCategory.java
│
├── service
│   ├── InvestecAccountService.java
│   ├── InvestecAuthService.java
│   └── MoneyCoachService.java
│
└── MoneyCoachApplication.java
