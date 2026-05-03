# AI Money Coach Advice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a hybrid AI money coach advice endpoint backed by deterministic safe-to-spend insights.

**Architecture:** `MoneyCoachService` remains the source of truth for balances and safe-to-spend. A new `MoneyCoachAdviceService` converts `SafeToSpendResponse` into risk level, summary, and recommendations, then optionally asks an `AiAdviceClient` to rewrite the summary.

**Tech Stack:** Java 17, Spring Boot 4.0.6, Maven, JUnit 5, Spring Web MVC, `RestTemplate`.

---

### Task 1: Advice Model and Deterministic Service

**Files:**
- Create: `src/main/java/za/co/byteservices/moneycoach/dto/MoneyCoachAdviceResponse.java`
- Create: `src/main/java/za/co/byteservices/moneycoach/model/MoneyCoachRiskLevel.java`
- Create: `src/main/java/za/co/byteservices/moneycoach/service/MoneyCoachAdviceService.java`
- Test: `src/test/java/za/co/byteservices/moneycoach/service/MoneyCoachAdviceServiceTest.java`

- [ ] Write tests for `CRITICAL`, `TIGHT`, and `HEALTHY` advice.
- [ ] Run the focused test and confirm it fails because the service does not exist.
- [ ] Add the response DTO, risk enum, and deterministic advice service.
- [ ] Run the focused test and confirm it passes.

### Task 2: Optional OpenAI Advice Client

**Files:**
- Create: `src/main/java/za/co/byteservices/moneycoach/config/OpenAiProperties.java`
- Create: `src/main/java/za/co/byteservices/moneycoach/service/AiAdviceClient.java`
- Create: `src/main/java/za/co/byteservices/moneycoach/service/OpenAiAdviceClient.java`
- Modify: `src/main/resources/application.properties`
- Test: `src/test/java/za/co/byteservices/moneycoach/service/MoneyCoachAdviceServiceTest.java`

- [ ] Add a test proving deterministic advice is used when the AI client returns empty.
- [ ] Run the focused test and confirm the new behavior fails.
- [ ] Add optional OpenAI properties and a client wrapper using `/v1/responses`.
- [ ] Inject the optional client into `MoneyCoachAdviceService`.
- [ ] Run the focused test and confirm it passes.

### Task 3: Controller Endpoint

**Files:**
- Modify: `src/main/java/za/co/byteservices/moneycoach/controller/MoneyCoachController.java`
- Test: `src/test/java/za/co/byteservices/moneycoach/MoneyCoachApplicationTests.java`

- [ ] Add `GET /api/coach/accounts/{accountId}/advice` with the same query params as safe-to-spend.
- [ ] Run the full Maven test suite.

