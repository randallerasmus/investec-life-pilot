# Investec Money Coach Context

## Project Identity

This repository is `investec-money-coach`, a Java 17 Spring Boot backend MVP for an AI money coaching product concept.

The current implementation connects to the Investec Programmable Banking / Account Information API and exposes REST endpoints for:

- Investec configuration checks
- OAuth2 client credentials token checks
- Account listing
- Account balance retrieval
- Account transaction retrieval
- A basic safe-to-spend calculation
- Hybrid AI money coach advice, grounded in deterministic safe-to-spend insights

The broader product vision is an AI Family Money Coach: a layer on top of banking data that helps users understand what is actually safe to spend after bills, planned savings, and financial goals.

## Current Reality

This is currently a backend integration MVP, not a complete AI coach.

Implemented:

- Spring Boot application entry point: `MoneyCoachApplication`
- Investec API property binding: `InvestecApiProperties`
- Investec OAuth2 token retrieval: `InvestecAuthService`
- Investec account, balance, and transaction API calls: `InvestecApiClient`
- Account orchestration: `InvestecAccountService`
- Safe-to-spend calculation: `MoneyCoachService`
- REST controllers: `InvestecController`, `MoneyCoachController`
- DTOs for token, account, balance, transaction, and safe-to-spend responses
- AI advice DTO and risk model: `MoneyCoachAdviceResponse`, `MoneyCoachRiskLevel`
- Deterministic advice service: `MoneyCoachAdviceService`
- Optional OpenAI advice rewrite client: `OpenAiAdviceClient`
- Basic Spring context load test

Not yet implemented:

- Mandatory AI/LLM dependency; advice works deterministically when OpenAI config is absent
- Transaction categorization logic using `SpendingCategory`
- Persistent users, budgets, goals, or spending rules
- Authentication/authorization for this service's own endpoints
- Consent management or multi-user banking access controls
- Database persistence
- Scheduled refreshes or background jobs
- Audit logging
- Observability
- Production-grade error handling
- Rate limiting
- OpenAPI documentation
- Frontend/mobile experience
- Financial-advice guardrails beyond README wording

## Tech Stack

- Java 17
- Spring Boot 4.0.6
- Maven wrapper
- Spring Web MVC
- Spring validation
- `RestTemplate` for outbound Investec API calls
- Optional OpenAI Responses API call for rewriting deterministic coaching advice
- Environment variables for Investec credentials

## Configuration

Application config is in `src/main/resources/application.properties`.

Required environment variables:

- `INVESTEC_CLIENT_ID`
- `INVESTEC_CLIENT_SECRET`
- `INVESTEC_API_KEY`

Optional environment variables:

- `OPENAI_API_KEY`
- `OPENAI_MODEL`

Default app port: `8080`

Default Investec base URL: `https://openapi.investec.com`

Default OpenAI base URL: `https://api.openai.com/v1`

## Important Endpoints

Investec support endpoints:

- `GET /api/investec/config-check`
- `GET /api/investec/token-check`
- `GET /api/investec/accounts`
- `GET /api/investec/accounts/{accountId}/balance`
- `GET /api/investec/accounts/{accountId}/transactions?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD`

Money coach endpoint:

- `GET /api/coach/accounts/{accountId}/safe-to-spend`
- `GET /api/coach/accounts/{accountId}/advice`

Safe-to-spend query parameters:

- `bondOrRent`
- `schoolFees`
- `insurance`
- `groceries`
- `fuel`
- `subscriptions`
- `otherBills`
- `goalSavingAmount`

The safe-to-spend calculation is currently:

```text
availableBalance - estimatedBills - goalSavingAmount
```

The advice endpoint returns risk level, summary, recommendations, whether AI rewrote the summary, and a disclaimer. Risk levels are:

- `CRITICAL`: safe-to-spend is below zero.
- `TIGHT`: safe-to-spend is zero or less than 10% of available balance.
- `HEALTHY`: safe-to-spend is at least 10% of available balance.

## Architecture Notes

Current request flow for Investec data:

```text
Controller -> InvestecAccountService -> InvestecAuthService -> InvestecApiClient -> Investec API
```

Current request flow for safe-to-spend:

```text
MoneyCoachController -> MoneyCoachService -> InvestecAccountService -> Investec API balance -> calculation
```

Current request flow for advice:

```text
MoneyCoachController -> MoneyCoachAdviceService -> MoneyCoachService -> deterministic advice -> optional OpenAiAdviceClient rewrite
```

There is no database. All coaching inputs are passed as query parameters at request time.

## Development Commands

Run tests:

```powershell
.\mvnw.cmd test
```

Run app locally:

```powershell
.\mvnw.cmd spring-boot:run
```

Check config after startup:

```text
http://localhost:8080/api/investec/config-check
```

## Product Direction

Recommended next product increments:

1. Add unit tests for `MoneyCoachService` with mocked balance data.
2. Add validation for negative bill and savings inputs.
3. Replace query-parameter coaching inputs with a proper request model.
4. Add transaction categorization and monthly spend summaries.
5. Add goal and recurring bill models.
6. Add a persistence layer.
7. Expand AI coaching prompts only after deterministic money calculations are reliable.
8. Keep guardrails so generated coaching is educational, explainable, and not regulated financial advice.

## Investec Usage Assessment

Investec could use this project as an internal innovation prototype or developer API demo after code ownership, branding, and API policy checks.

Investec should not use this as a production customer-facing product in its current state. It needs security, compliance, privacy, operational hardening, testing, and legal review before it can safely handle real customer data at bank scale.

Key production gaps:

- No service authentication or authorization
- No consent lifecycle
- No PII/data retention model
- No secrets management beyond environment variables
- No audit trail
- No resilience policies, timeout configuration, or retry strategy
- No formal threat model
- No compliance review for financial advice boundaries
- No license metadata in `pom.xml`
- README appears to have encoding issues for emoji/special characters

## How Assistants Should Work In This Repo

When `/moneycoach` is invoked, load this file first and treat it as the project brief.

Default behavior:

- Preserve the MVP's simple Spring Boot style unless the user asks for larger architecture changes.
- Keep financial calculations deterministic and tested before adding AI-generated coaching.
- Do not commit secrets or real banking data.
- Treat all banking data as sensitive.
- Avoid presenting the app as giving financial advice.
- If expanding AI, make it explain deterministic calculations rather than inventing numbers.
- Prefer clear REST DTOs, service-layer tests, and explicit validation.
