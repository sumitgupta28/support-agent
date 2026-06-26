# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Full-stack customer-support AI agent. A Spring Boot backend runs a ReAct loop against the
Anthropic Messages API (`claude-sonnet-4-6`), exposing four "MCP" tools that read and mutate a
PostgreSQL database. A React/Vite frontend renders the chat, a live tool-call trace, and an
escalation banner. The agent auto-resolves returns/refunds and escalates to a human when a case
is ambiguous or out of policy.

## Commands

### Backend (run from `backend/`)
- `./gradlew bootRun` — start backend on :8080 (needs `ANTHROPIC_API_KEY` env var + running PostgreSQL)
- `./gradlew test` — run all tests (uses H2 in-memory DB via the `test` profile; **no PostgreSQL or API key needed**)
- `./gradlew test --tests "AgentApplicationTests.refundPersistsToDatabase"` — run a single test
- `./gradlew build` — compile + test + assemble JAR
- `./gradlew clean` — delete build output

### Frontend (run from `frontend/`)
- `npm run dev` — Vite dev server on :3000 (proxies `/api` → :8080)
- `npm run build` — production build
- `npm run lint` — ESLint (`--max-warnings 0`)

### Full stack
- `./setup.sh` — one-time macOS setup: installs Java 21 / Node / PostgreSQL 16 via Homebrew, creates the `support_user`/`supportdb`, saves the API key to `~/.zshrc`, runs `npm install`.
- `./start.sh` — starts PostgreSQL, backend, and frontend together; requires `ANTHROPIC_API_KEY` already exported.
- `docker-compose up -d` — alternative: run just PostgreSQL 16 in a container (same `supportdb`/`support_user`/`support_pass` credentials).

## Architecture

Request flow: **React → `AgentController` (`/api/agent/chat`) → `AgentOrchestrator` → Anthropic API + MCP tools → PostgreSQL**, response carries `{ reply, toolTrace, escalated }`.

### The ReAct loop (`service/AgentOrchestrator.java`)
The heart of the system. Loops up to `agent.max-iterations` (default 6):
1. Calls Claude with the system prompt + full session history + all tool schemas.
2. If Claude returns `tool_use`: run it through the **PolicyEngine gate**, execute via the registry, append the result to history, and loop.
3. If Claude returns text: that's the final reply — return it.
4. If the iteration budget is exhausted: force-escalate.

The hard-coded `SYSTEM_PROMPT` encodes the agent's behavioral rules (verify customer first, look up order before refund, escalate when unsure or outside the 30-day window).

### Two layers of rule enforcement (important)
Rules are deliberately enforced in **two independent places** — do not collapse them:
- **`service/PolicyEngine.java`** — gates tool *ordering* per session before execution (e.g. `get_customer` must precede mutations; `lookup_order` must precede `process_refund`; no duplicate refund in a session). A violation short-circuits to escalation. State is in-memory, keyed by `sessionId`.
- **Tool-internal guardrails** — each mutating tool re-validates business rules against the DB even if the agent "already checked." See `ProcessRefundTool.execute()`: it re-verifies ownership, the 30-day window (`refundEligible`), and not-already-refunded server-side, because the agent's tool-result cache may be stale. Trust the database, not the model.

### MCP tools (`mcp/`)
Each tool implements the `McpTool` interface (`name`, `description`, `inputSchema`, `execute`). The `description`/`inputSchema` become the tool definition Claude sees; `execute` runs the actual logic.
- `McpToolRegistry` auto-discovers every `McpTool` Spring bean — **to add a tool, just create a new `@Component implements McpTool`; no manual registration.**
- Tools: `GetCustomerTool`, `LookupOrderTool` (reads), `ProcessRefundTool`, `EscalateToHumanTool` (mutations, `@Transactional`).

### Session state (`service/ContextManager.java`)
In-memory only (`ConcurrentHashMap` keyed by `sessionId`) — **state is lost on restart, not multi-instance safe.** Maintains two parallel structures:
- `histories` — messages in the exact Anthropic API shape (including paired `tool_use`/`tool_result` blocks).
- `toolTraces` — a UI-facing list of `{tool, args, result}` for the frontend trace panel.

### Anthropic client (`service/ClaudeApiClient.java`)
Thin hand-rolled OkHttp wrapper around `POST /v1/messages` (no SDK). Sets `anthropic-version: 2023-06-01`. Parses `stop_reason == "tool_use"` into a `ClaudeResponse.toolUse(...)`, otherwise concatenates text blocks. Model/key/max-tokens come from `application.properties`.

### Persistence
- JPA entities in `model/`; Spring Data repositories in `repository/`.
- **Flyway owns the schema** (`resources/db/migration/V1__create_tables.sql`, `V2__seed_data.sql`); Hibernate is `ddl-auto=validate` only — change schema via a new migration, never by editing entities alone.
- Tables: `customers`, `orders`, `escalations`.

### Frontend (`frontend/src/`)
- `hooks/useAgent.js` — central state hook: holds messages, generates the `sessionId` (uuid), drives loading/escalation state.
- `api/agentApi.js` — axios calls to `/api/agent/*`.
- Components are presentational: `ChatPanel`, `MessageBubble`, `ToolTrace` (renders the tool trace), `EscalationBanner`, `SidePanel`, `SessionInfo`, `BackendAlert`.

## Configuration notes
- `ANTHROPIC_API_KEY` is the only required env var; it's injected into `application.properties` as `anthropic.api.key`.
- Tunable knobs in `backend/src/main/resources/application.properties`: `anthropic.model`, `anthropic.max-tokens`, `agent.max-iterations`. DB credentials also live here.
- `build.gradle` deliberately pins `junit-jupiter.version=5.10.2` and omits `spring-boot-devtools` — these jars aren't in the local Gradle cache and downloads are blocked behind a corporate Zscaler proxy. Avoid bumping them unless the proxy situation changes.

## Manual test scenarios
Type these in the UI to exercise the main paths (seed data in `V2__seed_data.sql`):
- `Hi, I'm Alice (C001). Refund order ORD-101.` → auto-resolved (refund issued, DB updated).
- `I'm Bob Patel, C002. Return ORD-102.` → escalated (order outside the return window).
- `Refund ORD-103 for Carol.` → denied (already refunded).

To reset: `DELETE FROM escalations; UPDATE orders SET refunded = false;` in `psql -U support_user -d supportdb`.
