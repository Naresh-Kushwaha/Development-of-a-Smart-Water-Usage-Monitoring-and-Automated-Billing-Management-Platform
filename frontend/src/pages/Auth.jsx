
import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import api, { errMsg } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Field, Msg } from '../components/ui.jsx';
import './Auth.css';

const MODES = {
  login: 'Sign in',
  resident: 'Resident sign-up',
  admin: 'Register apartment',
};

const FEATURES = [
  {
    icon: '◷',
    title: 'Smart water insights',
    description: 'Understand daily consumption and spot unusual usage.',
  },
  {
    icon: '↗',
    title: 'Fair & transparent bills',
    description: 'Make shared water expenses simple and transparent.',
  },
  {
    icon: '⌁',
    title: 'Leak alerts',
    description: 'Stay informed when usage patterns look unusual.',
  },
];

export default function Auth() {
  const { user, login } = useAuth();
  const nav = useNavigate();

  const [mode, setMode] = useState('login');
  const [f, setF] = useState({});
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  if (user) return <Navigate to="/" replace />;

  const set = (key) => (e) => {
    const value =
      key === 'apartmentCode'
        ? e.target.value.toUpperCase()
        : e.target.value;

    setF((prev) => ({ ...prev, [key]: value }));
  };

  const changeMode = (nextMode) => {
    setMode(nextMode);
    setF({});
    setErr('');
    setShowPassword(false);
  };

  const submit = async (e) => {
    e.preventDefault();
    setErr('');
    setBusy(true);

    try {
      const path = {
        login: '/auth/login',
        resident: '/auth/register',
        admin: '/auth/register-admin',
      }[mode];

      const { data } = await api.post(path, f);
      login(data);
      nav('/');
    } catch (ex) {
      setErr(errMsg(ex));
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="aqua-auth">
      <div className="auth-shell">
        {/* Left branding panel */}
        <section className="auth-showcase">
          <div className="showcase-glow showcase-glow-one" />
          <div className="showcase-glow showcase-glow-two" />

          <a className="aqua-brand" href="/" aria-label="AquaTrack home">
            <span className="brand-icon">
              <svg
                viewBox="0 0 40 40"
                fill="none"
                aria-hidden="true"
              >
                <path
                  d="M20 4C15 12 8 19 8 25a12 12 0 0 0 24 0C32 19 25 12 20 4Z"
                  fill="currentColor"
                />
                <path
                  d="M14 26c.5 3.2 2.5 5 5.5 5.5"
                  stroke="#087E8B"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                />
              </svg>
            </span>
            <span>AquaTrack<span className="brand-dot">.</span></span>
          </a>

          <div className="showcase-content">
            <span className="eyebrow">
              <span className="status-dot" />
              SMARTER WATER MANAGEMENT
            </span>

            <h1>
              Every drop
              <br />
              <span>counts.</span>
            </h1>

            <p className="showcase-description">
              A smarter way to track water, manage apartment
              expenses, and build a more sustainable community.
            </p>

            <div className="feature-list">
              {FEATURES.map((feature) => (
                <div className="feature-item" key={feature.title}>
                  <div className="feature-icon">{feature.icon}</div>
                  <div>
                    <h3>{feature.title}</h3>
                    <p>{feature.description}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="showcase-footer">
            <div className="footer-watermark">AQUATRACK / 01</div>
            <p>Better insights. Fairer bills. A greener tomorrow.</p>
          </div>

          {/* Decorative water illustration */}
          <div className="water-art" aria-hidden="true">
            <div className="water-ring water-ring-one" />
            <div className="water-ring water-ring-two" />
            <div className="water-orb">
              <svg viewBox="0 0 120 120" fill="none">
                <path
                  d="M60 13C47 35 27 53 27 73a33 33 0 0 0 66 0C93 53 73 35 60 13Z"
                  fill="url(#waterGradient)"
                />
                <path
                  d="M43 75c1 9 7 15 17 17"
                  stroke="#FFFFFF"
                  strokeWidth="5"
                  strokeLinecap="round"
                />
                <defs>
                  <linearGradient
                    id="waterGradient"
                    x1="30"
                    y1="20"
                    x2="90"
                    y2="100"
                    gradientUnits="userSpaceOnUse"
                  >
                    <stop stopColor="#9BF6F0" />
                    <stop offset="1" stopColor="#13B8C5" />
                  </linearGradient>
                </defs>
              </svg>
            </div>
          </div>
        </section>

        {/* Right authentication panel */}
        <section className="auth-panel">
          <div className="mobile-brand">
            <span className="mobile-brand-icon">💧</span>
            AquaTrack<span>.</span>
          </div>

          <div className="auth-card">
            <div className="auth-heading">
              <span className="form-kicker">
                {mode === 'login'
                  ? 'WELCOME BACK'
                  : 'GET STARTED'}
              </span>

              <h2>
                {mode === 'login'
                  ? 'Welcome back'
                  : mode === 'resident'
                    ? 'Join your community'
                    : 'Set up your apartment'}
              </h2>

              <p>
                {mode === 'login'
                  ? 'Sign in to keep track of every drop.'
                  : mode === 'resident'
                    ? 'Create your account and connect to your apartment.'
                    : 'Create an apartment workspace for your residents.'}
              </p>
            </div>

            <div className="auth-tabs" role="tablist" aria-label="Account type">
              {Object.entries(MODES).map(([key, label]) => (
                <button
                  type="button"
                  role="tab"
                  aria-selected={key === mode}
                  key={key}
                  className={`auth-tab ${key === mode ? 'active' : ''}`}
                  onClick={() => changeMode(key)}
                >
                  {label}
                </button>
              ))}
            </div>

            <form className="auth-fields" onSubmit={submit}>
              {mode !== 'login' && (
                <Field label="Full name">
                  <input
                    name="fullName"
                    placeholder="e.g. Naresh Kushwaha"
                    autoComplete="name"
                    value={f.fullName || ''}
                    required
                    onChange={set('fullName')}
                  />
                </Field>
              )}

              {mode === 'admin' && (
                <>
                  <Field label="Apartment / society name">
                    <input
                      name="apartmentName"
                      placeholder="e.g. Green Valley Residency"
                      autoComplete="organization"
                      value={f.apartmentName || ''}
                      required
                      onChange={set('apartmentName')}
                    />
                  </Field>

                  <Field label="Address (optional)">
                    <input
                      name="address"
                      placeholder="Enter apartment address"
                      autoComplete="street-address"
                      value={f.address || ''}
                      onChange={set('address')}
                    />
                  </Field>
                </>
              )}

              {mode === 'resident' && (
                <>
                  <Field
                    label="Apartment code"
                    hint="Ask your apartment administrator for this code."
                  >
                    <input
                      name="apartmentCode"
                      placeholder="e.g. GREEN2026"
                      autoCapitalize="characters"
                      value={f.apartmentCode || ''}
                      required
                      onChange={set('apartmentCode')}
                    />
                  </Field>

                  <Field
                    label="Unit number"
                    hint="Use the unit registered by your administrator."
                  >
                    <input
                      name="unitNumber"
                      placeholder="e.g. A-101"
                      value={f.unitNumber || ''}
                      required
                      onChange={set('unitNumber')}
                    />
                  </Field>
                </>
              )}

              <Field label="Email address">
                <input
                  name="email"
                  type="email"
                  placeholder="you@example.com"
                  autoComplete="username"
                  value={f.email || ''}
                  required
                  onChange={set('email')}
                />
              </Field>

              <Field
                label="Password"
                hint={
                  mode === 'login'
                    ? ''
                    : 'Use at least 8 characters.'
                }
              >
                <div className="password-wrap">
                  <input
                    name="password"
                    type={showPassword ? 'text' : 'password'}
                    placeholder={
                      mode === 'login'
                        ? 'Enter your password'
                        : 'Create a strong password'
                    }
                    autoComplete={
                      mode === 'login'
                        ? 'current-password'
                        : 'new-password'
                    }
                    minLength={mode === 'login' ? 1 : 8}
                    value={f.password || ''}
                    required
                    onChange={set('password')}
                  />

                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowPassword((prev) => !prev)}
                    aria-label={
                      showPassword ? 'Hide password' : 'Show password'
                    }
                  >
                    {showPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
              </Field>

              <div className="auth-error" aria-live="polite">
                <Msg>{err}</Msg>
              </div>

              <button
                className="auth-submit"
                type="submit"
                disabled={busy}
              >
                {busy ? (
                  <>
                    <span className="auth-spinner" />
                    Please wait...
                  </>
                ) : (
                  <>
                    {MODES[mode]}
                    <span className="submit-arrow">→</span>
                  </>
                )}
              </button>
            </form>

            {mode === 'login' && (
              <div className="demo-box">
                <div className="demo-icon">i</div>
                <div>
                  <strong>Try the demo</strong>
                  <p>Admin: admin@demo.com</p>
                  <p>Resident: resident1@demo.com</p>
                  <small>
                    Demo passwords are configured by your application.
                  </small>
                </div>
              </div>
            )}

            <div className="auth-security">
              <span>◇</span>
              Your apartment data stays protected by your sign-in.
            </div>
          </div>

          <p className="panel-copyright">
            © {new Date().getFullYear()} AquaTrack · Smarter living starts here.
          </p>
        </section>
      </div>
    </main>
  );
}
