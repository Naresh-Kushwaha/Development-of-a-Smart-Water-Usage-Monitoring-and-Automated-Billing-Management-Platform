import { useState } from 'react';
import { Bar, BarChart, CartesianGrid, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import api, { errMsg } from '../api.js';
import InvoiceTable from '../components/InvoiceTable.jsx';
import { Card, Field, inr, iso, Msg, num, Spinner, Stat, useLoad } from '../components/ui.jsx';

export default function Dashboard() {
  const { data, error, loading, reload } = useLoad('/dashboard');
  const [form, setForm] = useState({ date: iso(new Date()), litres: '' });
  const [msg, setMsg] = useState({ kind: '', text: '' });

  if (loading && !data) return <Spinner />;
  if (error) return <Msg>{error}</Msg>;

  const submit = async (e) => {
    e.preventDefault();
    try {
      await api.post('/usage', { date: form.date, litres: Number(form.litres) });
      setMsg({ kind: 'ok', text: 'Reading saved.' });
      setForm({ ...form, litres: '' });
      reload();
    } catch (ex) { setMsg({ kind: 'error', text: errMsg(ex) }); }
  };

  const last = data.daily[data.daily.length - 1];
  const pct = data.thresholdKl ? Math.min(100, (Number(data.monthToDateKl) / Number(data.thresholdKl)) * 100) : 0;
  const c = data.cycle;

  return (
    <>
      <h2>Hello, unit {data.unitNumber} 👋</h2>
      <div className="grid stats">
        <Stat label="Latest reading" value={last ? `${num(last.litres, 0)} L` : '—'} sub={last?.date} />
        <Stat label="This month" value={`${num(data.monthToDateKl, 2)} kL`} sub={`Alert at ${num(data.thresholdKl, 1)} kL`} tone={pct >= 100 ? 'bad' : pct >= 80 ? 'warn' : ''} />
        <Stat label="Current cycle" value={c ? `${num(c.consumptionKl, 2)} kL` : '—'} sub={c?.label ?? 'No open cycle'} />
        <Stat label="Bill preview" value={c ? inr(c.estimatedTotal) : '—'} sub={c ? `Water ${inr(c.estimatedBase)} + shared ${inr(c.estimatedShared)}` : ''} />
      </div>

      <div className="meter" title={`${pct.toFixed(0)}% of your monthly alert threshold`}><i style={{ width: pct + '%' }} className={pct >= 100 ? 'bad' : pct >= 80 ? 'warn' : ''} /></div>

      <div className="grid two">
        <Card title="Daily consumption (last 30 days, litres)">
          <ResponsiveContainer width="100%" height={260}>
            <LineChart data={data.daily}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tickFormatter={(d) => d.slice(5)} fontSize={12} />
              <YAxis fontSize={12} />
              <Tooltip />
              <Line type="monotone" dataKey="litres" stroke="#0e7490" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </Card>
        <Card title="Monthly consumption (kL)">
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={data.monthly}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="month" fontSize={12} />
              <YAxis fontSize={12} />
              <Tooltip />
              {data.thresholdKl && <ReferenceLine y={Number(data.thresholdKl)} stroke="#dc2626" strokeDasharray="4 4" label={{ value: 'alert', fontSize: 11, fill: '#dc2626' }} />}
              <Bar dataKey="kl" fill="#06b6d4" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </Card>
      </div>

      <div className="grid two">
        <Card title="Log today's meter usage">
          <form className="inline" onSubmit={submit}>
            <Field label="Date"><input type="date" max={iso(new Date())} required value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} /></Field>
            <Field label="Litres used that day"><input type="number" min="0" step="0.1" required value={form.litres} onChange={(e) => setForm({ ...form, litres: e.target.value })} /></Field>
            <button className="primary">Save reading</button>
          </form>
          <Msg kind={msg.kind}>{msg.text}</Msg>
        </Card>
        <Card title="💡 Water-saving tips">
          <ul className="tips">{data.tips.map((t) => <li key={t}>{t}</li>)}</ul>
        </Card>
      </div>

      <Card title="Invoice history"><InvoiceTable invoices={data.invoices} /></Card>
    </>
  );
}
