# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Customer support agent using Claude (via Anthropic API) with a ReAct loop to handle customer inquiries. The system uses MCP-style tools for database operations and enforces policy rules before tool execution.

## Commands

### Backend (Spring Boot)
```bash
cd backend

# Run PostgreSQL via Docker (first time)
docker-compose -f ../docker-compose.yml up -d

# Run the application
./gradlew bootRun

# Run tests (uses H2 in-memory database)
./gradlew test

# Run a single test class
./gradlew test --tests "com.support.agent.AgentApplicationTests"

# Build JAR
./gradlew build
```

### Frontend (React + Vite)
```bash
cd frontend

# Install dependencies
npm install

# Run dev server (proxies /api to :8080)
npm run dev

# Build for production
npm run build

# Lint
npm run lint
```

## Architecture

### Backend (Spring Boot 3 / Java 21)

**Entry point:** `AgentApplication.java` → `AgentController.java`

**Core flow:**
1. `AgentOrchestrator` implements a ReAct loop: receives user message → calls Claude API → executes tools if needed → returns response
2. `ClaudeApiClient` makes HTTP calls to the Anthropic Messages API
3. `McpToolRegistry` auto-discovers all `McpTool` implementations via Spring DI
4. `PolicyEngine` enforces ordering rules (e.g., must call `get_customer` before `process_refund`)
5. `ContextManager` maintains per-session message history in Anthropic API format

**MCP Tools (`mcp/` package):**
- All tools implement `McpTool` interface (`name`, `description`, `inputSchema`, `execute`)
- Add a new tool by creating a `@Component` class implementing `McpTool` — no manual registration needed
- Tools: `GetCustomerTool`, `LookupOrderTool`, `ProcessRefundTool`, `EscalateToHumanTool`

**Database:**
- PostgreSQL for dev/prod, H2 for tests
- Flyway migrations in `resources/db/migration/`
- JPA entities: `Customer`, `Order`, `Escalation`

**Configuration:**
- `application.properties` — PostgreSQL + Anthropic API settings
- `application-test.properties` — H2 for tests, Flyway disabled

### Frontend (React 18 + Vite)

**Entry point:** `main.jsx` → `App.jsx`

**Key hook:** `useAgent.js` manages all session state (messages, tool traces, loading, escalation status)

**Components:**
- `ChatPanel.jsx` — Message list + input
- `MessageBubble.jsx` — User/agent/error message rendering
- `ToolTrace.jsx` — Collapsible MCP call log
- `EscalationBanner.jsx` — Shows when case is escalated

**API client:** `agentApi.js` — Axios calls to `/api/agent/*`

## Environment

The backend expects these environment variables (or application.properties defaults):
- `ANTHROPIC_API_KEY` — API key for Claude (defaults to "ollama" for local Ollama)
- `anthropic.api-url` — API endpoint (defaults to `http://localhost:11434/v1/messages` for Ollama)

## Policy Rules (PolicyEngine)

1. `get_customer` must be called before `lookup_order`, `process_refund`, or `escalate_to_human`
2. `lookup_order` must be called before `process_refund`
3. `process_refund` cannot be called twice in the same session (prevents duplicate refunds)

These are enforced server-side in `PolicyEngine.allow()` — the agent cannot bypass them even if it tries.