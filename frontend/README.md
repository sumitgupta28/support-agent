# Support Agent — React Frontend

React + Vite UI for the customer support resolution agent.

## Stack

- **React 18** — UI
- **Vite 5** — dev server + build tool
- **Axios** — HTTP client
- **uuid** — stable session IDs

## Project structure

```
src/
├── api/
│   └── agentApi.js          ← All HTTP calls to Spring Boot
├── hooks/
│   └── useAgent.js          ← All state + session logic
├── components/
│   ├── Header.jsx            ← App bar with status + reset
│   ├── BackendAlert.jsx      ← Warning banner when backend is down
│   ├── ChatPanel.jsx         ← Message list + input bar
│   ├── MessageBubble.jsx     ← User / agent / error bubbles
│   ├── TypingIndicator.jsx   ← Animated dots while agent thinks
│   ├── EscalationBanner.jsx  ← Shown when case is escalated
│   ├── SidePanel.jsx         ← Right column wrapper
│   ├── SessionInfo.jsx       ← Session ID, status, model
│   └── ToolTrace.jsx         ← Live MCP call log (collapsible)
├── styles/
│   └── global.css            ← CSS variables + resets
├── App.jsx                   ← Root layout (two-panel)
└── main.jsx                  ← React entry point
```

## Running locally

The backend must be running on port 8080 first:

```bash
# Terminal 1 — backend
cd ../backend
./gradlew bootRun

# Terminal 2 — frontend
npm install
npm run dev
```

Open **http://localhost:3000**

Vite proxies `/api` → `http://localhost:8080` so no CORS issues in dev.

## Test scenarios (try these in the chat)

| Message | Expected |
|---------|----------|
| `Hi, I'm Alice (C001). Refund order ORD-101.` | Auto-resolved — refund issued |
| `I'm Bob, C002. Return ORD-102.` | Escalated — order too old |
| `Refund ORD-103 for Carol.` | Denied — already refunded |
| `I have a billing problem.` | Agent asks clarifying questions |

## Build for production

```bash
npm run build
# Output in dist/ — serve with any static host
```
