import { useRef, useEffect, useState } from 'react'
import MessageBubble from './MessageBubble'
import EscalationBanner from './EscalationBanner'
import TypingIndicator from './TypingIndicator'

const SUGGESTED = [
  "Hi, I'm Alice (C001). Refund order ORD-101 please.",
  "I'm Bob Patel, C002. I want to return order ORD-102.",
  "I have a billing issue with a recent charge.",
  "Refund ORD-103 for Carol.",
]

export default function ChatPanel({ messages, loading, escalated, onSend }) {
  const [input, setInput] = useState('')
  const bottomRef = useRef(null)
  const inputRef  = useRef(null)

  // Auto-scroll to newest message
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  function handleSubmit(e) {
    e?.preventDefault()
    if (!input.trim() || loading) return
    onSend(input)
    setInput('')
    inputRef.current?.focus()
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSubmit()
    }
  }

  const isEmpty = messages.length === 0

  return (
    <div style={styles.panel}>
      {/* Message list */}
      <div style={styles.messageList}>
        {isEmpty && !loading && <EmptyState onSuggest={onSend} />}

        {messages.map(msg => (
          <MessageBubble key={msg.id} message={msg} />
        ))}

        {loading && <TypingIndicator />}

        {escalated && !loading && <EscalationBanner />}

        <div ref={bottomRef} />
      </div>

      {/* Suggestions (only shown when chat is empty) */}
      {isEmpty && !loading && (
        <div style={styles.suggestions}>
          {SUGGESTED.map(s => (
            <button
              key={s}
              style={styles.suggestion}
              onClick={() => onSend(s)}
            >
              {s}
            </button>
          ))}
        </div>
      )}

      {/* Input bar */}
      <form style={styles.inputBar} onSubmit={handleSubmit}>
        <textarea
          ref={inputRef}
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Describe your issue..."
          rows={1}
          style={styles.textarea}
          disabled={loading}
        />
        <button
          type="submit"
          style={{
            ...styles.sendBtn,
            opacity: (!input.trim() || loading) ? 0.4 : 1,
          }}
          disabled={!input.trim() || loading}
        >
          Send
        </button>
      </form>
    </div>
  )
}

function EmptyState() {
  return (
    <div style={styles.empty}>
      <div style={styles.emptyIcon}>◈</div>
      <p style={styles.emptyTitle}>Customer Support Agent</p>
      <p style={styles.emptySub}>
        Powered by Claude. Ask about a return, billing dispute, or account issue.
      </p>
    </div>
  )
}

const styles = {
  panel: {
    flex:          2,
    display:       'flex',
    flexDirection: 'column',
    background:    'var(--surface)',
    overflow:      'hidden',
    minWidth:      0,
  },
  messageList: {
    flex:       1,
    overflowY:  'auto',
    padding:    '24px 28px',
    display:    'flex',
    flexDirection: 'column',
    gap:        '4px',
  },
  inputBar: {
    display:      'flex',
    alignItems:   'flex-end',
    gap:          '10px',
    padding:      '14px 20px',
    borderTop:    '1px solid var(--border)',
    background:   'var(--surface)',
    flexShrink:   0,
  },
  textarea: {
    flex:         1,
    resize:       'none',
    border:       '1px solid var(--border-mid)',
    borderRadius: 'var(--radius-md)',
    padding:      '10px 14px',
    fontSize:     '14px',
    lineHeight:   '1.5',
    outline:      'none',
    background:   'var(--surface-2)',
    color:        'var(--text-primary)',
    maxHeight:    '140px',
    overflowY:    'auto',
  },
  sendBtn: {
    padding:      '10px 18px',
    background:   'var(--purple-600)',
    color:        '#fff',
    border:       'none',
    borderRadius: 'var(--radius-md)',
    fontSize:     '14px',
    fontWeight:   500,
    transition:   'opacity 0.15s, background 0.15s',
    flexShrink:   0,
  },
  suggestions: {
    display:    'flex',
    flexWrap:   'wrap',
    gap:        '8px',
    padding:    '0 28px 16px',
  },
  suggestion: {
    fontSize:     '12px',
    color:        'var(--purple-800)',
    background:   'var(--purple-50)',
    border:       '1px solid var(--purple-100)',
    borderRadius: 'var(--radius-sm)',
    padding:      '5px 10px',
    cursor:       'pointer',
    textAlign:    'left',
    lineHeight:   1.4,
    transition:   'background 0.15s',
  },
  empty: {
    display:        'flex',
    flexDirection:  'column',
    alignItems:     'center',
    justifyContent: 'center',
    flex:           1,
    textAlign:      'center',
    padding:        '60px 40px',
    gap:            '10px',
    color:          'var(--text-muted)',
  },
  emptyIcon: {
    fontSize: '32px',
    color:    'var(--purple-400)',
    marginBottom: '6px',
  },
  emptyTitle: {
    fontSize:   '16px',
    fontWeight: 600,
    color:      'var(--text-secondary)',
  },
  emptySub: {
    fontSize:  '13px',
    maxWidth:  '320px',
    color:     'var(--text-muted)',
    lineHeight: 1.6,
  },
}
