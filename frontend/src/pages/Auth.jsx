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

const WATER_TIPS = [
  {
    icon: '⌁',
    title: 'Fix leaks early',
    description: 'A small drip can waste water over time.',
  },
  {
    icon: '◉',
    title: 'Use water wisely',
    description: 'Turn off the tap while brushing your teeth.',
  },
  {
    icon: '◷',
    title: 'Take shorter showers',
    description: 'A few minutes less can make a difference.',
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
      key === 'apartmentCode' ? e.target.value.toUpperCase() : e.target.value;
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
      <header className="site-header">
        <a className="aqua-brand" href="/" aria-label="AquaTrack home">
          <span className="brand-icon" aria-hidden="true">💧</span>
          <span>AquaTrack<span className="brand-dot">.</span></span>
        </a>
        <div className="header-message">
          Smarter water <span>•</span> Fairer bills <span>•</span> Greener tomorrow
        </div>
      </header>

      <div className="auth-layout">
        <section className="auth-showcase" aria-label="AquaTrack benefits">
          <div className="hero-copy">
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
              Track water usage, manage shared expenses, and build a more
              sustainable community — one drop at a time.
            </p>

            <div className="feature-list">
              {FEATURES.map((feature) => (
                <div className="feature-item" key={feature.title}>
                  <div className="feature-icon" aria-hidden="true">{feature.icon}</div>
                  <div>
                    <h3>{feature.title}</h3>
                    <p>{feature.description}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="hero-visual">
            <img
              className="water-banner"
              src="/images/water-saving-banner.png"
              alt="Fresh water, green leaves and a calm sustainable community"
            />
            <div className="visual-shade" />
            <div className="usage-card usage-card-top">
              <span className="mini-icon">💧</span>
              <div>
                <p>Make every drop count</p>
                <strong>Use water mindfully</strong>
                <small>Small habits create lasting change</small>
              </div>
            </div>
            <div className="usage-card usage-card-bottom">
              <span className="mini-icon green">🌿</span>
              <div>
                <p>Water-saving reminder</p>
                <strong>Check for leaks regularly</strong>
                <small>Protect water and your budget</small>
              </div>
            </div>
          </div>

          <section className="tips-card" aria-labelledby="tips-title">
            <div className="tips-heading">
              <span className="tips-bulb" aria-hidden="true">✦</span>
              <div>
                <span className="section-kicker">WATER-SAVING TIPS</span>
                <h2 id="tips-title">Small changes. Real impact.</h2>
                <p>Simple habits can help reduce waste at home.</p>
              </div>
            </div>
            <div className="tips-list">
              {WATER_TIPS.map((tip) => (
                <article className="tip-item" key={tip.title}>
                  <span className="tip-icon" aria-hidden="true">{tip.icon}</span>
                  <div>
                    <h3>{tip.title}</h3>
                    <p>{tip.description}</p>
                  </div>
                </article>
              ))}
            </div>
          </section>

          <div className="impact-row">
            <div><span>💧</span><p><strong>Less waste</strong><small>Protect a shared resource</small></p></div>
            <div><span>⌂</span><p><strong>Clearer bills</strong><small>Understand your usage</small></p></div>
            <div><span>🌱</span><p><strong>Greener living</strong><small>Build better habits</small></p></div>
          </div>
          <p className="showcase-footer">Better insights. Fairer bills. A greener tomorrow.</p>
        </section>

        <section className="auth-panel" aria-label="Account access">
          <div className="mobile-brand">
            <span aria-hidden="true">💧</span> AquaTrack<span className="brand-dot">.</span>
          </div>

          <div className="auth-card">
            <div className="auth-heading">
              <span className="form-kicker">
                {mode === 'login' ? 'WELCOME BACK' : 'GET STARTED'}
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
                  <Field label="Apartment code" hint="Ask your apartment administrator for this code.">
                    <input
                      name="apartmentCode"
                      placeholder="e.g. GREEN2026"
                      autoCapitalize="characters"
                      value={f.apartmentCode || ''}
                      required
                      onChange={set('apartmentCode')}
                    />
                  </Field>
                  <Field label="Unit number" hint="Use the unit registered by your administrator.">
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

              <Field label="Password" hint={mode === 'login' ? '' : 'Use at least 8 characters.'}>
                <div className="password-wrap">
                  <input
                    name="password"
                    type={showPassword ? 'text' : 'password'}
                    placeholder={mode === 'login' ? 'Enter your password' : 'Create a strong password'}
                    autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                    minLength={mode === 'login' ? 1 : 8}
                    value={f.password || ''}
                    required
                    onChange={set('password')}
                  />
                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() => setShowPassword((prev) => !prev)}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? 'Hide' : 'Show'}
                  </button>
                </div>
              </Field>

              <div className="auth-error" aria-live="polite"><Msg>{err}</Msg></div>

              <button className="auth-submit" type="submit" disabled={busy}>
                {busy ? (
                  <><span className="auth-spinner" /> Please wait...</>
                ) : (
                  <>{MODES[mode]} <span className="submit-arrow">→</span></>
                )}
              </button>
            </form>

            {mode === 'login' && (
              <div className="demo-box">
                <div className="demo-icon" aria-hidden="true">i</div>
                <div>
                  <strong>Trying the demo?</strong>
                  <p>Admin: admin@demo.com</p>
                  <p>Resident: resident1@demo.com</p>
                  <small>Demo passwords are configured by your application.</small>
                </div>
              </div>
            )}

            <div className="auth-security">
              <span aria-hidden="true">◇</span>
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
