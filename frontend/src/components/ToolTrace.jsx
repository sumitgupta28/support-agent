import { useState } from 'react'

const TOOL_META = {
  get_customer:      { color: 'var(--purple-600)', bg: 'var(--purple-50)', border: 'var(--purple-100)', label: 'Query' },
  lookup_order:      { color: 'var(--purple-600)', bg: 'var(--purple-50)', border: 'var(--purple-100)', label: 'Query' },
  process_refund:    { color: 'var(--teal-600)',   bg: 'var(--teal-50)',   border: 'var(--teal-400)',   label: 'Mutation' },
  escalate_to_human: { color: 'var(--amber-600)',  bg: 'var(--amber-50)', border: '#EF9F27',            label: 'Action' },
}

function prettyJson(value) {
  try {
    return JSON.stringify(value, null, 2)
  } catch {
    return String(value)
  }
}

function ToolCard({ call, index }) {
  const [open, setOpen] = useState(index === 0)
  const meta = TOOL_META[call.tool] || {
    color: 'var(--gray-600)', bg: 'var(--gray-50)',
    border: 'var(--gray-100)', label: 'Tool',
  }

  return (
    <div style={styles.card}>
      {/* Card header — click to toggle */}
      <button style={styles.cardHeader} onClick={() => setOpen(o => !o)}>
        <div style={styles.headerLeft}>
          <span
            style={{
              ...styles.typeBadge,
              color:      meta.color,
              background: meta.bg,
              border:     `1px solid ${meta.border}`,
            }}
          >
            {meta.label}
          </span>
          <code style={styles.toolName}>{call.tool}</code>
        </div>
        <span style={{ ...styles.chevron, transform: open ? 'rotate(90deg)' : 'none' }}>
          ›
        </span>
      </button>

      {/* Expanded detail */}
      {open && (
        <div style={styles.cardBody}>
          <p style={styles.fieldLabel}>Input</p>
          <pre style={styles.pre}>{prettyJson(call.args)}</pre>

          <p style={styles.fieldLabel}>Result</p>
          <pre
            style={{
              ...styles.pre,
              background: call.result?.status === 'denied'
                ? 'var(--red-50)'
                : call.result?.status === 'refunded'
                ? 'var(--teal-50)'
                : 'var(--surface-2)',
            }}
          >
            {prettyJson(call.result)}
          </pre>
        </div>
      )}
    </div>
  )
}

export default function ToolTrace({ calls }) {
  return (
    <div style={styles.container}>
      <p style={styles.sectionLabel}>
        MCP tool calls
        {calls.length > 0 && (
          <span style={styles.count}>{calls.length}</span>
        )}
      </p>

      {calls.length === 0 ? (
        <p style={styles.empty}>No tool calls yet. Send a message to start.</p>
      ) : (
        <div style={styles.list}>
          {calls.map((call, i) => (
            <ToolCard key={i} call={call} index={i} />
          ))}
        </div>
      )}
    </div>
  )
}

const styles = {
  container: {
    padding: '16px',
    flex: 1,
  },
  sectionLabel: {
    fontSize:      '10px',
    fontWeight:    600,
    textTransform: 'uppercase',
    letterSpacing: '0.06em',
    color:         'var(--text-muted)',
    marginBottom:  '10px',
    display:       'flex',
    alignItems:    'center',
    gap:           '6px',
  },
  count: {
    fontSize:     '10px',
    fontWeight:   600,
    background:   'var(--purple-100)',
    color:        'var(--purple-800)',
    padding:      '1px 6px',
    borderRadius: '99px',
  },
  empty: {
    fontSize:   '12px',
    color:      'var(--text-muted)',
    lineHeight: 1.5,
    fontStyle:  'italic',
  },
  list: {
    display:       'flex',
    flexDirection: 'column',
    gap:           '8px',
  },
  card: {
    border:       '1px solid var(--border)',
    borderRadius: 'var(--radius-md)',
    overflow:     'hidden',
    background:   'var(--surface)',
  },
  cardHeader: {
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'space-between',
    width:          '100%',
    padding:        '9px 12px',
    background:     'var(--surface-2)',
    border:         'none',
    cursor:         'pointer',
    textAlign:      'left',
  },
  headerLeft: {
    display:    'flex',
    alignItems: 'center',
    gap:        '8px',
  },
  typeBadge: {
    fontSize:     '10px',
    fontWeight:   600,
    padding:      '2px 6px',
    borderRadius: '99px',
  },
  toolName: {
    fontSize:   '12px',
    fontWeight: 500,
    color:      'var(--text-primary)',
    fontFamily: 'var(--font-mono)',
  },
  chevron: {
    fontSize:   '16px',
    color:      'var(--text-muted)',
    transition: 'transform 0.15s',
    lineHeight: 1,
  },
  cardBody: {
    padding:    '10px 12px',
    borderTop:  '1px solid var(--border)',
    display:    'flex',
    flexDirection: 'column',
    gap:        '6px',
  },
  fieldLabel: {
    fontSize:      '10px',
    fontWeight:    600,
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    color:         'var(--text-muted)',
    margin:        0,
  },
  pre: {
    fontFamily:   'var(--font-mono)',
    fontSize:     '11px',
    color:        'var(--text-primary)',
    background:   'var(--surface-2)',
    borderRadius: 'var(--radius-sm)',
    padding:      '8px 10px',
    overflowX:    'auto',
    whiteSpace:   'pre-wrap',
    wordBreak:    'break-all',
    margin:       0,
    lineHeight:   1.5,
  },
}
