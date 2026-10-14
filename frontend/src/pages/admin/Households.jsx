import { useState } from 'react';
import api, { errMsg } from '../../api.js';
import { Card, Field, Msg, Spinner, useLoad } from '../../components/ui.jsx';

const EMPTY = { unitNumber: '', areaSqft: '', occupants: 2, meterNumber: '' };

export default function Households() {
  const { data, loading, reload } = useLoad('/admin/households');
  const [f, setF] = useState(EMPTY);
  const [editId, setEditId] = useState(null);
  const [err, setErr] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const save = async (e) => {
    e.preventDefault(); setErr('');
    const body = { ...f, areaSqft: Number(f.areaSqft), occupants: Number(f.occupants) };
    try {
      editId ? await api.put(`/admin/households/${editId}`, body) : await api.post('/admin/households', body);
      setF(EMPTY); setEditId(null); reload();
    } catch (ex) { setErr(errMsg(ex)); }
  };
  const edit = (h) => { setEditId(h.id); setF({ unitNumber: h.unitNumber, areaSqft: h.areaSqft, occupants: h.occupants, meterNumber: h.meterNumber ?? '' }); window.scrollTo({ top: 0, behavior: 'smooth' }); };
  const del = async (h) => {
    if (!confirm(`Delete unit ${h.unitNumber}? Its usage logs and invoices will be removed.`)) return;
    try { await api.delete(`/admin/households/${h.id}`); reload(); } catch (ex) { setErr(errMsg(ex)); }
  };
  const assign = async (h) => {
    const email = prompt(`Assign a registered resident to ${h.unitNumber} (email):`);
    if (!email) return;
    try { await api.post(`/admin/households/${h.id}/assign`, { email }); reload(); } catch (ex) { setErr(errMsg(ex)); }
  };

  return (
    <>
      <h2>Households &amp; meters</h2>
      <Card title={editId ? 'Edit household' : 'Register household'}>
        <form className="inline" onSubmit={save}>
          <Field label="Unit no."><input required value={f.unitNumber} onChange={set('unitNumber')} /></Field>
          <Field label="Area (sq ft)"><input type="number" min="1" step="0.1" required value={f.areaSqft} onChange={set('areaSqft')} /></Field>
          <Field label="Occupants"><input type="number" min="1" required value={f.occupants} onChange={set('occupants')} /></Field>
          <Field label="Meter no." hint="Blank = unmetered (billed by area)"><input value={f.meterNumber} onChange={set('meterNumber')} /></Field>
          <button className="primary">{editId ? 'Update' : 'Add'}</button>
          {editId && <button type="button" className="ghost" onClick={() => { setEditId(null); setF(EMPTY); }}>Cancel</button>}
        </form>
        <Msg>{err}</Msg>
      </Card>
      <Card title={`Units (${data?.length ?? 0})`}>
        {loading && !data ? <Spinner /> : (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Unit</th><th className="r">Area</th><th className="r">People</th><th>Meter</th><th>Residents</th><th></th></tr></thead>
              <tbody>
                {data?.map((h) => (
                  <tr key={h.id}>
                    <td><strong>{h.unitNumber}</strong></td>
                    <td className="r">{h.areaSqft}</td>
                    <td className="r">{h.occupants}</td>
                    <td>{h.metered ? h.meterNumber : <span className="pill preview">UNMETERED</span>}</td>
                    <td>{h.residents.join(', ') || <span className="muted">—</span>}</td>
                    <td className="actions">
                      <button className="link" onClick={() => assign(h)}>Assign resident</button>
                      <button className="link" onClick={() => edit(h)}>Edit</button>
                      <button className="link danger" onClick={() => del(h)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  );
}
