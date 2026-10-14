import { useEffect, useState } from 'react';
import api, { errMsg } from '../../api.js';
import InvoiceTable from '../../components/InvoiceTable.jsx';
import { Card, Field, iso, Msg, Spinner, StatusPill, useLoad } from '../../components/ui.jsx';

export default function Billing() {
  const { data: cycles, loading, reload } = useLoad('/admin/cycles');
  const [sel, setSel] = useState(null);
  const [invoices, setInvoices] = useState(null);
  const [err, setErr] = useState('');
  const now = new Date();
  const first = new Date(now.getFullYear(), now.getMonth(), 1);
  const last = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  const [f, setF] = useState({ label: now.toLocaleString('en', { month: 'long', year: 'numeric' }), startDate: iso(new Date(first.getTime() - first.getTimezoneOffset() * 60000)), endDate: iso(new Date(last.getTime() - last.getTimezoneOffset() * 60000)), sharedAreaKl: 0 });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const loadInvoices = (id) => api.get(`/admin/cycles/${id}/invoices`).then((r) => setInvoices(r.data)).catch((e) => setErr(errMsg(e)));
  useEffect(() => { if (sel) loadInvoices(sel); else setInvoices(null); }, [sel]);
  useEffect(() => { if (!sel && cycles?.length) setSel(cycles[0].id); }, [cycles]);

  const act = async (fn) => { setErr(''); try { await fn(); await reload(); if (sel) loadInvoices(sel); } catch (e) { setErr(errMsg(e)); } };
  const open = (e) => { e.preventDefault(); act(async () => { const { data } = await api.post('/admin/cycles', { ...f, sharedAreaKl: Number(f.sharedAreaKl) }); setSel(data.id); }); };
  const current = cycles?.find((c) => c.id === sel);

  return (
    <>
      <h2>Billing cycles</h2>
      <Card title="Open a new cycle">
        <form className="inline" onSubmit={open}>
          <Field label="Label"><input required value={f.label} onChange={set('label')} /></Field>
          <Field label="Start"><input type="date" required value={f.startDate} onChange={set('startDate')} /></Field>
          <Field label="End"><input type="date" required value={f.endDate} onChange={set('endDate')} /></Field>
          <Field label="Shared-area usage (kL)" hint="Garden, pool, lobby"><input type="number" min="0" step="0.01" value={f.sharedAreaKl} onChange={set('sharedAreaKl')} /></Field>
          <button className="primary">Open cycle</button>
        </form>
      </Card>
      <Msg>{err}</Msg>
      <Card title="Cycles">
        {loading && !cycles ? <Spinner /> : !cycles?.length ? <p className="muted">No cycles yet.</p> : (
          <div className="table-wrap"><table>
            <thead><tr><th>Label</th><th>Period</th><th className="r">Shared (kL)</th><th>Status</th><th></th></tr></thead>
            <tbody>{cycles.map((c) => (
              <tr key={c.id} className={c.id === sel ? 'sel' : ''}>
                <td><strong>{c.label}</strong></td><td>{c.startDate} → {c.endDate}</td><td className="r">{c.sharedAreaKl}</td><td><StatusPill status={c.status} /></td>
                <td className="actions">
                  <button className="link" onClick={() => setSel(c.id)}>View</button>
                  {c.status === 'OPEN' && <button className="link" onClick={() => confirm(`Finalize ${c.label}? Invoices will be generated and residents notified.`) && act(() => api.post(`/admin/cycles/${c.id}/finalize`))}>Finalize</button>}
                  {c.status === 'FINALIZED' && <button className="link" onClick={() => act(() => api.post(`/admin/cycles/${c.id}/archive`))}>Archive</button>}
                </td>
              </tr>
            ))}</tbody>
          </table></div>
        )}
      </Card>
      {current && (
        <Card title={`${current.label} - ${current.status === 'OPEN' ? 'live preview (not yet invoiced)' : 'invoices'}`}>
          <InvoiceTable invoices={invoices} admin onChanged={() => loadInvoices(sel)} />
        </Card>
      )}
    </>
  );
}
