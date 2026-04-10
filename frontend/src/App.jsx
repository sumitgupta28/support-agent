import { useEffect } from 'react'
import { useAgent } from './hooks/useAgent'
import ChatPanel from './components/ChatPanel'
import SidePanel from './components/SidePanel'
import Header from './components/Header'
import BackendAlert from './components/BackendAlert'

export default function App() {
  const agent = useAgent()

  // Ping the backend on mount to show an alert if it's down
  useEffect(() => {
    agent.pingBackend()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div style={styles.root}>
      <Header
        sessionId={agent.sessionId}
        onReset={agent.reset}
        backendOk={agent.backendOk}
      />

      {!agent.backendOk && <BackendAlert />}

      <main style={styles.main}>
        <ChatPanel
          messages={agent.messages}
          loading={agent.loading}
          escalated={agent.escalated}
          onSend={agent.send}
        />
        <SidePanel
          toolTrace={agent.toolTrace}
          escalated={agent.escalated}
          sessionId={agent.sessionId}
        />
      </main>
    </div>
  )
}

const styles = {
  root: {
    display:       'flex',
    flexDirection: 'column',
    height:        '100vh',
    overflow:      'hidden',
    background:    'var(--bg)',
  },
  main: {
    display:  'flex',
    flex:     1,
    overflow: 'hidden',
    gap:      '1px',
    background: 'var(--border)',
  },
}
