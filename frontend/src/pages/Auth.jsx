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

const HEADINGS = {
  login: {
    title: 'Welcome back',
    text: 'Sign in to keep track of every drop.',
  },
  resident: {
    title: 'Join your community',
    text: 'Create your account and connect to your apartment.',
  },
  admin: {
    title: 'Set up your apartment',
    text: 'Create a workspace for your residents and their bills.',
  },
};

/* Small inline icon set so nothing depends on emoji / font glyphs */
const PATHS = {
  drop: 'M12 3s6 6.2 6 10.5A6 6 0 0 1 6 13.5C6 9.2 12 3 12 3Z',
  chart: 'M4 19V5M4 19h16M8 15l3-4 3 2 4-6',
  bill: 'M6 3h12v18l-3-2-3 2-3-2-3 2V3ZM9 8h6M9 12h6',
  alert: 'M12 4 3 20h18L12 4ZM12 10v4M12 17v.01',
  leaf: 'M5 19C5 10 10 5 20 4c0 10-4 15-13 15M5 19c2-4 5-7 9-9',
  shower: 'M5 9a7 7 0 0 1 14 0H5ZM8 13v2M12 13v3M16 13v2M10 19v1M14 19v1',
  home: 'M4 11 12 4l8 7v9H4v-9ZM10 20v-5h4v5',
  shield: 'M12 3 5 6v6c0 4.5 3 7.5 7 9 4-1.5 7-4.5 7-9V6l-7-3ZM9 12l2 2 4-4',
  info: 'M12 8v.01M12 11v5M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Z',
  eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12ZM12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z',
  eyeOff: 'M3 3l18 18M10.6 6.1A9.8 9.8 0 0 1 12 6c6 0 10 6 10 6a17 17 0 0 1-3.2 3.7M6.6 7.6C3.9 9.4 2 12 2 12s4 6 10 6c1.5 0 2.9-.4 4.1-1M9.9 9.9a3 3 0 0 0 4.2 4.2',
};

function Icon({ name, size = 20 }) {
  return (
    <svg
      className="ico"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={PATHS[name]} />
    </svg>
  );
}

const FEATURES = [
  {
    icon: 'chart',
    title: 'Smart water insights',
    description: 'See daily consumption and spot unusual usage.',
  },
  {
    icon: 'bill',
    title: 'Fair, transparent bills',
    description: 'Share water costs based on what each unit uses.',
  },
  {
    icon: 'alert',
    title: 'Leak alerts',
    description: 'Get notified when usage patterns look off.',
  },
];

const WATER_TIPS = [
  {
    icon: 'drop',
    title: 'Fix leaks early',
    description: 'A small drip wastes a lot of water over time.',
  },
  {
    icon: 'leaf',
    title: 'Use water wisely',
    description: 'Turn off the tap while brushing your teeth.',
  },
  {
    icon: 'shower',
    title: 'Take shorter showers',
    description: 'A few minutes less makes a real difference.',
  },
];

