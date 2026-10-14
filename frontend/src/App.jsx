import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import Layout from './components/Layout.jsx';
import Auth from './pages/Auth.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Compare from './pages/Compare.jsx';
import Invoices from './pages/Invoices.jsx';
import Notifications from './pages/Notifications.jsx';
import Profile from './pages/Profile.jsx';
import Overview from './pages/admin/Overview.jsx';
import Households from './pages/admin/Households.jsx';
import Tariff from './pages/admin/Tariff.jsx';
import Purchases from './pages/admin/Purchases.jsx';
import Billing from './pages/admin/Billing.jsx';

function Guard({ role, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/'} replace />;
  return children;
}

function Home() {
  const { user } = useAuth();
  return user.role === 'ADMIN' ? <Navigate to="/admin" replace /> : <Dashboard />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Auth />} />
      <Route element={<Guard><Layout /></Guard>}>
        <Route path="/" element={<Home />} />
        <Route path="/compare" element={<Guard role="RESIDENT"><Compare /></Guard>} />
        <Route path="/invoices" element={<Guard role="RESIDENT"><Invoices /></Guard>} />
        <Route path="/notifications" element={<Notifications />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/admin" element={<Guard role="ADMIN"><Overview /></Guard>} />
        <Route path="/admin/households" element={<Guard role="ADMIN"><Households /></Guard>} />
        <Route path="/admin/tariff" element={<Guard role="ADMIN"><Tariff /></Guard>} />
        <Route path="/admin/purchases" element={<Guard role="ADMIN"><Purchases /></Guard>} />
        <Route path="/admin/billing" element={<Guard role="ADMIN"><Billing /></Guard>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
