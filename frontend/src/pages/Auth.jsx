
import { useEffect, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { DotLottieReact } from '@lottiefiles/dotlottie-react';

import api, { errMsg } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Field, Msg } from '../components/ui.jsx';
import './Auth.css';

const MODES = {
  login: 'Sign in',
  resident: 'Resident sign-up',
  admin: 'Register apartment',
};

function GoogleIcon() {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 48 48"
      aria-hidden="true"
    >
      <path
        fill="#EA4335"
        d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5Z"
      />
      <path
        fill="#4285F4"
        d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.7 7.18l7.19 5.59c4.2-3.88 7.55-9.58 7.55-17.24Z"
      />
      <path
        fill="#FBBC05"
        d="M10.53 28.59A14.4 14.4 0 0 1 9.75 24c0-1.59.27-3.13.78-4.59l-7.98-6.19A23.9 23.9 0 0 0 0 24c0 3.87.93 7.52 2.56 10.78l7.97-6.19Z"
      />
      <path
        fill="#34A853"
        d="M24 48c6.48 0 11.93-2.13 15.91-5.79l-7.19-5.59c-1.99 1.33-4.54 2.12-8.72 2.12-6.26 0-11.57-4.22-13.46-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48Z"
      />
    </svg>
  );
}

function WaterAnimation({ src, className = '', loop = true }) {
  return (
    <div className={className}>
      <DotLottieReact
        src={src}
        loop={loop}
        autoplay
        style={{
          width: '100%',
          height: '100%',
        }}
      />
    </div>
  );
}

