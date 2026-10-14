import { useState } from 'react';
import api, { errMsg } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Card, Field, Msg } from '../components/ui.jsx';

export default function Profile() {
  const { user, login } = useAuth();
  const [name, setName] = useState(user.fullName);
  const [pw, setPw] = useState('');
  const [msg, setMsg] = useState({ kind: '', text: '' });

  const save = async (e) => {
    e.preventDefault();
    try {
      const { data } = await api.put('/auth/me', { fullName: name, newPassword: pw || null });
      login({ token: localStorage.getItem('token'), user: data });
      setPw('');
      setMsg({ kind: 'ok', text: 'Profile updated.' });
    } catch (ex) { setMsg({ kind: 'error', text: errMsg(ex) }); }
  };

  return (
    <>
      <h2>Profile</h2>
      <Card>
        <form className="stack" onSubmit={save}>
          <Field label="Email"><input value={user.email} disabled /></Field>
          <Field label="Full name"><input required value={name} onChange={(e) => setName(e.target.value)} /></Field>
          <Field label="New password" hint="Leave blank to keep the current one"><input type="password" minLength={8} value={pw} onChange={(e) => setPw(e.target.value)} autoComplete="new-password" /></Field>
          <Msg kind={msg.kind}>{msg.text}</Msg>
          <button className="primary">Save changes</button>
        </form>
      </Card>
    </>
  );
}
