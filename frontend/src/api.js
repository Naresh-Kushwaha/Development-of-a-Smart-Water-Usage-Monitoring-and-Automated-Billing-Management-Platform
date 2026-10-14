import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((c) => {
  const t = localStorage.getItem('token');
  if (t) c.headers.Authorization = `Bearer ${t}`;
  return c;
});

api.interceptors.response.use(
  (r) => r,
  (e) => {
    if (e.response?.status === 401 && !location.pathname.startsWith('/login')) {
      localStorage.clear();
      location.href = '/login';
    }
    return Promise.reject(e);
  }
);

export const errMsg = (e) => e.response?.data?.message || e.message || 'Something went wrong';

export async function downloadInvoice(id, name = 'invoice.pdf') {
  const r = await api.get(`/invoices/${id}/pdf`, { responseType: 'blob' });
  const url = URL.createObjectURL(r.data);
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  URL.revokeObjectURL(url);
}

export default api;
