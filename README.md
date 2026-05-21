# LifePilot AI

LifePilot AI is a Java 17 / Spring Boot backend that uses Investec account data to produce deterministic budgeting insight, life-event simulations, grounded AI coaching, lightweight retrieval, evaluation scaffolding, and responsible AI guardrails.

It keeps the existing Money Coach MVP intact and extends it into a portfolio-ready fintech AI backend.

## What It Does

- Connects to the Investec Open API for accounts, balances, and transactions
- Calculates baseline safe-to-spend values
- Simulates life-event affordability scenarios
- Stores an in-memory financial knowledge base for simple RAG-style retrieval
- Produces grounded AI coaching answers using deterministic affordability logic plus retrieved knowledge
- Checks answers for risky financial claims and rewrites them into safer educational wording
- Exposes default evaluation scenarios for retrieval, groundedness, correctness, and guardrail quality

## New AI Modules

1. `KnowledgeRagService`
   - In-memory knowledge store
   - Add and list knowledge documents
   - Keyword-scored search with snippets and relevance scores
   - Structured so embeddings / pgvector can be added later

2. `AiCoachService`
   - Accepts account-aware coaching questions
   - Reuses Investec-backed balance and transaction data
   - Uses `SafeToSpendEngineService` and `KnowledgeRagService`
   - Applies `ResponsibleAiGuardrailsService` before returning the answer
   - Optionally rewrites deterministic coaching through the existing OpenAI advice client when configured

3. `SafeToSpendEngineService`
   - Advanced safe-to-spend logic
   - Considers balance, recent transactions, estimated recurring expenses, emergency buffer, payday, and planned purchase amount
   - Falls back safely when Investec data is unavailable in local development

4. `EvaluationService`
   - Returns default evaluation scenarios for common coaching questions
   - Includes criteria for retrieval quality, calculation correctness, groundedness, hallucination risk, and guardrail compliance

5. `ResponsibleAiGuardrailsService`
   - Detects risky language such as guaranteed returns, certainty claims, reckless loan advice, and unsupported financial instructions
   - Rewrites high-risk wording into educational guidance
   - Appends a financial-safety disclaimer

## Package Structure

Root package:

```text
za.co.byteservices.moneycoach
```

Key areas:

```text
controller/
  InvestecController
  MoneyCoachController
  LifePilotController
  LifePilotAiController

service/
  InvestecAuthService
  InvestecAccountService
  MoneyCoachService
  MoneyCoachAdviceService
  LifePilotScenarioService
  KnowledgeRagService
  AiCoachService
  SafeToSpendEngineService
  EvaluationService
  ResponsibleAiGuardrailsService
  OpenAiAdviceClient

dto/
  Existing Investec / coach / scenario DTOs
  New AI request and response DTOs

config/
  InvestecApiProperties
  OpenAiProperties
```

## REST Endpoints

Existing endpoints remain available:

```text
GET  /api/investec/config-check
GET  /api/investec/token-check
GET  /api/investec/accounts
GET  /api/investec/accounts/{accountId}/balance
GET  /api/investec/accounts/{accountId}/transactions?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD

GET  /api/coach/accounts/{accountId}/safe-to-spend
GET  /api/coach/accounts/{accountId}/advice

POST /api/lifepilot/scenarios
```

New LifePilot AI endpoints:

```text
GET  /api/lifepilot/knowledge/documents
POST /api/lifepilot/knowledge/documents
POST /api/lifepilot/knowledge/search
POST /api/lifepilot/safe-to-spend/accounts/{accountId}/advanced
POST /api/lifepilot/ai-coach/accounts/{accountId}/ask
POST /api/lifepilot/guardrails/check
GET  /api/lifepilot/evaluations/default
```

## Example Requests

### Add a knowledge document

```json
{
  "title": "Emergency Fund Basics",
  "content": "An emergency fund helps cover unexpected expenses without relying on debt.",
  "source": "internal-financial-education"
}
```

### Search the knowledge base

```json
{
  "question": "Why do I need an emergency fund?"
}
```

### Advanced safe-to-spend

```json
{
  "payday": "2026-05-31",
  "emergencyBuffer": 1000,
  "plannedPurchaseAmount": 2500,
  "plannedPurchaseDescription": "Golf clubs"
}
```

### Ask the AI coach

```json
{
  "question": "Can I afford to spend R2500 on golf clubs this month?",
  "payday": "2026-05-31",
  "emergencyBuffer": 1000
}
```

### Guardrail check