export default function Auth() {
  const { user, login } = useAuth();
  const nav = useNavigate();

  const [mode, setMode] = useState('login');
  const [f, setF] = useState({});
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  const [google, setGoogle] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    api.get('/auth/providers')
      .then((r) => setGoogle(!!r.data.google))
      .catch(() => setGoogle(false));
  }, []);

  if (user) return <Navigate to="/" replace />;

  const set = (k) => (e) =>
    setF((prev) => ({
      ...prev,
      [k]: e.target.value,
    }));

  const changeMode = (nextMode) => {
    setMode(nextMode);
    setErr('');
    setF({});
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
    <main className="auth-page">

      {/* Background decoration */}
      <div className="auth-orb auth-orb-one" />
      <div className="auth-orb auth-orb-two" />
      <div className="auth-grid-bg" />

      <div className="auth-layout">

        {/* LEFT SIDE - BRANDING */}
        <section className="auth-showcase">

          <div className="brand">
            <div className="brand-icon">💧</div>

            <span>
              Aqua<span className="brand-highlight">Track</span>
            </span>

            <span className="brand-beta">
              SMART WATER
            </span>
          </div>

          <div className="showcase-content">

            <span className="eyebrow">
              <span className="live-dot" />
              INTELLIGENT WATER MANAGEMENT
            </span>

            <h1>
              Every drop
              <br />
              <span>has a story.</span>
            </h1>

            <p className="showcase-description">
              Take control of your water consumption.
              Track usage, monitor expenses and build
              a smarter, more sustainable community.
            </p>

            {/* LOTTIE WATER ANIMATION */}
            <WaterAnimation
              src="/animations/water-drop.lottie"
              className="auth-water-animation"
            />

            <div className="feature-list">

              <div className="feature-item">
                <div className="feature-icon">📊</div>

                <div>
                  <h3>Smart Analytics</h3>
                  <p>
                    Visualize daily usage and consumption trends.
                  </p>
                </div>
              </div>

              <div className="feature-item">
                <div className="feature-icon">🔔</div>

                <div>
                  <h3>Real-time Alerts</h3>
                  <p>
                    Get notified about leaks and unusual usage.
                  </p>
                </div>
              </div>

              <div className="feature-item">
                <div className="feature-icon">💰</div>

                <div>
                  <h3>Transparent Billing</h3>
                  <p>
                    Fair expense sharing and downloadable invoices.
                  </p>
                </div>
              </div>

            </div>

            <div className="showcase-stats">

              <div>
                <strong>24/7</strong>
                <span>Monitoring</span>
              </div>

              <div className="stat-divider" />

              <div>
                <strong>Smart</strong>
                <span>Notifications</span>
              </div>

              <div className="stat-divider" />

              <div>
                <strong>100%</strong>
                <span>Transparency</span>
              </div>

            </div>
          </div>

          <div className="showcase-footer">
            <span>© 2026 AquaTrack</span>
            <span>Water smarter. Live better.</span>
          </div>

        </section>

        {/* RIGHT SIDE - AUTHENTICATION */}
        <section className="auth-card-wrapper">

          <div className="auth-card">

            <div className="mobile-brand">
              <div className="brand-icon">💧</div>

              <span>
                Aqua<span className="brand-highlight">Track</span>
              </span>
            </div>

            <div className="auth-heading">

              <span className="auth-welcome">
                {mode === 'login'
                  ? 'WELCOME BACK'
                  : 'GET STARTED'}
              </span>

              <h2>
                {mode === 'login'
                  ? 'Welcome back!'
                  : mode === 'resident'
                    ? 'Join your community'
                    : 'Create your apartment'}
              </h2>

              <p>
                {mode === 'login'
                  ? 'Sign in to continue to your dashboard.'
                  : 'Fill in your details to get started.'}
              </p>

            </div>

            {/* AUTHENTICATION TABS */}
            <div className="auth-tabs">

              {Object.entries(MODES).map(([key, value]) => (
                <button
                  type="button"
                  key={key}
                  className={mode === key ? 'active' : ''}
                  onClick={() => changeMode(key)}
                >
                  {value}
                </button>
              ))}

            </div>

            {/* GOOGLE LOGIN */}
            {google && mode !== 'admin' && (
              <div className="google-section">

                <a
                  href="/api/oauth2/authorization/google"
                  className="google-login"
                >
                  <GoogleIcon />
                  <span>Continue with Google</span>
                </a>

                <div className="auth-divider">
                  <span>OR CONTINUE WITH EMAIL</span>
                </div>

              </div>
            )}

            {/* AUTHENTICATION FORM */}
            <form onSubmit={submit} className="auth-fields">

              {/* FULL NAME */}
              {mode !== 'login' && (
                <Field label="Full name">
                  <input
                    required
                    placeholder="Enter your full name"
                    autoComplete="name"
                    onChange={set('fullName')}
                  />
                </Field>
              )}

              {/* ADMIN FIELDS */}
              {mode === 'admin' && (
                <>
                  <Field label="Apartment / society name">
                    <input
                      required
                      placeholder="e.g. Green Valley Residency"
                      onChange={set('apartmentName')}
                    />
                  </Field>

                  <Field label="Address (optional)">
                    <input
                      placeholder="Enter apartment address"
                      onChange={set('address')}
                    />
                  </Field>
                </>
              )}

              {/* RESIDENT FIELDS */}
              {mode === 'resident' && (
                <>
                  <Field
                    label="Apartment code"
                    hint="Get this code from your apartment administrator."
                  >
                    <input
                      required
                      placeholder="e.g. AQUA2026"
                      style={{ textTransform: 'uppercase' }}
                      onChange={set('apartmentCode')}
                    />
                  </Field>

                  <Field
                    label="Unit number"
                    hint="Your registered flat number."
                  >
                    <input
                      required
                      placeholder="e.g. A-101"
                      onChange={set('unitNumber')}
                    />
                  </Field>
                </>
              )}

              {/* EMAIL */}
              <Field label="Email address">
                <input
                  type="email"
                  required
                  placeholder="name@example.com"
                  autoComplete="username"
                  onChange={set('email')}
                />
              </Field>

              {/* PASSWORD */}
              <Field
                label="Password"
                hint={mode === 'login' ? '' : 'Minimum 8 characters.'}
              >
                <div className="password-wrapper">

                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    minLength={mode === 'login' ? 1 : 8}
                    placeholder="Enter your password"
                    autoComplete={
                      mode === 'login'
                        ? 'current-password'
                        : 'new-password'
                    }
                    onChange={set('password')}
                  />

                  <button
                    type="button"
                    className="password-toggle"
                    onClick={() =>
                      setShowPassword((prev) => !prev)
                    }
                    aria-label={
                      showPassword ? 'Hide password' : 'Show password'
                    }
                  >
                    {showPassword ? '🙈' : '👁'}
                  </button>

                </div>
              </Field>

              {/* SECURE SIGN-IN */}
              {mode === 'login' && (
                <div className="auth-options">
                  <span className="secure-label">
                    <span>🔒</span> Secure sign-in
                  </span>
                </div>
              )}

              {/* ERROR MESSAGE */}
              <Msg>{err}</Msg>

              {/* SUBMIT BUTTON */}
              <button
                className="auth-submit"
                type="submit"
                disabled={busy}
              >

                {busy ? (
                  <>
                    <span className="auth-loading-lottie">
                      <DotLottieReact
                        src="/animations/water-loading.lottie"
                        loop
                        autoplay
                        style={{
                          width: '100%',
                          height: '100%',
                        }}
                      />
                    </span>

                    Processing...
                  </>
                ) : (
                  <>
                    {MODES[mode]}
                    <span className="submit-arrow">→</span>
                  </>
                )}

              </button>

            </form>

            {/* DEMO CREDENTIALS */}
            {mode === 'login' && (
              <div className="demo-credentials">

                <span className="demo-icon">💡</span>

                <div>
                  <strong>Explore with demo accounts</strong>
                  <p>Admin: admin@demo.com / Admin@123</p>
                  <p>
                    Resident: resident1@demo.com / Resident@123
                  </p>
                </div>

              </div>
            )}

            {/* SECURITY FOOTER */}
            <div className="auth-security">
              <span>🛡️</span>
              Your information is protected with secure authentication.
            </div>

          </div>

        </section>

      </div>
    </main>
  );
}
