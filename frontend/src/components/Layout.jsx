import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '../auth.jsx';

const resident = [
  ['/', 'Dashboard', '📊'],
  ['/compare', 'Compare', '⚖️'],
  ['/invoices', 'Invoices', '🧾'],
  ['/notifications', 'Alerts', '🔔'],
  ['/profile', 'Profile', '👤'],
];
const admin = [
  ['/admin', 'Overview', '📊'],
  ['/admin/households', 'Households', '🏢'],
  ['/admin/tariff', 'Tariff', '⚙️'],
  ['/admin/purchases', 'Water purchases', '🚚'],
  ['/admin/billing', 'Billing cycles', '🧾'],
  ['/notifications', 'Alerts', '🔔'],
  ['/profile', 'Profile', '👤'],
];

export default function Layout() {
  const { user, logout } = useAuth();
  const nav = useNavigate();
  const [open, setOpen] = useState(false);
  const links = user.role === 'ADMIN' ? admin : resident;

  return (
    <div className="shell">
      <aside className={open ? 'side open' : 'side'}>
        <div className="brand">💧 AquaTrack</div>
        <nav onClick={() => setOpen(false)}>
          {links.map(([to, label, icon]) => (
            <NavLink key={to} to={to} end={to === '/' || to === '/admin'}>
              <span>{icon}</span>{label}
            </NavLink>
          ))}
        </nav>
        <div className="who">
          <strong>{user.fullName}</strong>
          <small>{user.apartmentName}{user.unitNumber ? ` · ${user.unitNumber}` : ''}</small>
          {user.role === 'ADMIN' && <small>Resident sign-up code: <code>{user.apartmentCode}</code></small>}
          <button className="ghost" onClick={() => { logout(); nav('/login'); }}>Sign out</button>
        </div>
      </aside>
      <main>
        <button className="burger" onClick={() => setOpen(!open)} aria-label="Toggle menu">☰</button>
        <Outlet />
      </main>
    </div>
  );
}
