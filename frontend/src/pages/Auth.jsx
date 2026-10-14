import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import api, { errMsg } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Field, Msg } from '../components/ui.jsx';

const MODES = { login: 'Sign in', resident: 'Resident sign-up', admin: 'Register apartment' };

export default function Auth() {
  const { user, login } = useAuth();
  const nav = useNavigate();
  const [mode, setMode] = useState('login');
  const [f, setF] = useState({});
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  if (user) return <Navigate to="/" replace />;

  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = async (e) => {
    e.preventDefault();
    setErr(''); setBusy(true);
    try {
      const path = { login: '/auth/login', resident: '/auth/register', admin: '/auth/register-admin' }[mode];
      const { data } = await api.post(path, f);
      login(data);
      nav('/');
    } catch (ex) { setErr(errMsg(ex)); } finally { setBusy(false); }
  };

  return (
    <div className="auth">
      <div className="auth-art">
        <h1>💧 AquaTrack</h1>
        <p>Know every litre. Split every rupee fairly.</p>
        <ul>
          <li>Daily usage charts and bill previews</li>
          <li>Leak and overuse alerts by email</li>
          <li>Transparent shared-cost allocation</li>
          <li>One-click PDF invoices</li>
        </ul>
      </div>
      <form className="auth-form" onSubmit={submit}>
        <div className="tabs">
          {Object.entries(MODES).map(([k, v]) => (
            <button type="button" key={k} className={k === mode ? 'on' : ''} onClick={() => { setMode(k); setErr(''); }}>{v}</button>
          ))}
        </div>
        {mode !== 'login' && <Field label="Full name"><input required onChange={set('fullName')} /></Field>}
        {mode === 'admin' && (
          <>
            <Field label="Apartment / society name"><input required onChange={set('apartmentName')} /></Field>
            <Field label="Address (optional)"><input onChange={set('address')} /></Field>
          </>
        )}
        {mode === 'resident' && (
          <>
            <Field label="Apartment code" hint="Shown to your admin in the sidebar"><input required onChange={set('apartmentCode')} style={{ textTransform: 'uppercase' }} /></Field>
            <Field label="Unit number" hint="e.g. A-101 - your admin must have registered it"><input required onChange={set('unitNumber')} /></Field>
          </>
        )}
        <Field label="Email"><input type="email" required autoComplete="username" onChange={set('email')} /></Field>
        <Field label="Password" hint={mode === 'login' ? '' : 'At least 8 characters'}>
          <input type="password" required minLength={mode === 'login' ? 1 : 8} autoComplete={mode === 'login' ? 'current-password' : 'new-password'} onChange={set('password')} />
        </Field>
        <Msg>{err}</Msg>
        <button className="primary" disabled={busy}>{busy ? 'Please wait…' : MODES[mode]}</button>
        {mode === 'login' && (
          <small className="muted demo">
            Demo: admin@demo.com / Admin@123 &nbsp;·&nbsp; resident1@demo.com / Resident@123
          </small>
        )}
      </form>
    </div>
  );
}
