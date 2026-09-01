# LifePilot Context

## Project Identity

This repository is `lifepilot`, a Java 17 Spring Boot backend MVP for life-event financial simulation and money coaching on top of live Investec account data.

The current implementation connects to the Investec Programmable Banking / Account Information API and exposes REST endpoints for:

- Investec configuration and token checks
- Account listing, balance, and transaction retrieval
- A basic safe-to-spend calculation
- Hybrid Money Coach advice, grounded in deterministic safe-to-spend insights
- LifePilot scenario simulation for life-event monthly impact
- A day-by-day balance forecast with dated cashflow risks, driven by recurring payment detection
- An advanced safe-to-spend engine, a retrieval-grounded AI coach, responsible-AI guardrails, and evaluation criteria

The broader product vision is LifePilot: a life-event financial simulator that helps users understand whether major life decisions are affordable before they commit to them.

Money Coach remains the internal budgeting module inside LifePilot.

## Current Reality

This is a backend integration MVP. All calculations are deterministic and unit-tested; AI is optional and only ever rewrites numbers the deterministic layer already produced.

Implemented:

Core Investec integration

- Spring Boot application entry point: `MoneyCoachApplication`
- Investec property binding: `InvestecApiProperties`; OpenAI property binding: `OpenAiProperties`
- Investec OAuth2 client-credentials token retrieval: `InvestecAuthService` (no token caching; a token is fetched per call)
- Investec account, balance, and transaction API calls: `InvestecApiClient`
- Account orchestration: `InvestecAccountService`
- CORS for the local frontend: `CorsConfig`

Money Coach module

- Safe-to-spend calculation: `MoneyCoachService`
- Deterministic advice with risk levels and recommendations: `MoneyCoachAdviceService`
- Optional OpenAI advice rewrite: `AiAdviceClient` interface, `OpenAiAdviceClient` implementation

LifePilot simulation and forecasting

- Scenario simulation: `LifePilotScenarioService`, `LifePilotScenarioType`
- Recurring payment detection over transaction history: `RecurringPaymentDetector`, `RecurringCadence`, `RecurringPayment`
- Day-by-day balance forecasting with dated cashflow risk windows: `BalanceForecastService`, `ForecastDay`, `CashflowRisk`, `BalanceForecastResponse`
- Shared debit/credit direction rules: `TransactionAmounts` (package-private helper), `TransactionDates`

LifePilot AI layer

- Advanced safe-to-spend engine that estimates recurring expenses from live transactions: `SafeToSpendEngineService`
- In-memory keyword RAG over seeded financial-education documents: `KnowledgeRagService`
- Retrieval-grounded coach that answers a natural-language question: `AiCoachService`
- Responsible-AI guardrails that scan an answer and substitute a safe one: `ResponsibleAiGuardrailsService`
- Static evaluation scenarios and criteria: `EvaluationService`

Controllers

- `InvestecController`, `MoneyCoachController`, `LifePilotController`, `LifePilotAiController`

Tests

- 15 test classes under `src/test/java`, covering every service plus the controllers, CORS config, and Spring context load

Not yet implemented:

- Survival budget impact calculation
- Transaction categorization using `SpendingCategory` (the enum exists but nothing populates it)
- Persistent users, budgets, goals, or spending rules
- Database persistence — the knowledge store is in-memory (`CopyOnWriteArrayList`) and resets on restart
- Vector embeddings for retrieval; `KnowledgeRagService` scores by keyword overlap only
- Authentication/authorization for this service's own endpoints
- Consent management or multi-user banking access controls
- Scheduled refreshes or background jobs
- Audit logging
- Observability
- Production-grade error handling
- Rate limiting
- OpenAPI documentation
- Frontend/mobile experience
- Financial-advice guardrails beyond the guardrail keyword scan and disclaimer wording

## Tech Stack

- Java 17
- Spring Boot 4.0.6 (`spring-boot-starter-webmvc`, `spring-boot-starter-validation`, devtools, Lombok optional)
- Maven wrapper
- `RestTemplate` for outbound Investec and OpenAI calls
- Optional OpenAI Responses API call for rewriting deterministic coaching advice
- Environment variables for Investec and OpenAI credentials
- No database, no JPA, no vector store

## Configuration

Application config is in `src/main/resources/application.properties`.

