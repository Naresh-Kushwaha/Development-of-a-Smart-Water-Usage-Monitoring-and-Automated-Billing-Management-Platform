import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { errMsg } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Field, Msg } from '../components/ui.jsx';

const ERRORS = {
  email_not_verified: 'Google did not confirm your email address, so we cannot sign you in.',
  oauth_failed: 'Google sign-in was cancelled or failed. Please try again.',
};

/**
 * The backend sends the browser here after Google login, with the result in the URL fragment:
 *   #token=...              -> signed in (existing account)
 *   #signup=<ticket>&email  -> first visit: ask which apartment/unit, then create the resident account
 *   #error=...              -> something went wrong
 * Fragments are never sent to a server; we remove it from the address bar immediately.
 */
export default function OAuthCallback() {
  const { login } = useAuth();
  const nav = useNavigate();
  const params = useMemo(() => new URLSearchParams(window.location.hash.slice(1)), []);
  const ticket = params.get('signup');
  const errorCode = params.get('error');

  const [error, setError] = useState(errorCode ? (ERRORS[errorCode] || 'Sign-in failed.') : '');
  const [f, setF] = useState({ apartmentCode: '', unitNumber: '' });
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const token = params.get('token');
    window.history.replaceState(null, '', '/oauth/callback');
    if (!token) return;
    api.get('/auth/me', { headers: { Authorization: `Bearer ${token}` } })
      .then((r) => { login({ token, user: r.data }); nav('/', { replace: true }); })
      .catch((e) => setError(errMsg(e)));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const complete = async (e) => {
    e.preventDefault();
    setError(''); setBusy(true);
    try {
      const { data } = await api.post('/auth/oauth/complete', { ticket, ...f });
      login(data);
      nav('/', { replace: true });
    } catch (ex) { setError(errMsg(ex)); } finally { setBusy(false); }
  };

  if (ticket) {
    return (
      <div className="auth" style={{ gridTemplateColumns: '1fr' }}>
        <form className="auth-form" onSubmit={complete}>
          <h2>Almost done</h2>
          <p className="muted">
            Signed in with Google as <strong>{params.get('email')}</strong>. Tell us where you live to finish creating your resident account.
          </p>
          <Field label="Apartment code" hint="Ask your apartment admin"><input required value={f.apartmentCode} onChange={(e) => setF({ ...f, apartmentCode: e.target.value })} style={{ textTransform: 'uppercase' }} /></Field>
          <Field label="Unit number" hint="e.g. A-101 - your admin must have registered it"><input required value={f.unitNumber} onChange={(e) => setF({ ...f, unitNumber: e.target.value })} /></Field>
          <Msg>{error}</Msg>
          <button className="primary" disabled={busy}>{busy ? 'Please wait…' : 'Create my account'}</button>
          <Link to="/login">Cancel</Link>
        </form>
      </div>
    );
  }

  return (
    <div className="auth" style={{ gridTemplateColumns: '1fr' }}>
      <div className="auth-form">
        {error ? <><Msg>{error}</Msg><Link to="/login">Back to sign in</Link></> : <p className="muted">Signing you in…</p>}
      </div>
    </div>
  );
}