const IMPACT = [
  { icon: 'drop', title: 'Less waste', text: 'Protect a shared resource' },
  { icon: 'home', title: 'Clearer bills', text: 'Understand your usage' },
  { icon: 'leaf', title: 'Greener living', text: 'Build better habits' },
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

  const heading = HEADINGS[mode];

  return (
    <main className="aq">
      {/* ───────── Form panel (first on mobile, right on desktop) ───────── */}
      <section className="aq-panel" aria-label="Account access">
        <div className="aq-panel-inner">
          <a className="aq-brand aq-brand--mobile" href="/" aria-label="AquaTrack home">
            <span className="aq-logo"><Icon name="drop" size={18} /></span>
            AquaTrack
          </a>

          <div className="aq-card">
            <div className="aq-heading">
              <h2>{heading.title}</h2>
              <p>{heading.text}</p>
            </div>

            <div className="aq-tabs" role="tablist" aria-label="Account type">
              {Object.entries(MODES).map(([key, label]) => (
                <button
                  type="button"
                  role="tab"
                  aria-selected={key === mode}
                  key={key}
                  className={`aq-tab ${key === mode ? 'is-active' : ''}`}
                  onClick={() => changeMode(key)}
                >
                  {label}
                </button>
              ))}
            </div>

            <form className="aq-fields" onSubmit={submit}>
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
                <div className="aq-row">
                  <Field label="Apartment code" hint="Ask your apartment administrator.">
                    <input
                      name="apartmentCode"
                      placeholder="e.g. GREEN2026"
                      autoCapitalize="characters"
                      value={f.apartmentCode || ''}
                      required
                      onChange={set('apartmentCode')}
                    />
                  </Field>
                  <Field label="Unit number" hint="As registered by your administrator.">
                    <input
                      name="unitNumber"
                      placeholder="e.g. A-101"
                      value={f.unitNumber || ''}
                      required
                      onChange={set('unitNumber')}
                    />
                  </Field>
                </div>
              )}

              <Field label="Email address">
                <input
                  name="email"
                  type="email"
                  inputMode="email"
                  placeholder="you@example.com"
                  autoComplete="username"
                  value={f.email || ''}
                  required
                  onChange={set('email')}
                />
              </Field>

              <Field label="Password" hint={mode === 'login' ? '' : 'Use at least 8 characters.'}>
                <div className="aq-password">
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
                    className="aq-eye"
                    onClick={() => setShowPassword((prev) => !prev)}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    aria-pressed={showPassword}
                  >
                    <Icon name={showPassword ? 'eyeOff' : 'eye'} size={20} />
                  </button>
                </div>
              </Field>

              <div className="aq-error" aria-live="polite"><Msg>{err}</Msg></div>

              <button className="aq-submit" type="submit" disabled={busy}>
                {busy ? (
                  <><span className="aq-spinner" /> Please wait…</>
                ) : (
                  MODES[mode]
                )}
              </button>
            </form>

            {mode === 'login' && (
              <div className="aq-demo">
                <span className="aq-demo-ico"><Icon name="info" size={18} /></span>
                <div>
                  <strong>Trying the demo?</strong>
                  <p>Admin: <code>admin@demo.com</code></p>
                  <p>Resident: <code>resident1@demo.com</code></p>
                  <small>Demo passwords are configured by your application.</small>
                </div>
              </div>
            )}

            <p className="aq-secure">
              <Icon name="shield" size={16} />
              Your apartment data stays protected by your sign-in.
            </p>
          </div>

          <p className="aq-copy">
            © {new Date().getFullYear()} AquaTrack · Smarter living starts here.
          </p>
        </div>
      </section>

      {/* ───────── Showcase (below on mobile, left on desktop) ───────── */}
      <section className="aq-show" aria-label="AquaTrack benefits">
        <div className="aq-show-inner">
          <header className="aq-topbar">
            <a className="aq-brand" href="/" aria-label="AquaTrack home">
              <span className="aq-logo"><Icon name="drop" size={18} /></span>
              AquaTrack
            </a>
            <span className="aq-tagline">Smarter water. Fairer bills.</span>
          </header>

          <div className="aq-hero">
            <span className="aq-pill"><span className="aq-live" /> Smarter water management</span>
            <h1>Every drop counts.</h1>
            <p className="aq-lead">
              Track water usage, manage shared expenses, and build a more
              sustainable community, one drop at a time.
            </p>

            <ul className="aq-features">
              {FEATURES.map((ft) => (
                <li key={ft.title}>
                  <span className="aq-ficon"><Icon name={ft.icon} /></span>
                  <div>
                    <h3>{ft.title}</h3>
                    <p>{ft.description}</p>
                  </div>
                </li>
              ))}
            </ul>
          </div>

          <figure className="aq-visual">
            <img
              src="/images/water-saving-banner.png"
              alt="Fresh water, green leaves and a calm sustainable community"
              loading="lazy"
              onError={(e) => { e.currentTarget.style.display = 'none'; }}
            />
            <figcaption className="aq-notes">
              <div className="aq-note">
                <span className="aq-note-ico"><Icon name="drop" size={18} /></span>
                <div>
                  <strong>Use water mindfully</strong>
                  <small>Small habits create lasting change.</small>
                </div>
              </div>
              <div className="aq-note aq-note--green">
                <span className="aq-note-ico"><Icon name="leaf" size={18} /></span>
                <div>
                  <strong>Check for leaks regularly</strong>
                  <small>Protect water and your budget.</small>
                </div>
              </div>
            </figcaption>
          </figure>

          <section className="aq-tips" aria-labelledby="aq-tips-title">
            <h2 id="aq-tips-title">Small changes, real impact</h2>
            <p>Simple habits that cut waste at home.</p>
            <div className="aq-tips-grid">
              {WATER_TIPS.map((tip) => (
                <article key={tip.title}>
                  <span className="aq-tip-ico"><Icon name={tip.icon} size={18} /></span>
                  <h3>{tip.title}</h3>
                  <p>{tip.description}</p>
                </article>
              ))}
            </div>
          </section>

          <ul className="aq-impact">
            {IMPACT.map((it) => (
              <li key={it.title}>
                <Icon name={it.icon} size={22} />
                <p><strong>{it.title}</strong><small>{it.text}</small></p>
              </li>
            ))}
          </ul>
        </div>
      </section>
    </main>
  );
}