Required environment variables:

- `INVESTEC_CLIENT_ID`
- `INVESTEC_CLIENT_SECRET`
- `INVESTEC_API_KEY`

Optional environment variables:

- `OPENAI_API_KEY`
- `OPENAI_MODEL`

Both must be set before any AI rewrite happens; if either is blank, `OpenAiAdviceClient` returns empty and callers fall back to the deterministic text.

Default app port: `8080`

Default Investec base URL: `https://openapi.investec.com`

Default OpenAI base URL: `https://api.openai.com/v1`

CORS allows `http://localhost:8081` and `http://127.0.0.1:8081` on `/api/**` for `GET`, `POST`, `OPTIONS`.

## Important Endpoints

Investec support endpoints:

- `GET /api/investec/config-check`
- `GET /api/investec/token-check`
- `GET /api/investec/accounts`
- `GET /api/investec/accounts/{accountId}/balance`
- `GET /api/investec/accounts/{accountId}/transactions?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD`

Money Coach module endpoints:

- `GET /api/coach/accounts/{accountId}/safe-to-spend`
- `GET /api/coach/accounts/{accountId}/advice`

LifePilot simulation and forecast endpoints:

- `POST /api/lifepilot/scenarios`
- `GET /api/lifepilot/accounts/{accountId}/forecast` (query parameters)
- `POST /api/lifepilot/accounts/{accountId}/forecast` (request body, body optional)

LifePilot AI endpoints:

- `GET /api/lifepilot/knowledge/documents`
- `POST /api/lifepilot/knowledge/documents`
- `POST /api/lifepilot/knowledge/search`
- `POST /api/lifepilot/safe-to-spend/accounts/{accountId}/advanced`
- `POST /api/lifepilot/ai-coach/accounts/{accountId}/ask`
- `POST /api/lifepilot/guardrails/check`
- `GET /api/lifepilot/evaluations/default`

## Calculations

### Basic safe-to-spend

Query parameters on both Money Coach endpoints, all optional and defaulting to `0`:

`bondOrRent`, `schoolFees`, `insurance`, `groceries`, `fuel`, `subscriptions`, `otherBills`, `goalSavingAmount`

```text
availableBalance - estimatedBills - goalSavingAmount
```

Risk levels (`MoneyCoachRiskLevel`), shared by advice, scenarios, and the advanced engine:

- `CRITICAL`: the resulting amount is below zero.
- `TIGHT`: the amount is zero, or less than 10% of the available balance.
- `HEALTHY`: the amount is at least 10% of the available balance.

The advice endpoint returns risk level, summary, recommendations, whether AI rewrote the summary, and a disclaimer.

### Advanced safe-to-spend

`POST /api/lifepilot/safe-to-spend/accounts/{accountId}/advanced` takes `payday`, `emergencyBuffer`, `plannedPurchaseAmount`, `plannedPurchaseDescription`.

```text
safeToSpend = currentBalance - estimatedRecurringExpenses - emergencyBuffer
```

Recurring expenses are estimated from the last 30 days of transactions: the larger of (a) debits whose description repeats or matches a keyword (`rent`, `insurance`, `subscription`, `school`, `loan`, `debit order`) and (b) average daily outflow projected over the days until payday. Payday defaults to 14 days out. Affordability against the planned purchase is `AFFORDABLE` / `TIGHT` / `NOT_AFFORDABLE`, with `TIGHT` at the greater of 5% of balance or 500. Investec call failures are caught and flagged as `fallbackUsed` rather than propagated.

### Balance forecast

`BalanceForecastRequest`: `horizonDays` (1-365, default 90), `minimumBalanceThreshold` (default 0), `expectedMonthlyIncome`, `nextIncomeDate`, `includeDiscretionarySpend` (default true).

`BalanceForecastService` pulls 180 days of history, runs `RecurringPaymentDetector` for both outflows and inflows, places each detected item on its own future due date, and applies an average daily discretionary figure for everything left over. Day zero carries recurring items already due but no discretionary spend, since today's variable spending is already inside the reported balance. A supplied `expectedMonthlyIncome` replaces detected income rather than adding to it. Consecutive days below the threshold are grouped into a single `CashflowRisk` window — `CRITICAL` when the balance goes negative, `TIGHT` otherwise.

