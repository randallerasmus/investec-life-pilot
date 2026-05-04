# LifePilot

LifePilot is a Spring Boot backend MVP for life-event financial simulation and money coaching on top of live Investec account data.

Tagline:

> See the financial impact of a life decision before you make it.

## What This Project Does Today

The current backend connects to the Investec Programmable Banking / Account Information API and can:

- retrieve Investec accounts
- retrieve account balances
- retrieve transactions
- calculate safe-to-spend money after bills and savings goals
- classify budget health as `HEALTHY`, `TIGHT`, or `CRITICAL`
- return educational budgeting recommendations
- optionally rewrite deterministic coaching advice with OpenAI

The existing Money Coach functionality remains the internal budgeting module inside LifePilot.

## Product Direction

LifePilot is evolving toward a life-event simulator.

Example user question:

> What happens if I send my child to private school for R6,500 per month?

The target LifePilot response should explain:

- current safe-to-spend money
- projected safe-to-spend after the life event
- monthly impact
- risk level
- survival budget impact
- practical recommendations

## Current API Endpoints

Investec support endpoints:

```text
GET /api/investec/config-check
GET /api/investec/token-check
GET /api/investec/accounts
GET /api/investec/accounts/{accountId}/balance
GET /api/investec/accounts/{accountId}/transactions?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD
```

Money Coach module endpoints:

```text
GET /api/coach/accounts/{accountId}/safe-to-spend
GET /api/coach/accounts/{accountId}/advice
```

Planned LifePilot endpoint:

```text
POST /api/lifepilot/scenarios
```

## Configuration

Required environment variables:

```text
INVESTEC_CLIENT_ID
INVESTEC_CLIENT_SECRET
INVESTEC_API_KEY
```

Optional OpenAI environment variables:

```text
OPENAI_API_KEY
OPENAI_MODEL
```

If OpenAI is not configured, the advice endpoint still returns deterministic recommendations.

## Tech Stack

- Java 17
- Spring Boot 4.0.6
- Maven
- Spring Web MVC
- Investec Open API
- Optional OpenAI Responses API integration

## Run Locally

```powershell
.\mvnw.cmd spring-boot:run
```

Default URL:

```text
http://localhost:8080
```

## Run Tests

```powershell
.\mvnw.cmd test
```

## Disclaimer

LifePilot provides educational budgeting and planning guidance only. It does not provide regulated financial advice and does not make financial decisions on behalf of users.

Do not commit API keys, secrets, tokens, private certificates, or real customer data.

## Author

Randall Erasmus

- GitHub: https://github.com/randallerasmus

