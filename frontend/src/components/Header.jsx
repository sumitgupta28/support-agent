export default function Header({ sessionId, onReset, backendOk }) {
  return (
    <header style={styles.header}>
      <div style={styles.left}>
        <div style={styles.logo}>
          <span style={styles.logoIcon}>◈</span>
          <span style={styles.logoText}>Support Agent</span>
        </div>
        <span style={styles.model}>claude-sonnet-4-6</span>
      </div>

      <div style={styles.right}>
        {/* Backend status dot */}
        <div style={styles.statusRow}>
          <span
            style={{
              ...styles.dot,
              background: backendOk ? '#1D9E75' : '#E24B4A',
            }}
          />
          <span style={styles.statusLabel}>
            {backendOk ? 'Backend connected' : 'Backend offline'}
          </span>
        </div>

        {/* Session ID (truncated) */}
        <code style={styles.session}>
          {sessionId.substring(0, 8)}
        </code>

        <button style={styles.resetBtn} onClick={onReset} title="Start a new session">
          ↺ New session
        </button>
      </div>
    </header>
  )
}

const styles = {
  header: {
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'space-between',
    padding:        '0 20px',
    height:         '52px',
    background:     'var(--surface)',
    borderBottom:   '1px solid var(--border)',
    flexShrink:     0,
  },
  left: {
    display:    'flex',
    alignItems: 'center',
    gap:        '12px',
  },
  logo: {
    display:    'flex',
    alignItems: 'center',
    gap:        '7px',
  },
  logoIcon: {
    fontSize:   '18px',
    color:      'var(--purple-600)',
    lineHeight: 1,
  },
  logoText: {
    fontWeight: 600,
    fontSize:   '15px',
    color:      'var(--purple-800)',
    letterSpacing: '-0.01em',
  },
  model: {
    fontSize:        '11px',
    color:           'var(--purple-600)',
    background:      'var(--purple-50)',
    border:          '1px solid var(--purple-100)',
    borderRadius:    '99px',
    padding:         '2px 8px',
    fontFamily:      'var(--font-mono)',
  },
  right: {
    display:    'flex',
    alignItems: 'center',
    gap:        '14px',
  },
  statusRow: {
    display:    'flex',
    alignItems: 'center',
    gap:        '6px',
  },
  dot: {
    width:        '7px',
    height:       '7px',
    borderRadius: '50%',
    flexShrink:   0,
  },
  statusLabel: {
    fontSize: '12px',
    color:    'var(--text-secondary)',
  },
  session: {
    fontSize:     '11px',
    color:        'var(--text-muted)',
    background:   'var(--surface-2)',
    padding:      '2px 7px',
    borderRadius: 'var(--radius-sm)',
    border:       '1px solid var(--border)',
  },
  resetBtn: {
    fontSize:     '12px',
    fontWeight:   500,
    color:        'var(--text-secondary)',
    background:   'transparent',
    border:       '1px solid var(--border-mid)',
    borderRadius: 'var(--radius-sm)',
    padding:      '5px 10px',
    cursor:       'pointer',
    transition:   'background 0.15s',
  },
}
