import { createContext, useContext, useState } from 'react';
import api from './api.js';

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('user')); } catch { return null; }
  });
  const login = ({ token, user }) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    setUser(user);
  };
  const logout = () => {
    // Revoke the token on the server (kept in Redis until it expires), then forget it locally.
    // The header is passed explicitly because localStorage is cleared right below.
    const token = localStorage.getItem('token');
    if (token) api.post('/auth/logout', null, { headers: { Authorization: `Bearer ${token}` } }).catch(() => {});
    localStorage.clear();
    setUser(null);
  };
  return <Ctx.Provider value={{ user, login, logout }}>{children}</Ctx.Provider>;
}
