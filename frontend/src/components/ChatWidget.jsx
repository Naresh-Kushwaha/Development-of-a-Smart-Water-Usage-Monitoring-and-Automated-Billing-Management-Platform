import { useEffect, useRef, useState } from 'react';
import api, { errMsg } from '../api.js';
import { useAuth } from '../auth.jsx';

const SUGGESTIONS = {
  RESIDENT: ['How do I log a reading?', 'Why is my bill high?', 'What does a leak alert mean?', 'How can I pay my invoice?'],
  ADMIN: ['How do I upload readings?', 'How does cost sharing work?', 'How do I finalize a cycle?', 'How do I add a household?'],
};

const s = {
  fab: { position: 'fixed', right: 20, bottom: 20, zIndex: 50, width: 56, height: 56, borderRadius: '50%', border: 0, cursor: 'pointer', background: '#0e7490', color: '#fff', fontSize: 26, boxShadow: '0 4px 14px rgba(0,0,0,.25)' },
  panel: { position: 'fixed', right: 20, bottom: 88, zIndex: 50, width: 'min(380px, 92vw)', height: 'min(520px, 75vh)', display: 'flex', flexDirection: 'column', background: '#fff', borderRadius: 14, boxShadow: '0 8px 30px rgba(0,0,0,.28)', overflow: 'hidden' },
  head: { background: '#083344', color: '#fff', padding: '.7rem 1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  body: { flex: 1, overflowY: 'auto', padding: '.8rem', display: 'flex', flexDirection: 'column', gap: '.5rem', background: '#f8fafc' },
  user: { alignSelf: 'flex-end', background: '#0e7490', color: '#fff', padding: '.5rem .75rem', borderRadius: '12px 12px 2px 12px', maxWidth: '85%', whiteSpace: 'pre-wrap', fontSize: 14 },
  bot: { alignSelf: 'flex-start', background: '#fff', border: '1px solid #e2e8f0', padding: '.5rem .75rem', borderRadius: '12px 12px 12px 2px', maxWidth: '88%', whiteSpace: 'pre-wrap', fontSize: 14 },
  chips: { display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 4 },
  chip: { border: '1px solid #67e8f9', background: '#ecfeff', color: '#155e75', borderRadius: 99, padding: '.25rem .65rem', fontSize: 12, cursor: 'pointer' },
  form: { display: 'flex', gap: 6, padding: '.6rem', borderTop: '1px solid #e2e8f0', background: '#fff' },
  input: { flex: 1, padding: '.5rem .6rem', border: '1px solid #cbd5e1', borderRadius: 8, font: 'inherit' },
};

export default function ChatWidget() {
  const { user } = useAuth();
  const firstName = (user.fullName || '').split(' ')[0] || 'there';
  const greeting = { role: 'assistant', content: `Hi ${firstName}! I'm the AquaTrack assistant. Ask me about readings, bills, alerts or payments.` };

  const [open, setOpen] = useState(false);
  const [msgs, setMsgs] = useState([greeting]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  const endRef = useRef(null);

  useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [msgs, busy, open]);

  const send = async (text) => {
    const q = (text ?? input).trim();
    if (!q || busy) return;
    const next = [...msgs, { role: 'user', content: q }];
    setMsgs(next); setInput(''); setErr(''); setBusy(true);
    try {
      // the greeting is only for display; send the real conversation (the server keeps the last few turns)
      const { data } = await api.post('/chat', { messages: next.slice(1).map(({ role, content }) => ({ role, content })) });
      setMsgs([...next, { role: 'assistant', content: data.reply }]);
    } catch (e) { setErr(errMsg(e)); } finally { setBusy(false); }
  };

  return (
    <>
      {open && (
        <section style={s.panel} aria-label="AquaTrack assistant">
          <header style={s.head}>
            <strong>💧 AquaTrack assistant</strong>
            <button onClick={() => setOpen(false)} aria-label="Close chat" style={{ background: 'none', border: 0, color: '#fff', fontSize: 20, cursor: 'pointer' }}>×</button>
          </header>
          <div style={s.body}>
            {msgs.map((m, i) => <div key={i} style={m.role === 'user' ? s.user : s.bot}>{m.content}</div>)}
            {msgs.length === 1 && (
              <div style={s.chips}>
                {(SUGGESTIONS[user.role] || SUGGESTIONS.RESIDENT).map((q) => <button key={q} style={s.chip} onClick={() => send(q)}>{q}</button>)}
              </div>
            )}
            {busy && <div style={{ ...s.bot, color: '#64748b' }}>Typing…</div>}
            {err && <div style={{ ...s.bot, background: '#fee2e2', color: '#991b1b', borderColor: '#fecaca' }} role="alert">{err}</div>}
            <div ref={endRef} />
          </div>
          <form style={s.form} onSubmit={(e) => { e.preventDefault(); send(); }}>
            <input style={s.input} value={input} maxLength={1000} onChange={(e) => setInput(e.target.value)} placeholder="Ask a question…" aria-label="Your question" />
            <button className="primary" disabled={busy || !input.trim()}>Send</button>
          </form>
        </section>
      )}
      <button style={s.fab} onClick={() => setOpen(!open)} aria-label={open ? 'Close assistant' : 'Open assistant'}>{open ? '×' : '💬'}</button>
    </>
  );
}
