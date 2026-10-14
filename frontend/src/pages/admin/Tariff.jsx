import { useEffect, useState } from 'react';
import api, { errMsg } from '../../api.js';
import { Card, Field, Msg, Spinner, useLoad } from '../../components/ui.jsx';

export default function Tariff() {
  const { data, loading } = useLoad('/admin/tariff');
  const [f, setF] = useState(null);
  const [msg, setMsg] = useState({ kind: '', text: '' });
  useEffect(() => { if (data) setF(data); }, [data]);
  if (loading || !f) return <Spinner />;
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const save = async (e) => {
    e.preventDefault();
    const n = (k) => Number(f[k]);
    try {
      await api.put('/admin/tariff', { ...f, tier1LimitKl: n('tier1LimitKl'), tier1Rate: n('tier1Rate'), tier2Rate: n('tier2Rate'), fixedCharge: n('fixedCharge'), alertThresholdKl: n('alertThresholdKl') });
      setMsg({ kind: 'ok', text: 'Tariff saved. It applies to cycle calculations from now on.' });
    } catch (ex) { setMsg({ kind: 'error', text: errMsg(ex) }); }
  };

  return (
    <>
      <h2>Tariff configuration</h2>
      <Card>
        <form className="form-grid" onSubmit={save}>
          <Field label="Tier-1 limit (kL per cycle)" hint="Base rate applies up to this volume"><input type="number" step="0.01" min="0.01" required value={f.tier1LimitKl} onChange={set('tier1LimitKl')} /></Field>
          <Field label="Tier-1 rate (₹ / kL)"><input type="number" step="0.01" min="0" required value={f.tier1Rate} onChange={set('tier1Rate')} /></Field>
          <Field label="Tier-2 rate (₹ / kL)" hint="Applied beyond the tier-1 limit"><input type="number" step="0.01" min="0" required value={f.tier2Rate} onChange={set('tier2Rate')} /></Field>
          <Field label="Fixed charge per household (₹)" hint="Used in Tiered mode"><input type="number" step="0.01" min="0" required value={f.fixedCharge} onChange={set('fixedCharge')} /></Field>
          <Field label="Overuse alert threshold (kL / month)"><input type="number" step="0.01" min="0.01" required value={f.alertThresholdKl} onChange={set('alertThresholdKl')} /></Field>
          <Field label="Shared-area split" hint="Garden, pool, lobby usage">
            <select value={f.sharedMethod} onChange={set('sharedMethod')}><option value="AREA">By flat size</option><option value="OCCUPANCY">By occupancy</option></select>
          </Field>
          <Field label="Billing mode" hint="Tiered = rate card per household. Cost sharing = split what you paid for bulk water by consumption (unmetered units fall back to flat area).">
            <select value={f.billingMode} onChange={set('billingMode')}><option value="TIERED">Tiered tariff</option><option value="COST_SHARING">Cost sharing (proportional)</option></select>
          </Field>
          <div className="span"><Msg kind={msg.kind}>{msg.text}</Msg><button className="primary">Save tariff</button></div>
        </form>
      </Card>
    </>
  );
}
