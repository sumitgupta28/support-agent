import axios from 'axios'

// Vite proxy forwards /api → http://localhost:8080
const http = axios.create({
  baseURL: '/api/agent',
  headers: { 'Content-Type': 'application/json' },
  timeout: 60_000, // 60s — Claude can take time on multi-tool calls
})

/**
 * Send a user message and receive the agent reply + tool trace.
 * @returns {{ reply: string, toolTrace: ToolCall[], escalated: boolean }}
 */
export async function sendMessage(sessionId, message) {
  const { data } = await http.post('/chat', { sessionId, message })
  return data
}

/**
 * Clear session history and policy state on the backend.
 */
export async function clearSession(sessionId) {
  await http.delete(`/session/${sessionId}`)
}

/**
 * Simple health check — used to verify the backend is reachable.
 * @returns {boolean}
 */
export async function checkHealth() {
  try {
    await http.get('/health')
    return true
  } catch {
    return false
  }
}
