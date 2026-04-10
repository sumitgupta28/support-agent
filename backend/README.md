# Customer Support Agent — Spring Boot + Gradle + PostgreSQL

A Spring Boot agent that uses the Claude API (claude-sonnet-4-6) with four MCP tools
to resolve customer support requests autonomously, targeting ≥80% first-contact resolution.

---

## Prerequisites (macOS)

```bash
brew install openjdk@21 node postgresql@16

echo 'export PATH="/opt/homebrew/opt/postgresql@16/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

java -version   # 21+
psql --version  # 16.x
node -v         # 18+
```

Set your Anthropic API key permanently:

```bash
echo 'export ANTHROPIC_API_KEY=sk-ant-your-key-here' >> ~/.zshrc
source ~/.zshrc
```

---

## PostgreSQL setup (one time)

```bash
brew services start postgresql@16
psql postgres
```

Inside psql:

```sql
CREATE USER support_user WITH PASSWORD 'support_pass';
CREATE DATABASE supportdb OWNER support_user;
GRANT ALL PRIVILEGES ON DATABASE supportdb TO support_user;
\q
```

Verify:

```bash
psql -U support_user -d supportdb
\dt   -- should show empty schema (Flyway creates tables on first run)
\q
```

---

## Running the backend

```bash
cd backend
./gradlew bootRun
```

Flyway automatically runs `V1__create_tables.sql` and `V2__seed_data.sql` on startup.

You should see:
```
Started AgentApplication on port 8080
```

### Useful Gradle tasks

| Task                      | Description                          |
|---------------------------|--------------------------------------|
| `./gradlew bootRun`       | Start the application                |
| `./gradlew test`          | Run tests (uses H2, no PostgreSQL needed) |
| `./gradlew build`         | Compile + test + produce JAR         |
| `./gradlew bootJar`       | Build executable JAR only            |
| `./gradlew dependencies`  | Show resolved dependency tree        |
| `./gradlew clean`         | Delete build/ directory              |

---

## Running the frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:3000

---

## Test scenarios

| # | Input                                                          | Expected outcome          |
|---|----------------------------------------------------------------|---------------------------|
| A | "Hi, I'm Alice, customer C001. Refund order ORD-101."          | Auto-resolved with refund |
| B | "I'm Bob Patel, C002. Refund ORD-102."                        | Escalated (45 days old)   |
| C | "Refund ORD-103 for Carol."                                    | Denied (already refunded) |
| D | "I have a billing problem."                                    | Clarifying questions asked |

### Verify in PostgreSQL after test A:

```sql
psql -U support_user -d supportdb
SELECT id, refunded FROM orders WHERE id = 'ORD-101';
-- refunded should be true

SELECT * FROM escalations;
-- one row after test B
```

---

## Project structure

```
backend/
├── build.gradle                         ← Gradle build (replaces pom.xml)
├── settings.gradle
├── gradlew / gradlew.bat                ← Gradle wrapper
├── gradle/wrapper/gradle-wrapper.properties
└── src/
    ├── main/
    │   ├── java/com/support/agent/
    │   │   ├── AgentApplication.java
    │   │   ├── config/CorsConfig.java
    │   │   ├── controller/AgentController.java
    │   │   ├── service/
    │   │   │   ├── AgentOrchestrator.java  ← ReAct loop
    │   │   │   ├── ClaudeApiClient.java    ← Anthropic API
    │   │   │   ├── ContextManager.java     ← Session history
    │   │   │   └── PolicyEngine.java       ← Tool ordering rules
    │   │   ├── mcp/
    │   │   │   ├── McpTool.java            ← Interface
    │   │   │   ├── McpToolRegistry.java    ← Auto-discovers tools
    │   │   │   ├── GetCustomerTool.java
    │   │   │   ├── LookupOrderTool.java
    │   │   │   ├── ProcessRefundTool.java
    │   │   │   └── EscalateToHumanTool.java
    │   │   ├── model/                      ← JPA entities + DTOs
    │   │   └── repository/                 ← Spring Data JPA
    │   └── resources/
    │       ├── application.properties
    │       ├── application-test.properties ← H2 for tests
    │       └── db/migration/
    │           ├── V1__create_tables.sql
    │           └── V2__seed_data.sql
    └── test/
        └── java/com/support/agent/
            └── AgentApplicationTests.java
```

---

## API endpoints

| Method | Path                         | Description            |
|--------|------------------------------|------------------------|
| POST   | `/api/agent/chat`            | Send a message         |
| DELETE | `/api/agent/session/{id}`    | Reset session state    |
| GET    | `/api/agent/health`          | Health check           |

### POST /api/agent/chat

Request:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "I'd like a refund on order ORD-101"
}
```

Response:
```json
{
  "reply": "I've processed your refund for ORD-101. Confirmation: REF-AB123456.",
  "toolTrace": [
    { "tool": "get_customer", "args": { "customer_id": "C001" }, "result": {...} },
    { "tool": "lookup_order", "args": { "order_id": "ORD-101" }, "result": {...} },
    { "tool": "process_refund", "args": { ... }, "result": { "status": "refunded", ... } }
  ],
  "escalated": false
}
```