`RecurringPaymentDetector` groups transactions by a normalised merchant key (up to 3 tokens), then accepts a group only when it has at least 2 occurrences, a median gap matching a `RecurringCadence` band (weekly, fortnightly, monthly, quarterly, annual), amount deviation within 25%, and confidence at least 0.50. A payment unseen for more than twice its own cadence is treated as cancelled. The detector is deterministic and does no I/O — callers supply both the transactions and the date to project from.

`TransactionAmounts` is the single place that decides direction: Investec reports positive amounts with `type` of `DEBIT`/`CREDIT`, while some fixtures use signed amounts. Both conventions are honoured there so no call site re-derives the rule.

### AI coach

`POST /api/lifepilot/ai-coach/accounts/{accountId}/ask` takes `question` (required), `payday`, `emergencyBuffer`. It extracts a purchase amount from the question text, runs the advanced safe-to-spend engine, retrieves knowledge snippets, composes a deterministic answer, optionally lets the AI client rewrite it, then passes the result through the guardrails before returning. The response carries the answer, risk level, a confidence score, a `basedOn` list of the figures used, the retrieved knowledge, and any guardrail warnings.

`ResponsibleAiGuardrailsService` scans for certainty language, concentrated-investment advice, leverage suggestions, unqualified instructions, and claims implying unavailable data. A clean answer gets the educational disclaimer appended; a flagged answer is replaced entirely with a safe one.

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

Current request flow for scenarios:

```text
LifePilotController -> LifePilotScenarioService -> MoneyCoachService -> scenario impact calculation
```

Current request flow for the balance forecast:

```text
LifePilotController -> BalanceForecastService -> InvestecAccountService (balance + 180 days of transactions)
                                              -> RecurringPaymentDetector -> day-by-day timeline -> cashflow risks
```

Current request flow for the AI coach:

```text
LifePilotAiController -> AiCoachService -> SafeToSpendEngineService -> Investec balance + transactions
                                        -> KnowledgeRagService (retrieval)
                                        -> optional AiAdviceClient rewrite
                                        -> ResponsibleAiGuardrailsService -> final answer
```

There is no database. Coaching inputs are passed per request, and the knowledge store lives in memory for the life of the process.

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

## Design Documents

- `docs/superpowers/specs/2026-05-03-ai-money-coach-advice-design.md`
- `docs/superpowers/plans/2026-05-03-ai-money-coach-advice.md`
- `docs/superpowers/specs/2026-05-22-lifepilot-v2-mysql-ai-design.md` (MySQL persistence and AI upgrades; not yet implemented)

## Product Direction

Recommended next product increments:

1. Add survival budget impact calculation.
2. Add validation for negative bill, savings, and scenario inputs — `LifePilotScenarioRequest` and `AdvancedSafeToSpendRequest` currently carry no constraints.
3. Add AI explanation support for scenario responses.
4. Replace query-parameter coaching inputs with a proper request model.
5. Add transaction categorization and monthly spend summaries, populating `SpendingCategory`.
6. Add goal and recurring bill models.
7. Add a persistence layer, per `2026-05-22-lifepilot-v2-mysql-ai-design.md`.
8. Cache the Investec access token instead of fetching one per outbound call.

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
- Knowledge documents are accepted from any caller with no moderation or ownership model

## How Assistants Should Work In This Repo

When `/lifepilot` is invoked, load this file first and treat it as the project brief.

When `/moneycoach` is invoked, treat it as a compatibility alias for `/lifepilot`.

Default behavior:

- Preserve the MVP's simple Spring Boot style unless the user asks for larger architecture changes.
- Treat LifePilot as the public product name.
- Treat Money Coach as the internal budgeting module.
- Keep financial calculations deterministic and tested before adding AI-generated coaching.
- Do not commit secrets or real banking data.
- Treat all banking data as sensitive.
- Avoid presenting the app as giving financial advice.
- If expanding AI, make it explain deterministic calculations rather than inventing numbers.
- Prefer clear REST DTOs, service-layer tests, and explicit validation.
- Keep transaction direction logic in `TransactionAmounts` rather than re-deriving debit/credit rules at a call site.
- Keep detection and forecasting services free of I/O and clock reads: pass transactions and an `asOf` date in, so the behaviour stays testable.
