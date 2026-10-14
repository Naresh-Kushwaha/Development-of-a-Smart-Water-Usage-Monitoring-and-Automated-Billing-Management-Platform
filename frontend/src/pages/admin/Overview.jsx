import { useRef, useState } from 'react';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import api, { errMsg } from '../../api.js';
import { Card, daysAgo, iso, Msg, num, Spinner, Stat, useLoad } from '../../components/ui.jsx';

export default function Overview() {
  const [range, setRange] = useState({ from: daysAgo(29), to: iso(new Date()) });
  const { data, error, loading, reload } = useLoad(`/admin/usage/comparison?from=${range.from}&to=${range.to}`);
  const households = useLoad('/admin/households');
  const fileRef = useRef();
  const [csv, setCsv] = useState(null);
  const [alertMsg, setAlertMsg] = useState('');
  const [err, setErr] = useState('');

  const upload = async (e) => {
    e.preventDefault();
    const file = fileRef.current.files[0];
    if (!file) return;
    const fd = new FormData();
    fd.append('file', file);
    setErr(''); setCsv(null);
    try { const { data } = await api.post('/admin/usage/upload', fd); setCsv(data); reload(); }
    catch (ex) { setErr(errMsg(ex)); }
  };

  const runAlerts = async () => {
    try {
      const { data } = await api.post('/admin/alerts/run');
      setAlertMsg(`Check complete: ${data.overuse} new overuse alert(s), ${data.anomalies} anomaly flag(s).`);
    } catch (ex) { setErr(errMsg(ex)); }
  };

  const template = () => {
    const rows = (households.data || []).filter((h) => h.metered).map((h) => `${h.unitNumber},${iso(new Date())},`);
    const blob = new Blob([`unit_number,date,litres\n${rows.join('\n')}\n`], { type: 'text/csv' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'meter-readings-template.csv';
    a.click();
  };

  const total = (data || []).reduce((s, u) => s + Number(u.kl), 0);
  const top = (data || []).slice().sort((a, b) => Number(b.kl) - Number(a.kl))[0];

  return (
    <>
      <h2>Apartment overview</h2>
      <Card title="Usage by household" right={
        <div className="inline">
          <input type="date" value={range.from} onChange={(e) => setRange({ ...range, from: e.target.value })} />
          <input type="date" value={range.to} onChange={(e) => setRange({ ...range, to: e.target.value })} />
        </div>}>
        <Msg>{error}</Msg>
        {loading && !data ? <Spinner /> : (
          <>
            <div className="grid stats">
              <Stat label="Total metered" value={`${num(total, 1)} kL`} />
              <Stat label="Households" value={data?.length ?? 0} />
              <Stat label="Highest user" value={top ? top.unitNumber : '—'} sub={top ? `${num(top.kl, 1)} kL` : ''} />
            </div>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={data}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="unitNumber" fontSize={12} />
                <YAxis fontSize={12} />
                <Tooltip />
                <Legend />
                <Bar dataKey="kl" name="Consumption (kL)" fill="#06b6d4" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </>
        )}
      </Card>

      <div className="grid two">
        <Card title="Upload meter readings (CSV)">
          <form className="stack" onSubmit={upload}>
            <input ref={fileRef} type="file" accept=".csv,text/csv" required />
            <small className="muted">Columns: <code>unit_number,date,litres</code> - one row per unit per day. Duplicates are skipped.</small>
            <div className="row">
              <button className="primary">Upload</button>
              <button type="button" className="ghost" onClick={template}>Download template</button>
            </div>
          </form>
          {csv && (
            <Msg kind={csv.errors.length ? 'warn' : 'ok'}>
              Imported {csv.imported}, skipped {csv.duplicates} duplicate(s){csv.errors.length ? `, ${csv.errors.length} error(s):` : '.'}
              {csv.errors.slice(0, 8).map((x) => <div key={x}>• {x}</div>)}
            </Msg>
          )}
        </Card>
        <Card title="Alerts &amp; anomaly detection">
          <p className="muted">Checks run automatically every morning. Run them now after uploading new data - overuse alerts, leak flags (usage &gt; 2σ above a household's average) and admin reports go out by email.</p>
          <button className="primary" onClick={runAlerts}>Run checks now</button>
          <Msg kind="ok">{alertMsg}</Msg>
        </Card>
      </div>
      <Msg>{err}</Msg>
    </>
  );
}
