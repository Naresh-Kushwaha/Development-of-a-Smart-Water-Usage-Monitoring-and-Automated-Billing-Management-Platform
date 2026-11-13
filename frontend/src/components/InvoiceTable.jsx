
import { useEffect, useState } from 'react';
import { DotLottieReact } from '@lottiefiles/dotlottie-react';

import api, {
  downloadInvoice,
  errMsg
} from '../api.js';

import { useAuth } from '../auth.jsx';
import { payInvoice } from '../payments.js';

import {
  inr,
  num,
  StatusPill,
  Msg,
  Spinner
} from './ui.jsx';

import './InvoiceTable.css';

export default function InvoiceTable({
  invoices,
  admin = false,
  onChanged
}) {
  const { user } = useAuth();

  const [err, setErr] = useState('');
  const [ok, setOk] = useState('');
  const [payOn, setPayOn] = useState(false);
  const [paying, setPaying] = useState(null);
  const [processingId, setProcessingId] = useState(null);

  // Check whether Razorpay is enabled for resident payments.
  useEffect(() => {
    if (admin) {
      setPayOn(false);
      return;
    }

    let active = true;

    api.get('/payments/config')
      .then((response) => {
        if (active) {
          setPayOn(!!response.data.enabled);
        }
      })
      .catch(() => {
        if (active) {
          setPayOn(false);
        }
      });

    return () => {
      active = false;
    };
  }, [admin]);

  const run = async (fn, successMessage = '') => {
    setErr('');
    setOk('');

    try {
      await fn();

      if (successMessage) {
        setOk(successMessage);
      }

      if (onChanged) {
        await onChanged();
      }
    } catch (error) {
      setErr(errMsg(error));
    }
  };

  // Pay invoice using Razorpay.
  const pay = async (invoice) => {
    setPaying(invoice.id);

    try {
      await run(
        () => payInvoice(invoice.id, user),
        'Payment received successfully! A receipt has been emailed to you.'
      );
    } finally {
      setPaying(null);
    }
  };

  // Update invoice adjustment.
  const adjust = async (invoice) => {
    const amount = window.prompt(
      `Adjustment for ${invoice.unitNumber} (use a negative number for a credit):`,
      invoice.adjustment ?? 0
    );

    if (amount === null || amount.trim() === '') {
      return;
    }

    if (!Number.isFinite(Number(amount))) {
      setErr('Please enter a valid adjustment amount.');
      return;
    }

    const note = window.prompt(
      'Reason / note (optional):',
      invoice.adjustmentNote ?? ''
    );

    if (note === null) {
      return;
    }

    setProcessingId(invoice.id);

    try {
      await run(
        () => api.put(
          `/admin/invoices/${invoice.id}/adjustment`,
          {
            amount: Number(amount),
            note
          }
        ),
        'Invoice adjustment updated successfully.'
      );
    } finally {
      setProcessingId(null);
    }
  };

  // Download invoice PDF.
  const downloadPdf = async (invoice) => {
    setProcessingId(invoice.id);

    try {
      await run(() =>
        downloadInvoice(
          invoice.id,
          `invoice-${invoice.unitNumber}-${invoice.cycleLabel}.pdf`
        )
      );
    } finally {
      setProcessingId(null);
    }
  };

  // Mark invoice as paid from admin panel.
  const markPaid = async (invoice) => {
    const confirmed = window.confirm(
      `Mark invoice for ${invoice.unitNumber} as paid?`
    );

    if (!confirmed) return;

    setProcessingId(invoice.id);

    try {
      await run(
        () => api.put(`/admin/invoices/${invoice.id}/paid`),
        'Invoice marked as paid successfully.'
      );
    } finally {
      setProcessingId(null);
    }
  };

  // Loading state.
  if (invoices === null || invoices === undefined) {
    return (
      <div className="invoice-loading">
        <Spinner />
        <p>Loading invoices...</p>
      </div>
    );
  }

  // Empty state.
  if (!invoices.length) {
    return (
      <div className="invoice-empty-state">

        <div className="invoice-empty-animation">
          <DotLottieReact
            src="/animations/empty-building.lottie"
            loop
            autoplay
          />
        </div>

        <h3>No invoices available</h3>

        <p>
          {admin
            ? 'Invoices will appear here once this billing cycle is finalized or preview data is available.'
            : 'You do not have any invoices available at the moment.'}
        </p>

      </div>
    );
  }

  return (
    <div className="invoice-table-component">

      {/* Feedback messages */}

      {err && (
        <div className="invoice-message">
          <Msg kind="error">{err}</Msg>
        </div>
      )}

      {ok && (
        <div className="invoice-success-message">

          <div className="invoice-success-animation">
            <DotLottieReact
              src="/animations/alert-success.lottie"
              loop={false}
              autoplay
            />
          </div>

          <Msg kind="ok">{ok}</Msg>

        </div>
      )}

      {/* Invoice table */}

      <div className="invoice-table-wrap">

        <table className="invoice-table">

          <thead>
            <tr>

              {admin && <th>Unit</th>}

              <th>Billing Cycle</th>

              <th className="invoice-align-right">
                Used (kL)
              </th>

              <th className="invoice-align-right">
                Water
              </th>

              <th className="invoice-align-right">
                Shared
              </th>

              <th className="invoice-align-right">
                Adjustment
              </th>

              <th className="invoice-align-right">
                Total
              </th>

              <th>Status</th>

              <th>Actions</th>

            </tr>
          </thead>

          <tbody>

            {invoices.map((invoice, index) => {

              const isProcessing =
                processingId === invoice.id ||
                paying === invoice.id;

              return (
                <tr key={invoice.id ?? index}>

                  {admin && (
                    <td>
                      <strong className="invoice-unit">
                        {invoice.unitNumber}
                      </strong>
                    </td>
                  )}

                  <td className="invoice-cycle-cell">
                    {invoice.cycleLabel}
                  </td>

                  <td className="invoice-align-right">
                    {num(invoice.consumptionKl, 2)}
                  </td>

                  <td className="invoice-align-right">
                    {inr(invoice.baseCharge)}
                  </td>

                  <td className="invoice-align-right">
                    {inr(invoice.sharedCharge)}
                  </td>

                  <td
                    className="invoice-align-right"
                    title={invoice.adjustmentNote || ''}
                  >
                    <span
                      className={
                        Number(invoice.adjustment) < 0
                          ? 'invoice-credit'
                          : ''
                      }
                    >
                      {inr(invoice.adjustment)}
                    </span>
                  </td>

                  <td className="invoice-align-right">
                    <strong className="invoice-total">
                      {inr(invoice.total)}
                    </strong>
                  </td>

                  <td>
                    {invoice.id ? (
                      <StatusPill status={invoice.status} />
                    ) : (
                      <span className="pill preview">
                        PREVIEW
                      </span>
                    )}
                  </td>

                  <td>
                    <div className="invoice-actions">

                      {/* Resident payment */}

                      {!admin &&
                        payOn &&
                        invoice.id &&
                        invoice.status === 'UNPAID' && (

                          <button
                            type="button"
                            className="invoice-action-button pay"
                            disabled={isProcessing}
                            onClick={() => pay(invoice)}
                          >
                            {paying === invoice.id
                              ? 'Opening...'
                              : 'Pay now'}
                          </button>

                        )}

                      {/* PDF download */}

                      {invoice.id && (

                        <button
                          type="button"
                          className="invoice-action-button pdf"
                          disabled={isProcessing}
                          onClick={() => downloadPdf(invoice)}
                        >
                          PDF
                        </button>

                      )}

                      {/* Admin adjustment */}

                      {admin && invoice.id && (

                        <button
                          type="button"
                          className="invoice-action-button adjust"
                          disabled={isProcessing}
                          onClick={() => adjust(invoice)}
                        >
                          Adjust
                        </button>

                      )}

                      {/* Admin mark as paid */}

                      {admin &&
                        invoice.id &&
                        invoice.status === 'UNPAID' && (

                          <button
                            type="button"
                            className="invoice-action-button paid"
                            disabled={isProcessing}
                            onClick={() => markPaid(invoice)}
                          >
                            Mark paid
                          </button>

                        )}

                    </div>
                  </td>

                </tr>
              );
            })}

          </tbody>

        </table>

      </div>

    </div>
  );
}
