# Customer Support Resolution Agent

A full-stack AI agent that handles customer support requests (returns, billing disputes,
account issues) using Claude claude-sonnet-4-6, four MCP tools, and a real PostgreSQL backend.
Targets ≥ 80% first-contact resolution with a clean human escalation path.

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│  React UI  ·  localhost:3000                     │
│  ChatPanel │ ToolTrace │ EscalationBanner        │
└─────────────────┬───────────────────────────────┘
                  │  HTTP  (Vite proxy)
┌─────────────────▼───────────────────────────────┐
│  Spring Boot Agent  ·  localhost:8080  (Gradle)  │
│                                                  │
│  AgentController                                 │
│       │                                          │
│  AgentOrchestrator  ←──  ReAct loop              │
│       ├── ClaudeApiClient  →  Anthropic API      │
│       ├── ContextManager   (session history)     │
│       ├── PolicyEngine     (tool ordering rules) │
│       └── McpToolRegistry                        │
│             ├── get_customer    (query)           │
│             ├── lookup_order    (query)           │
│             ├── process_refund  (mutation, @Tx)  │
│             └── escalate_to_human (action, @Tx)  │
└─────────────────┬───────────────────────────────┘
                  │  JPA / Flyway
┌─────────────────▼───────────────────────────────┐
│  PostgreSQL  ·  localhost:5432  /  supportdb     │
│  customers │ orders │ escalations                │
└─────────────────────────────────────────────────┘
```

---

## Quick start (macOS)

### Step 1 — Run Ollama locally 

#### should have prior setup for ollama

```bash
ollama launch claude --model glm-5:cloud
```


### Step 2 — Start Database

```bash
docker-compose up
```


### Step 3 — Start backend 

```bash
cd backend

```


Open **http://localhost:3000**

---

## Manual startup (three terminals)

If you prefer to control each service separately:

```bash
# Terminal 1 — PostgreSQL
brew services start postgresql@16

# Terminal 2 — Backend
cd backend
export ANTHROPIC_API_KEY=sk-ant-...
./gradlew bootRun

# Terminal 3 — Frontend
cd frontend
npm run dev
```

---

## Test scenarios

| # | Message to type | Expected behaviour |
|---|-----------------|-------------------|
| A | `Hi, I'm Alice (C001). Refund order ORD-101.` | Auto-resolved — refund issued, DB updated |
| B | `I'm Bob Patel, C002. Return ORD-102.` | Escalated — 45-day-old order |
| C | `Refund ORD-103 for Carol.` | Denied — already refunded |
| D | `I have a billing problem.` | Agent asks clarifying questions |

### Verify in PostgreSQL

```bash
psql -U support_user -d supportdb

-- After scenario A
SELECT id, refunded FROM orders WHERE id = 'ORD-101';
-- refunded = true

-- After scenario B
SELECT * FROM escalations;
-- one row with reason and context_json

-- Reset seed data (re-run migration)
DELETE FROM escalations;
UPDATE orders SET refunded = false;
```

---

## Useful Gradle commands

| Command | Description |
|---------|-------------|
| `./gradlew bootRun` | Start the backend |
| `./gradlew test` | Run tests (H2, no PostgreSQL needed) |
| `./gradlew build` | Compile + test + build JAR |
| `./gradlew bootJar` | Build executable JAR |
| `./gradlew clean` | Delete build directory |
| `./gradlew dependencies` | Show dependency tree |

---

## Project layout

```
support-agent/
├── setup.sh              ← First-time setup (run once)
├── start.sh              ← Start all three services
├── README.md
│
├── backend/              ← Spring Boot + Gradle
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradlew
│   └── src/
│       ├── main/java/com/support/agent/
│       │   ├── AgentApplication.java
│       │   ├── config/CorsConfig.java
│       │   ├── controller/AgentController.java
│       │   ├── service/
│       │   │   ├── AgentOrchestrator.java
│       │   │   ├── ClaudeApiClient.java
│       │   │   ├── ContextManager.java
│       │   │   └── PolicyEngine.java
│       │   ├── mcp/
│       │   │   ├── McpTool.java
│       │   │   ├── McpToolRegistry.java
│       │   │   ├── GetCustomerTool.java
│       │   │   ├── LookupOrderTool.java
│       │   │   ├── ProcessRefundTool.java
│       │   │   └── EscalateToHumanTool.java
│       │   ├── model/
│       │   └── repository/
│       └── resources/
│           ├── application.properties
│           ├── application-test.properties
│           └── db/migration/
│               ├── V1__create_tables.sql
│               └── V2__seed_data.sql
│
└── frontend/             ← React + Vite
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── api/agentApi.js
        ├── hooks/useAgent.js
        ├── components/
        │   ├── Header.jsx
        │   ├── BackendAlert.jsx
        │   ├── ChatPanel.jsx
        │   ├── MessageBubble.jsx
        │   ├── TypingIndicator.jsx
        │   ├── EscalationBanner.jsx
        │   ├── SidePanel.jsx
        │   ├── SessionInfo.jsx
        │   └── ToolTrace.jsx
        ├── styles/global.css
        └── App.jsx
```

---

## API reference

### POST `/api/agent/chat`

```json
// Request
{ "sessionId": "uuid", "message": "I need a refund on ORD-101" }

// Response
{
  "reply": "I've processed your refund. Confirmation: REF-AB12CD34.",
  "toolTrace": [
    { "tool": "get_customer",  "args": { "customer_id": "C001" }, "result": {...} },
    { "tool": "lookup_order",  "args": { "order_id": "ORD-101" }, "result": {...} },
    { "tool": "process_refund","args": {...},                      "result": { "status": "refunded", ... } }
  ],
  "escalated": false
}
```

### DELETE `/api/agent/session/{sessionId}`
Clears conversation history and policy state. Called by the "New session" button.

### GET `/api/agent/health`
Returns `{ "status": "ok" }` — used by the frontend status dot.

---

## Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `ANTHROPIC_API_KEY` | Yes | Your Anthropic API key (`sk-ant-...`) |

Database credentials are in `backend/src/main/resources/application.properties`.
Change them there and in your PostgreSQL setup if needed.
