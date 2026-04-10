import ToolTrace from './ToolTrace'
import SessionInfo from './SessionInfo'

export default function SidePanel({ toolTrace, escalated, sessionId }) {
  return (
    <aside style={styles.panel}>
      <SessionInfo sessionId={sessionId} escalated={escalated} />
      <div style={styles.divider} />
      <ToolTrace calls={toolTrace} />
    </aside>
  )
}

const styles = {
  panel: {
    flex:          1,
    display:       'flex',
    flexDirection: 'column',
    background:    'var(--surface)',
    overflowY:     'auto',
    minWidth:      '280px',
    maxWidth:      '380px',
  },
  divider: {
    height:     '1px',
    background: 'var(--border)',
    flexShrink: 0,
  },
}
