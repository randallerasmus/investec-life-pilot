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

## Repositories

LifePilot is two repositories. This file lives in the backend and is the brief for both.

| Repository | Local path | Contents |
| --- | --- | --- |
| `investec-life-pilot` | `C:\INTERFRONT2026\investec-life-pilot` | Spring Boot backend, and the programmable card code under `card/` |
| `lifepilot-frontend` | `C:\INTERFRONT2026\lifepilot-frontend` | Vite + React + shadcn UI, talks to the backend over HTTP |

The backend serves on `8080` and the frontend dev server on `8081`, which is the
origin `CorsConfig` allows. Both defaulted to `8080` until September 2026, so
whichever process started first won and the other failed to bind.

## Current Work: Future You Bounty

The active goal is the Investec "Future You" Q3 2026 bounty challenge, which
runs 27 August to 30 September 2026. The brief asks for a tool that forecasts
future balances, spots recurring payments, highlights upcoming cashflow risks,
or answers "can I afford this?", built on the Investec API or programmable
cards.

All four are now covered, which shapes what is worth building next: the
differentiator is depth and demonstrability rather than breadth.

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

Not yet implemented on `main`:

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
- A mobile experience; the web frontend lives in `lifepilot-frontend`
- Financial-advice guardrails beyond the guardrail keyword scan and disclaimer wording

## In Flight On Branches

Work finished but not yet merged, as of 3 September 2026. Everything above
describes `main`; everything here is on a branch. Merge in this order, because
the forecast screen reads fields the survival branch adds.

| Branch | Repo | What it adds |
| --- | --- | --- |
| `docs/refresh-lifepilot-context` | backend | This file |
| `feature/scenario-survival-contract` | backend | Survival fields on the scenario response |
| `feature/programmable-card-guardrail` | backend | Card authorisation guardrail under `card/` |
| `feature/balance-forecast-screen` | frontend | Forecast screen, contract and port fixes |

### `feature/scenario-survival-contract`

Adds `SurvivalStatus` (AFFORDABLE / TIGHT / UNAFFORDABLE) and five fields to
`LifePilotScenarioResponse`: `survivalStatus`, `monthlyBufferAfterScenario`,
`monthlyShortfall`, `safeToSpendDropPercent`, `survivalMessage`. This closes the
survival budget impact calculation that sat at the top of Product Direction.

The frontend was already reading all five, so until this merges the results
panel renders an empty status badge and two blank money tiles. `HOME_RENOVATION`
joins `LifePilotScenarioType`; the dropdown already offered it, so choosing it
failed enum binding.

Buffer and shortfall are split so exactly one carries a figure.
`safeToSpendDropPercent` is null, not zero, when there is no positive
safe-to-spend to measure against. `LifePilotScenarioResponseJsonTest` pins the
JSON key set, because the drift that blanked the panel was invisible from both
sides.

### `feature/programmable-card-guardrail`

Card code that declines a swipe the forecast cannot afford, measured against
discretionary headroom rather than the balance. See `card/README.md`.

The shape is forced by a hard constraint: `beforeTransaction` gets two seconds
including the round trip, and a snapshot build measured 4.7 seconds locally even
with the Investec calls failing fast. So the card never computes. It reads a
cached snapshot in about 9ms, and `afterTransaction`, which has fifteen seconds,
rebuilds it.

Adds `CardGuardrailProperties`, `SpendingSnapshot`, `SpendingSnapshotService`,
`CardAuthorizationService`, `CardController`, `CardDecision`, `CardAssessment`,
and `card/main.js`.

Never verified inside the Investec sandbox. Hook names, the 2s/15s windows,
`process.env`, and the authorisation fields were taken from the Investec docs
and community repos rather than assumed, and the card code avoids optional
chaining and `AbortController` because that runtime could not be tested against.

### `feature/balance-forecast-screen` (frontend)

Adds the `/forecast` route: balance curve, shaded risk windows, KPI tiles,
recurring payment and income tables. Also moves the dev server to 8081 and
aligns the scenario types with the backend enum. Never rendered in a browser
during development, so the chart is unchecked for label collisions at long
horizons.

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

Card guardrail environment variables, on `feature/programmable-card-guardrail`:

- `LIFEPILOT_CARD_SHARED_SECRET` — required. The card endpoints return 503 while
  it is blank. They must be reachable from the Investec sandbox and they answer
  with balance data, so an unconfigured deployment refuses to serve rather than
  serving anyone.
