import api from '../api.js';
import { Card, Spinner, useLoad } from '../components/ui.jsx';

const ICON = { OVERUSE: '⚠️', LEAK: '🚨', BILL: '🧾' };

export default function Notifications() {
  const { data, loading, reload } = useLoad('/notifications');
  if (loading && !data) return <Spinner />;
  const markAll = async () => { await api.post('/notifications/read-all'); reload(); };
  const mark = async (n) => { if (!n.read) { await api.post(`/notifications/${n.id}/read`); reload(); } };
  return (
    <>
      <h2>Alerts &amp; notifications</h2>
      <Card right={data?.some((n) => !n.read) && <button className="ghost" onClick={markAll}>Mark all read</button>}>
        {!data?.length && <p className="muted">Nothing here yet - you're all caught up. 🎉</p>}
        <ul className="notes">
          {data?.map((n) => (
            <li key={n.id} className={n.read ? '' : 'unread'} onClick={() => mark(n)}>
              <span>{ICON[n.type]}</span>
              <div><p>{n.message}</p><small>{new Date(n.createdAt).toLocaleString()}</small></div>
            </li>
          ))}
        </ul>
      </Card>
    </>
  );
}
