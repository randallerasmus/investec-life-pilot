## LifePilot v2 MySQL + AI Product Design

### Goal

Turn LifePilot from a simulator-centric prototype into a stronger AI product demo with:

- MySQL-backed persistence for knowledge and evaluation history
- A visible AI coach chat experience
- A knowledge management surface
- An evaluation report surface
- Existing deterministic financial calculations preserved as the source of truth

This milestone should improve the project's interview story for AI/backend roles without introducing unnecessary infrastructure such as auth, vector databases, or multi-service deployment.

### Product Shape

LifePilot v2 will expose four user-facing surfaces:

1. `Simulator`
   The current life-event simulator remains the first screen and continues to use deterministic affordability logic grounded in Investec account data.

2. `AI Coach`
   A chat-style experience where the user asks a question about affordability, spending pressure, or a planned purchase. The response is grounded in:
   - deterministic affordability calculations
   - retrieved knowledge documents
   - responsible AI guardrails

3. `Knowledge`
   A lightweight admin/search interface for financial guidance documents stored in MySQL. This supports the AI coach and makes the retrieval layer visible during demos.

4. `Evaluations`
   A report screen showing evaluation runs against canned prompts, including answer quality, grounding signals, warnings, and status.

### Architecture

The existing Spring Boot backend remains a single service. The v2 change is additive:

- keep Investec API integration in the current backend
- keep OpenAI integration optional and environment-driven
- keep deterministic safe-to-spend and scenario logic as the source of truth
- add MySQL persistence via Spring Data JPA
- extend the frontend to expose the existing and new AI capabilities

This keeps the architecture coherent for local development while making the backend more realistic and durable.

### Persistence Model

#### Knowledge Documents

Store knowledge documents in MySQL instead of memory.

Table: `knowledge_documents`

- `id`
- `title`
- `content`
- `source`
- `tags`
- `created_at`
- `updated_at`

Responsibilities:

- create documents
- list documents
- search documents
- provide retrieval input to the AI coach

The implementation should preserve the current simple retrieval behavior while moving storage to MySQL.

#### Evaluation Runs

Persist AI coaching/evaluation runs for history and report rendering.

Table: `evaluation_runs`

- `id`
- `account_id`
- `question`
- `ai_answer`
- `risk_level`
- `confidence_score`
- `status`
- `created_at`

Status values should support:

- `PASSED`
- `WARNING`
- `FAILED`
- `DETERMINISTIC_ONLY`

Table: `evaluation_run_retrievals`

- `id`
- `evaluation_run_id`
- `knowledge_document_id`
- `title_snapshot`
- `source_snapshot`
- `relevance_score`

Table: `evaluation_run_warnings`

- `id`
- `evaluation_run_id`
- `warning_text`

This schema supports both visible reports and future evaluation analytics without overcomplicating the system.

### Backend Behavior

#### KnowledgeRagService

Replace in-memory document storage with repository-backed persistence.

The retrieval algorithm for this milestone remains lexical and Java-side:

- tokenize the query
- score title/content/source matches
- sort by score
- return the top matches

This is intentionally not a vector search phase. The schema and service boundaries should leave room for embeddings later, but the current milestone should stay easy to run and debug.

#### AiCoachService

The AI coach flow remains:

1. compute deterministic affordability context
2. retrieve relevant knowledge documents
3. create a deterministic answer
4. optionally rewrite with OpenAI
5. run guardrails
6. persist the resulting evaluation run
7. return answer, confidence, knowledge evidence, and warnings

If OpenAI is unavailable, the service must still return a deterministic grounded answer and persist the run with `DETERMINISTIC_ONLY`.

If Investec data is unavailable, the response should clearly indicate degraded grounding rather than implying confidence it does not have.

#### Evaluation Service

Extend the current evaluation capabilities from static criteria to executable runs.

The service should:

- provide default evaluation scenarios/prompts
- trigger an AI coach run for a selected scenario
- classify the result into `PASSED`, `WARNING`, `FAILED`, or `DETERMINISTIC_ONLY`
- persist the run and related warnings/retrieval evidence
- return recent run history for the frontend report screen

For this milestone, evaluation classification can be rules-based rather than model-graded. The important thing is that the system stores and exposes evidence.

### Frontend Experience

The frontend should evolve from a single simulator page into a compact multi-surface product.

Recommended navigation:

- `Simulator`
- `AI Coach`
- `Knowledge`
- `Evaluations`

#### Simulator

Keep the current simulator intact with only minimal integration changes needed to fit the new navigation shell.

#### AI Coach

Add a chat-oriented screen with:

- account ID input or inherited account context
- question input
- answer panel
- confidence score
- warnings
- "based on" evidence
- retrieved knowledge snippets

The experience should feel product-ready rather than a raw JSON console.

#### Knowledge

Add a management surface with:

- list of existing documents
- create document form
- optional search/filter input

The goal is not full editorial tooling. It is a clean demo surface showing that the knowledge layer is real and persistent.

#### Evaluations

Add a report screen with:

- default evaluation prompts
- run-evaluation action
- table or cards for recent run history
- visible status badges
- warnings and evidence inspection

This should make the project's evaluation story concrete.

### Error Handling

The trust model should be explicit and user-visible.

- If Investec data fails, the system should explain that grounding is degraded.
- If OpenAI fails, the system should fall back gracefully to deterministic guidance.
- If knowledge retrieval returns nothing useful, the system should say so and continue with deterministic context.
- If MySQL is unavailable, backend startup should fail clearly rather than silently reverting to in-memory behavior.

### Testing Strategy

Testing should stay layered and focused.

Backend:

- repository tests for MySQL-backed entities/repositories
- service tests for knowledge persistence and retrieval behavior
- service tests for AI coach persistence and fallback status handling
- service tests for evaluation run creation and classification
- controller tests for new knowledge/evaluation endpoints

Frontend:

- tests for AI coach rendering and submission flow
- tests for knowledge create/list behavior
- tests for evaluation report rendering

This milestone does not require end-to-end browser automation unless the UI integration becomes unusually risky.

### Migration and Configuration

Add MySQL support through standard Spring Boot configuration.

Configuration should use environment variables for:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Local development target:

- database: `lifepilot`
- user: `lifepilot_user`

Schema management can start with JPA auto-update for local iteration if needed, but the code should be structured so Flyway can be added cleanly next.

### Out of Scope

The following are intentionally excluded from this milestone:

- authentication and authorization
- multi-user tenancy
- vector database or embeddings pipeline
- weekly emailed reports
- background job scheduling
- production deployment work

### Implementation Boundary

This milestone is complete when:

- knowledge documents are stored in MySQL
- AI coach runs are persisted in MySQL
- evaluation runs and their evidence/warnings are persisted in MySQL
- frontend exposes Simulator, AI Coach, Knowledge, and Evaluations
- backend fallback behavior is explicit and tested
- the app remains runnable locally with MySQL, Investec credentials, and optional OpenAI credentials
