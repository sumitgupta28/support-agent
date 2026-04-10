import { useEffect, useState } from 'react'

const PHASES = [
  'Thinking...',
  'Calling tools...',
  'Reasoning...',
  'Finalising response...',
]

export default function TypingIndicator() {
  const [phase, setPhase] = useState(0)

  useEffect(() => {
    const interval = setInterval(() => {
      setPhase(p => (p + 1) % PHASES.length)
    }, 2200)
    return () => clearInterval(interval)
  }, [])

  return (
    <div style={styles.row}>
      <div style={styles.avatar}>◈</div>
      <div style={styles.bubble}>
        <div style={styles.dots}>
          <span style={{ ...styles.dot, animationDelay: '0ms' }} />
          <span style={{ ...styles.dot, animationDelay: '160ms' }} />
          <span style={{ ...styles.dot, animationDelay: '320ms' }} />
        </div>
        <span style={styles.label}>{PHASES[phase]}</span>
      </div>
      <style>{`
        @keyframes bounce {
          0%, 80%, 100% { transform: translateY(0); opacity: 0.4; }
          40%            { transform: translateY(-5px); opacity: 1; }
        }
      `}</style>
    </div>
  )
}

const styles = {
  row: {
    display:      'flex',
    alignItems:   'flex-start',
    gap:          '10px',
    marginBottom: '12px',
  },
  avatar: {
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
  },
  bubble: {
    display:      'flex',
    alignItems:   'center',
    gap:          '10px',
    background:   'var(--surface-2)',
    border:       '1px solid var(--border)',
    borderRadius: '4px 14px 14px 14px',
    padding:      '10px 16px',
  },
  dots: {
    display: 'flex',
    gap:     '5px',
  },
  dot: {
    display:         'inline-block',
    width:           '6px',
    height:          '6px',
    borderRadius:    '50%',
    background:      'var(--purple-400)',
    animation:       'bounce 1.2s ease-in-out infinite',
  },
  label: {
    fontSize: '12px',
    color:    'var(--text-muted)',
  },
}
