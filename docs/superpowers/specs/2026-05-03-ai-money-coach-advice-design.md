# AI Money Coach Advice Design

## Goal

Add an AI money coach advice endpoint that builds on the existing safe-to-spend calculation.

## Selected Approach

Use a hybrid design:

- Deterministic Java logic calculates the financial state, risk level, summary, and recommendations.
- Optional OpenAI integration can rewrite those deterministic insights into a friendlier coaching message.
- If OpenAI is not configured or fails, the API still returns the deterministic advice.

## Endpoint

`GET /api/coach/accounts/{accountId}/advice`

It accepts the same query parameters as `/api/coach/accounts/{accountId}/safe-to-spend`:

- `bondOrRent`
- `schoolFees`
- `insurance`
- `groceries`
- `fuel`
- `subscriptions`
- `otherBills`
- `goalSavingAmount`

## Response Shape

The response includes:

- `accountId`
- `availableBalance`
- `estimatedBills`
- `goalSavingAmount`
- `safeToSpend`
- `currency`
- `riskLevel`
- `summary`
- `recommendations`
- `aiGenerated`
- `disclaimer`

## Risk Levels

- `CRITICAL`: safe-to-spend is below zero.
- `TIGHT`: safe-to-spend is zero or less than 10% of available balance.
- `HEALTHY`: safe-to-spend is at least 10% of available balance.

## Guardrails

The service must not present itself as regulated financial advice. All output includes a disclaimer that this is educational budgeting guidance only.

The AI layer must be grounded only in deterministic facts supplied by the service.

