
import { useState } from 'react';
import { DotLottieReact } from '@lottiefiles/dotlottie-react';
import api, { errMsg } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Card, Field, Msg } from '../components/ui.jsx';
import './Profile.css';

export default function Profile() {
  const { user, login } = useAuth();

  const [name, setName] = useState(user?.fullName || '');
  const [pw, setPw] = useState('');
  const [msg, setMsg] = useState({ kind: '', text: '' });
  const [saving, setSaving] = useState(false);

  const save = async (e) => {
    e.preventDefault();
    setMsg({ kind: '', text: '' });

    if (pw && pw.length < 8) {
      setMsg({
        kind: 'error',
        text: 'Password must contain at least 8 characters.'
      });
      return;
    }

    try {
      setSaving(true);

      const { data } = await api.put('/auth/me', {
        fullName: name.trim(),
        newPassword: pw || null
      });

      login({
        token: localStorage.getItem('token'),
        user: data
      });

      setPw('');
      setMsg({
        kind: 'ok',
        text: 'Your profile has been updated successfully.'
      });
    } catch (ex) {
      setMsg({
        kind: 'error',
        text: errMsg(ex)
      });
    } finally {
      setSaving(false);
    }
  };

  const initials = (user?.fullName || 'User')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();

  return (
    <div className="profile-page">
      <section className="profile-hero">
        <div className="profile-hero-content">
          <span className="profile-eyebrow">
            ACCOUNT SETTINGS
          </span>

          <h2>My Profile</h2>

          <p>
            Manage your personal information and keep your account secure.
          </p>

          <div className="profile-hero-badges">
            <span>✓ Account active</span>
            <span>🔒 Secure profile</span>
          </div>
        </div>

        <div className="profile-hero-animation">
          <DotLottieReact
            src="/animations/water-drop.lottie"
            loop
            autoplay
          />
        </div>
      </section>

      <div className="profile-layout">
        <aside className="profile-sidebar">
          <Card>
            <div className="profile-user-card">
              <div className="profile-avatar">
                {initials}
              </div>

              <h3>{user?.fullName || 'User'}</h3>
              <p>{user?.email}</p>

              <span className="profile-role">
                {user?.role || 'Platform User'}
              </span>
            </div>

            <div className="profile-sidebar-divider" />

            <div className="profile-info-item">
              <span className="profile-info-icon">✉</span>
              <div>
                <small>Email address</small>
                <strong>{user?.email}</strong>
              </div>
            </div>

            <div className="profile-info-item">
              <span className="profile-info-icon">✓</span>
              <div>
                <small>Account status</small>
                <strong className="profile-active">Active</strong>
              </div>
            </div>
          </Card>
        </aside>

        <main className="profile-main">
          <Card>
            <div className="profile-section-heading">
              <div>
                <span className="profile-section-label">
                  PERSONAL INFORMATION
                </span>
                <h3>Basic details</h3>
                <p>Update the information associated with your account.</p>
              </div>

              <span className="profile-section-icon">👤</span>
            </div>

            <form className="stack profile-form" onSubmit={save}>
              <Field label="Email address" hint="Your registered email address">
                <input
                  type="email"
                  value={user?.email || ''}
                  disabled
                />
              </Field>

              <Field label="Full name">
                <input
                  type="text"
                  required
                  maxLength={100}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Enter your full name"
                />
              </Field>

              <div className="profile-form-divider" />

              <div className="profile-section-heading security-heading">
                <div>
                  <span className="profile-section-label">
                    ACCOUNT SECURITY
                  </span>
                  <h3>Change password</h3>
                  <p>Leave this field empty if you don't want to change it.</p>
                </div>

                <span className="profile-section-icon">🔐</span>
              </div>

              <Field
                label="New password"
                hint="Use at least 8 characters for better security."
              >
                <input
                  type="password"
                  minLength={8}
                  value={pw}
                  onChange={(e) => setPw(e.target.value)}
                  autoComplete="new-password"
                  placeholder="Enter a new password"
                />
              </Field>

              <Msg kind={msg.kind}>{msg.text}</Msg>

              <div className="profile-form-actions">
                <button
                  type="submit"
                  className="primary profile-save-btn"
                  disabled={saving || !name.trim()}
                >
                  {saving ? 'Saving changes...' : 'Save changes'}
                </button>
              </div>
            </form>
          </Card>

          <div className="profile-security-note">
            <span className="profile-security-icon">🛡️</span>
            <div>
              <strong>Your account security matters</strong>
              <p>
                Never share your password or authentication token with anyone.
              </p>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
