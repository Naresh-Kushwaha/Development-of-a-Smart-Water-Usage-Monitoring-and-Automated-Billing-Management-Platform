import { useState } from 'react';
import api, { downloadInvoice, errMsg } from '../api.js';
import { inr, num, StatusPill, Msg } from './ui.jsx';

export default function InvoiceTable({ invoices, admin = false, onChanged }) {
  const [err, setErr] = useState('');
  if (!invoices?.length) return <p className="muted">No invoices yet.</p>;

  const run = async (fn) => {
    setErr('');
    try { await fn(); onChanged?.(); 

    }
     catch (e) { setErr(errMsg(e)); 
      
     }
  };
  const adjust = (i) => {
    const amount = prompt(`Adjustment for ${i.unitNumber} (use a negative number for a credit):`, i.adjustment ?? 0);
    if (amount === null || isNaN(Number(amount))) return;
    const note = prompt('Reason / note (optional):', i.adjustmentNote ?? '') ?? '';
    run(() => api.put(`/admin/invoices/${i.id}/adjustment`, { amount: Number(amount), note }));
  };

  return (
    <>
      <Msg>{err}</Msg>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              {admin && <th>Unit</th>}
              <th>Cycle</th><th className="r">Used (kL)</th><th className="r">Water</th>
              <th className="r">Shared</th><th className="r">Adj.</th><th className="r">Total</th>
              <th>Status</th><th></th>
            </tr>
          </thead>
          <tbody>
            {invoices.map((i, idx) => (
              <tr key={i.id ?? idx}>
                {admin && <td><strong>{i.unitNumber}</strong></td>}
                <td>{i.cycleLabel}</td>
                <td className="r">{num(i.consumptionKl, 2)}</td>
                <td className="r">{inr(i.baseCharge)}</td>
                <td className="r">{inr(i.sharedCharge)}</td>
                <td className="r" title={i.adjustmentNote || ''}>{inr(i.adjustment)}</td>
                <td className="r"><strong>{inr(i.total)}</strong></td>
                <td>{i.id ? <StatusPill status={i.status} /> : <span className="pill preview">PREVIEW</span>}</td>
                <td className="actions">
                  {i.id && <button className="link" onClick={() => run(() => downloadInvoice(i.id, `invoice-${i.unitNumber}-${i.cycleLabel}.pdf`))}>PDF</button>}
                  {admin && i.id && <button className="link" onClick={() => adjust(i)}>Adjust</button>}
                  {admin && i.id && i.status === 'UNPAID' && <button className="link" onClick={() => run(() => api.put(`/admin/invoices/${i.id}/paid`))}>Mark paid</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
