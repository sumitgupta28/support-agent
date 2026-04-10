export default function EscalationBanner() {
  return (
    <div style={styles.banner}>
      <div style={styles.iconWrap}>
        <span style={styles.icon}>▲</span>
      </div>
      <div style={styles.content}>
        <p style={styles.title}>Escalated to human agent</p>
        <p style={styles.body}>
          This case has been handed off. A human agent will follow up within
          2 hours. A ticket has been created in the escalations table.
        </p>
      </div>
    </div>
  )
}

const styles = {
  banner: {
    display:      'flex',
    alignItems:   'flex-start',
    gap:          '12px',
    background:   'var(--amber-50)',
    border:       '1px solid #EF9F27',
    borderRadius: 'var(--radius-md)',
    padding:      '14px 16px',
    margin:       '8px 0',
  },
  iconWrap: {
    width:          '30px',
    height:         '30px',
    borderRadius:   '50%',
    background:     'var(--amber-50)',
    border:         '1px solid #EF9F27',
    display:        'flex',
    alignItems:     'center',
    justifyContent: 'center',
    flexShrink:     0,
  },
  icon: {
    fontSize: '13px',
    color:    'var(--amber-600)',
  },
  content: {
    display:       'flex',
    flexDirection: 'column',
    gap:           '3px',
  },
  title: {
    fontWeight: 600,
    fontSize:   '13px',
    color:      'var(--amber-800)',
    margin:     0,
  },
  body: {
    fontSize:   '12px',
    color:      'var(--amber-600)',
    lineHeight: 1.5,
    margin:     0,
  },
}
