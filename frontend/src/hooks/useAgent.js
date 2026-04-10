import { useState, useCallback, useRef } from 'react'
import { v4 as uuid } from 'uuid'
import { sendMessage, clearSession, checkHealth } from '../api/agentApi'

/**
 * Encapsulates all agent session state.
 * Components stay declarative — they only receive data + callbacks.
 */
export function useAgent() {
  // Stable session ID for the lifetime of this browser tab
  const sessionId = useRef(uuid()).current

  const [messages,  setMessages]  = useState([])   // { id, role, text, timestamp }
  const [toolTrace, setToolTrace] = useState([])   // [{ tool, args, result }]
  const [loading,   setLoading]   = useState(false)
  const [escalated, setEscalated] = useState(false)
  const [backendOk, setBackendOk] = useState(true)

  // ── Send a message ──────────────────────────────────────────────────────
  const send = useCallback(async (text) => {
    if (!text.trim() || loading) return

    const userMsg = {
      id:        uuid(),
      role:      'user',
      text:      text.trim(),
      timestamp: new Date(),
    }
    setMessages(prev => [...prev, userMsg])
    setLoading(true)
    setToolTrace([])

    try {
      const response = await sendMessage(sessionId, text.trim())

      const agentMsg = {
        id:        uuid(),
        role:      'agent',
        text:      response.reply,
        escalated: response.escalated,
        timestamp: new Date(),
      }
      setMessages(prev => [...prev, agentMsg])
      setToolTrace(response.toolTrace || [])
      setEscalated(response.escalated || false)
      setBackendOk(true)

    } catch (err) {
      const errorMsg = {
        id:        uuid(),
        role:      'error',
        text:      err.response?.data?.message
                   || 'Could not reach the backend. Is the Spring Boot app running on port 8080?',
        timestamp: new Date(),
      }
      setMessages(prev => [...prev, errorMsg])
      setBackendOk(false)
    } finally {
      setLoading(false)
    }
  }, [sessionId, loading])

  // ── Reset session ───────────────────────────────────────────────────────
  const reset = useCallback(async () => {
    try { await clearSession(sessionId) } catch { /* best-effort */ }
    setMessages([])
    setToolTrace([])
    setEscalated(false)
    setLoading(false)
  }, [sessionId])

  // ── Health check ────────────────────────────────────────────────────────
  const pingBackend = useCallback(async () => {
    const ok = await checkHealth()
    setBackendOk(ok)
    return ok
  }, [])

  return {
    sessionId,
    messages,
    toolTrace,
    loading,
    escalated,
    backendOk,
    send,
    reset,
    pingBackend,
  }
}