```json
{
  "answer": "You should invest all your money in one stock because it will definitely go up."
}
```

## Example Responses

### AI coach response shape

```json
{
  "answer": "For the question 'Can I afford to spend R2500 on golf clubs this month?', the purchase may be possible but the buffer is tight before payday. This is educational guidance based on available transaction data, not regulated financial advice.",
  "accountId": "account-id",
  "riskLevel": "TIGHT",
  "confidence": 82,
  "basedOn": [
    "Current balance 12500.00",
    "Estimated recurring expenses 7200.00",
    "Safe to spend before purchase 4300.00"
  ],
  "retrievedKnowledge": [
    {
      "title": "Emergency Fund Basics",
      "source": "internal-financial-education",
      "contentSnippet": "An emergency fund helps cover unexpected expenses...",
      "relevanceScore": 5
    }
  ],
  "guardrailWarnings": []
}
```

### Guardrail response shape

```json
{
  "warnings": [
    "Detected guaranteed-return or certainty language.",
    "Detected concentrated investment advice presented too strongly."
  ],
  "safeAnswer": "Based on the available account and transaction context, avoid treating this as a guaranteed outcome. This is educational guidance based on available transaction data, not regulated financial advice.",
  "rewritten": true
}
```

## Local Run

### Requirements

- Java 17
- Maven 3.9+ or the included Maven wrapper

### Environment variables

Required for live Investec calls:

```text
INVESTEC_CLIENT_ID
INVESTEC_CLIENT_SECRET
INVESTEC_API_KEY
```

Optional for OpenAI rewriting:

```text
OPENAI_API_KEY
OPENAI_MODEL
```

Configured properties in `src/main/resources/application.properties`:

```properties
spring.application.name=lifepilot
server.port=8080

investec.base-url=https://openapi.investec.com
investec.client-id=${INVESTEC_CLIENT_ID:}
investec.client-secret=${INVESTEC_CLIENT_SECRET:}
investec.api-key=${INVESTEC_API_KEY:}

openai.base-url=https://api.openai.com/v1
openai.api-key=${OPENAI_API_KEY:}
openai.model=${OPENAI_MODEL:}
```

### Run tests

```powershell
.\mvnw.cmd clean test
```

If you prefer system Maven:

```powershell
mvn clean test
```

### Run the app

```powershell
.\mvnw.cmd spring-boot:run
```

Default base URL:

```text
http://localhost:8080
```

## curl Examples

```bash
curl -X GET http://localhost:8080/api/lifepilot/knowledge/documents
```

```bash
curl -X POST http://localhost:8080/api/lifepilot/knowledge/documents \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Emergency Fund Basics\",\"content\":\"An emergency fund helps cover unexpected expenses...\",\"source\":\"internal-financial-education\"}"
```

```bash
curl -X POST http://localhost:8080/api/lifepilot/knowledge/search \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"Why do I need an emergency fund?\"}"
```

```bash
curl -X POST http://localhost:8080/api/lifepilot/safe-to-spend/accounts/acc-123/advanced \
  -H "Content-Type: application/json" \
  -d "{\"payday\":\"2026-05-31\",\"emergencyBuffer\":1000,\"plannedPurchaseAmount\":2500,\"plannedPurchaseDescription\":\"Golf clubs\"}"
```

```bash
curl -X POST http://localhost:8080/api/lifepilot/ai-coach/accounts/acc-123/ask \
  -H "Content-Type: application/json" \
  -d "{\"question\":\"Can I afford to spend R2500 on golf clubs this month?\",\"payday\":\"2026-05-31\",\"emergencyBuffer\":1000}"
```

```bash
curl -X POST http://localhost:8080/api/lifepilot/guardrails/check \
  -H "Content-Type: application/json" \
  -d "{\"answer\":\"You should invest all your money in one stock because it will definitely go up.\"}"
```

```bash
curl -X GET http://localhost:8080/api/lifepilot/evaluations/default
```

## Notes

- The knowledge base is intentionally in-memory so the project remains easy to run.
- The AI coach remains grounded in deterministic calculations and available transaction data.
- OpenAI is optional. If it is not configured or the call fails, the app falls back to local deterministic guidance.
- The project still does not provide regulated financial advice.

## Future Roadmap

- pgvector-backed knowledge storage
- Embeddings-based retrieval
- Reranking and hybrid retrieval
- Evaluation dashboard and regression suite
- Transaction categorization improvements
- React frontend for interactive coaching and life-event simulation
