export default function BackendAlert() {
  return (
    <div style={styles.bar}>
      <span style={styles.icon}>▲</span>
      <span>
        Cannot reach the backend on{' '}
        <code style={styles.code}>http://localhost:8080</code>.
        Start Spring Boot with{' '}
        <code style={styles.code}>./gradlew bootRun</code> and refresh.
      </span>
    </div>
  )
}

const styles = {
  bar: {
    display:    'flex',
    alignItems: 'center',
    gap:        '8px',
    padding:    '10px 20px',
    background: 'var(--amber-50)',
    borderBottom: '1px solid #EF9F27',
    fontSize:   '13px',
    color:      'var(--amber-800)',
    flexShrink: 0,
  },
  icon: {
    fontSize:   '12px',
    color:      'var(--amber-600)',
    flexShrink: 0,
  },
  code: {
    fontFamily:   'var(--font-mono)',
    fontSize:     '12px',
    background:   'rgba(0,0,0,0.06)',
    padding:      '1px 5px',
    borderRadius: '4px',
    color:        'var(--amber-800)',
  },
}
