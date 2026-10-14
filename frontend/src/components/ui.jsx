import { useCallback, useEffect, useState } from 'react';
import api, { errMsg } from '../api.js';

export const inr = (n) =>
  '₹' + Number(n ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
export const num = (n, d = 1) => Number(n ?? 0).toLocaleString('en-IN', { maximumFractionDigits: d });
export const iso = (d) => d.toISOString().slice(0, 10);
export const daysAgo = (n) => { const d = new Date(); d.setDate(d.getDate() - n); return iso(d); };

export function Card({ title, right, children, className = '' }) {
  return (
    <section className={`card ${className}`}>
      {(title || right) && (
        <header className="card-h">
          <h3>{title}</h3>
          {right}
        </header>
      )}
      {children}
    </section>
  );
}

export function Stat({ label, value, sub, tone }) {
  return (
    <div className={`stat ${tone || ''}`}>
      <span className="stat-l">{label}</span>
      <strong>{value}</strong>
      {sub && <small>{sub}</small>}
    </div>
  );
}

export function Msg({ kind = 'error', children }) {
  if (!children) return null;
  return <div className={`msg ${kind}`} role={kind === 'error' ? 'alert' : 'status'}>{children}</div>;
}

export function Spinner() { return <div className="spinner" aria-label="Loading" />; }

/** Loads data from an API path; returns { data, error, loading, reload }. */
export function useLoad(path, deps = []) {
  const [state, set] = useState({ data: null, error: '', loading: true });
  const load = useCallback(() => {
    set((s) => ({ ...s, loading: true }));
    api.get(path)
      .then((r) => set({ data: r.data, error: '', loading: false }))
      .catch((e) => set({ data: null, error: errMsg(e), loading: false }));
  }, [path]);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(load, [load, ...deps]);
  return { ...state, reload: load };
}

export function Field({ label, hint, children }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {hint && <small>{hint}</small>}
    </label>
  );
}

export const StatusPill = ({ status }) => <span className={`pill ${String(status).toLowerCase()}`}>{status}</span>;
