function formatTime(date) {
  return new Date(date).toLocaleTimeString([], {
    hour: '2-digit', minute: '2-digit',
  })
}

export default function MessageBubble({ message }) {
  const { role, text, escalated, timestamp } = message

  if (role === 'user') {
    return (
      <div style={styles.userRow}>
        <div style={styles.userBubble}>
          <p style={styles.userText}>{text}</p>
        </div>
        <span style={styles.timestamp}>{formatTime(timestamp)}</span>
      </div>
    )
  }

  if (role === 'error') {
    return (
      <div style={styles.errorRow}>
        <div style={styles.errorBubble}>
          <span style={styles.errorIcon}>▲</span>
          <p style={styles.errorText}>{text}</p>
        </div>
      </div>
    )
  }

  // Agent message
  return (
    <div style={styles.agentRow}>
      <div style={styles.agentAvatar}>◈</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', maxWidth: '75%' }}>
        <div
          style={{
            ...styles.agentBubble,
            borderColor: escalated
              ? '#EF9F27'
              : 'var(--border)',
            background: escalated
              ? 'var(--amber-50)'
              : 'var(--surface-2)',
          }}
        >
          {text.split('\n').map((line, i) => (
            <p key={i} style={styles.agentText}>{line || <br />}</p>
          ))}
        </div>
        <span style={styles.timestamp}>{formatTime(timestamp)}</span>
      </div>
    </div>
  )
}

const styles = {
  // User
  userRow: {
    display:        'flex',
    flexDirection:  'column',
    alignItems:     'flex-end',
    gap:            '4px',
    marginBottom:   '12px',
  },
  userBubble: {
    background:   'var(--purple-600)',
    borderRadius: '14px 14px 4px 14px',
    padding:      '10px 14px',
    maxWidth:     '72%',
  },
  userText: {
    color:      '#fff',
    fontSize:   '14px',
    lineHeight: 1.5,
    margin:     0,
  },

  // Agent
  agentRow: {
    display:      'flex',
    alignItems:   'flex-start',
    gap:          '10px',
    marginBottom: '12px',
  },
  agentAvatar: {
    width:          '30px',
    height:         '30px',
    borderRadius:   '50%',
    background:     'var(--purple-50)',
    border:         '1px solid var(--purple-100)',
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
    fontSize:       '14px',
    color:          'var(--purple-600)',
    flexShrink:     0,
    marginTop:      '2px',
  },
  agentBubble: {
    border:       '1px solid',
    borderRadius: '4px 14px 14px 14px',
    padding:      '10px 14px',
    display:      'flex',
    flexDirection: 'column',
    gap:          '4px',
  },
  agentText: {
    fontSize:   '14px',
    lineHeight: 1.6,
    color:      'var(--text-primary)',
    margin:     0,
  },

  // Error
  errorRow: {
    display:      'flex',
    marginBottom: '12px',
  },
  errorBubble: {
    display:      'flex',
    alignItems:   'flex-start',
    gap:          '8px',
    background:   'var(--red-50)',
    border:       '1px solid var(--red-400)',
    borderRadius: 'var(--radius-md)',
    padding:      '10px 14px',
    maxWidth:     '80%',
  },
  errorIcon: {
    color:      'var(--red-400)',
    fontSize:   '12px',
    marginTop:  '2px',
    flexShrink: 0,
  },
  errorText: {
    fontSize:   '13px',
    color:      'var(--red-600)',
    margin:     0,
    lineHeight: 1.5,
  },

  // Shared
  timestamp: {
    fontSize: '11px',
    color:    'var(--text-muted)',
  },
}
