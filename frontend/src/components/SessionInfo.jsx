export default function SessionInfo({ sessionId, escalated }) {
  return (
    <div style={styles.container}>
      <p style={styles.sectionLabel}>Session</p>

      <div style={styles.row}>
        <span style={styles.key}>ID</span>
        <code style={styles.value}>{sessionId.substring(0, 16)}…</code>
      </div>

      <div style={styles.row}>
        <span style={styles.key}>Status</span>
        <span
          style={{
            ...styles.badge,
            background: escalated ? 'var(--amber-50)'  : 'var(--teal-50)',
            color:      escalated ? 'var(--amber-800)' : 'var(--teal-800)',
            border:     `1px solid ${escalated ? '#EF9F27' : 'var(--teal-400)'}`,
          }}
        >
          {escalated ? 'Escalated' : 'Active'}
        </span>
      </div>

      <div style={styles.row}>
        <span style={styles.key}>Backend</span>
        <span style={styles.value}>localhost:8080</span>
      </div>

      <div style={styles.row}>
        <span style={styles.key}>Model</span>
        <code style={styles.value}>claude-sonnet-4-6</code>
      </div>
    </div>
  )
}

const styles = {
  container: {
    padding: '16px',
  },
  sectionLabel: {
    fontSize:      '10px',
    fontWeight:    600,
    textTransform: 'uppercase',
    letterSpacing: '0.06em',
    color:         'var(--text-muted)',
    marginBottom:  '10px',
  },
  row: {
    display:        'flex',
    justifyContent: 'space-between',
    alignItems:     'center',
    padding:        '5px 0',
    borderBottom:   '1px solid var(--border)',
  },
  key: {
    fontSize: '12px',
    color:    'var(--text-secondary)',
  },
  value: {
    fontSize:     '11px',
    color:        'var(--text-primary)',
    fontFamily:   'var(--font-mono)',
    background:   'var(--surface-2)',
    padding:      '1px 5px',
    borderRadius: '4px',
  },
  badge: {
    fontSize:     '11px',
    fontWeight:   500,
    padding:      '2px 8px',
    borderRadius: '99px',
  },
}
