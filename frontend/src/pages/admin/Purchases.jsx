import { useState } from 'react';
import api, { errMsg } from '../../api.js';
import { Card, Field, inr, iso, Msg, num, Spinner, useLoad } from '../../components/ui.jsx';

export default function Purchases() {
  const { data, loading, reload } = useLoad('/admin/purchases');
  const [f, setF] = useState({ date: iso(new Date()), source: 'Tanker delivery', volumeKl: '', unitCost: '', notes: '' });
  const [err, setErr] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const add = async (e) => {
    e.preventDefault(); setErr('');
    try {
      await api.post('/admin/purchases', { ...f, volumeKl: Number(f.volumeKl), unitCost: Number(f.unitCost) });
      setF({ ...f, volumeKl: '', unitCost: '', notes: '' }); reload();
    } catch (ex) { setErr(errMsg(ex)); }
  };
  const del = async (p) => { if (confirm('Delete this purchase?')) { await api.delete(`/admin/purchases/${p.id}`); reload(); } };

  return (
    <>
      <h2>Bulk water purchases</h2>
      <Card title="Record a purchase">
        <form className="inline" onSubmit={add}>
          <Field label="Date"><input type="date" max={iso(new Date())} required value={f.date} onChange={set('date')} /></Field>
          <Field label="Source"><input required list="sources" value={f.source} onChange={set('source')} /><datalist id="sources"><option>Tanker delivery</option><option>Municipal supply</option><option>Borewell</option></datalist></Field>
          <Field label="Volume (kL)"><input type="number" min="0.01" step="0.01" required value={f.volumeKl} onChange={set('volumeKl')} /></Field>
          <Field label="Cost per kL (₹)"><input type="number" min="0" step="0.01" required value={f.unitCost} onChange={set('unitCost')} /></Field>
          <Field label="Notes"><input value={f.notes} onChange={set('notes')} /></Field>
          <button className="primary">Add</button>
        </form>
        <Msg>{err}</Msg>
      </Card>
      <Card title="History">
        {loading && !data ? <Spinner /> : (
          <div className="table-wrap"><table>
            <thead><tr><th>Date</th><th>Source</th><th className="r">Volume (kL)</th><th className="r">₹ / kL</th><th className="r">Total</th><th>Notes</th><th></th></tr></thead>
            <tbody>{data?.map((p) => (
              <tr key={p.id}><td>{p.date}</td><td>{p.source}</td><td className="r">{num(p.volumeKl, 2)}</td><td className="r">{inr(p.unitCost)}</td><td className="r"><strong>{inr(p.totalCost)}</strong></td><td>{p.notes}</td><td className="actions"><button className="link danger" onClick={() => del(p)}>Delete</button></td></tr>
            ))}</tbody>
          </table></div>
        )}
      </Card>
    </>
  );
}