- `LIFEPILOT_CARD_MONITOR_ONLY` — defaults to `true`. The guardrail reports its
  verdict without ever declining.
- `LIFEPILOT_CARD_EMERGENCY_BUFFER` — defaults to `500.00`.
- `LIFEPILOT_CARD_SNAPSHOT_TTL_SECONDS` — defaults to `300`.

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

Card endpoints, on `feature/programmable-card-guardrail`. Both require the
`X-LifePilot-Card-Key` header:

- `POST /api/lifepilot/cards/authorization` — answers from cache, does no
  outbound work. Called from `beforeTransaction`, which has a 2s budget.
- `POST /api/lifepilot/cards/accounts/{accountId}/snapshot` — rebuilds the
  snapshot. Slow. Called from `afterTransaction`, which has 15s.

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

Card authorisation, on `feature/programmable-card-guardrail`. Two flows, split
by how much time each hook has:

```text
beforeTransaction (2s)  -> CardController -> CardAuthorizationService -> cached SpendingSnapshot
                                          -> no outbound calls, ~9ms

afterTransaction (15s)  -> CardController -> SpendingSnapshotService -> BalanceForecastService
                                          -> Investec balance + 180 days of transactions, ~4.7s
```

There is no database. Coaching inputs are passed per request, and both the
knowledge store and the card snapshot cache live in memory for the life of the
process.

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

Run the frontend, from the `lifepilot-frontend` repo:

```powershell
npm run dev
```

It serves on `http://localhost:8081` and expects the backend on `8080`. Override
the API base with `VITE_LIFEPILOT_API_BASE`.

Without Investec credentials the app still starts. Forecast and card
authorisation catch the failed calls and answer with `fallbackUsed: true` and
zeroed figures rather than erroring; safe-to-spend, advice, and scenarios throw,
because they have no fallback path.

## Design Documents

- `docs/superpowers/specs/2026-05-03-ai-money-coach-advice-design.md`
- `docs/superpowers/plans/2026-05-03-ai-money-coach-advice.md`
- `docs/superpowers/specs/2026-05-22-lifepilot-v2-mysql-ai-design.md` (MySQL persistence and AI upgrades; not yet implemented)

## Product Direction

Before 30 September, in bounty-value order:

1. Add a demo mode backed by fixture data. Nothing here runs without Investec
   credentials, so nobody evaluating the submission can see it work.
   `BalanceForecastService` and `RecurringPaymentDetector` already accept
   injected transactions and an `asOf` date, so this is cheap.
2. Render the forecast screen in a browser and check the chart at a 365-day
   horizon. It has never been looked at.
3. Simulate the card code in the Investec sandbox.
4. Rewire scenarios onto `BalanceForecastService`, so a life event redraws the
   day-by-day curve instead of subtracting a flat monthly figure. Scenarios
   still go through `MoneyCoachService` and ignore the better engine.

Longer term:

5. Add validation for negative bill, savings, and scenario inputs —
   `LifePilotScenarioRequest` and `AdvancedSafeToSpendRequest` carry no constraints.
6. Add AI explanation support for scenario responses.
7. Replace query-parameter coaching inputs with a proper request model.
8. Add transaction categorization and monthly spend summaries, populating `SpendingCategory`.
9. Add goal and recurring bill models.
10. Add a persistence layer, per `2026-05-22-lifepilot-v2-mysql-ai-design.md`.
    This would also survive a restart, which the card snapshot cache currently
    does not.
11. Cache the Investec access token instead of fetching one per outbound call.

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
- The card guardrail authenticates with a single shared secret, which is
  proportionate for one user and nothing more. It also caches balances in
  memory, which a data retention model would have to account for.

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
- When changing a response DTO the frontend reads, change `lifepilot-frontend`
  in the same piece of work. Drift between the two is silent: the UI renders
  blanks rather than failing, which is how five fields went missing unnoticed.

Card guardrail rules, which are safety decisions rather than style:

- Every uncertain path approves. No snapshot, a stale one, a timeout, an HTTP
  error, an unrecognised decision value: all let the payment through. A
  guardrail that declines when it is unsure strands someone at a checkout,
  which is worse than the overdraft it was avoiding.
- Essential merchant categories are never declined, whatever the forecast says.
- Monitor mode stays the default. Declining a real transaction is not a
  behaviour to enable on someone's behalf.
- Never put work on the `beforeTransaction` path. Two seconds covers the round
  trip, so that endpoint answers from cache and makes no outbound calls.
