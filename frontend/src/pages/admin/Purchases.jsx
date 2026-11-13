
import { useMemo, useState } from 'react';
import { DotLottieReact } from '@lottiefiles/dotlottie-react';

import api, { errMsg } from '../../api.js';
import {
  Card,
  Field,
  inr,
  iso,
  Msg,
  num,
  Spinner,
  useLoad
} from '../../components/ui.jsx';

import './Purchases.css';

const EMPTY_FORM = {
  date: iso(new Date()),
  source: 'Tanker delivery',
  volumeKl: '',
  unitCost: '',
  notes: ''
};

export default function Purchases() {
  const { data, loading, reload } = useLoad('/admin/purchases');

  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState(null);

  const [message, setMessage] = useState({
    kind: '',
    text: ''
  });

  const purchases = Array.isArray(data) ? data : [];

  const summary = useMemo(() => {
    return purchases.reduce(
      (result, purchase) => ({
        volume: result.volume + Number(purchase.volumeKl || 0),
        expenditure: result.expenditure + Number(purchase.totalCost || 0),
        count: result.count + 1
      }),
      {
        volume: 0,
        expenditure: 0,
        count: 0
      }
    );
  }, [purchases]);

  const averageCost = summary.volume > 0
    ? summary.expenditure / summary.volume
    : 0;

  const updateField = (field) => (event) => {
    setForm((previous) => ({
      ...previous,
      [field]: event.target.value
    }));

    setMessage({ kind: '', text: '' });
  };

  const addPurchase = async (event) => {
    event.preventDefault();

    setSaving(true);
    setMessage({ kind: '', text: '' });

    try {
      const payload = {
        ...form,
        volumeKl: Number(form.volumeKl),
        unitCost: Number(form.unitCost)
      };

      await api.post('/admin/purchases', payload);

      setForm({
        ...EMPTY_FORM,
        date: iso(new Date()),
        source: form.source
      });

      setMessage({
        kind: 'ok',
        text: 'Water purchase recorded successfully!'
      });

      await reload();
    } catch (error) {
      setMessage({
        kind: 'error',
        text: errMsg(error)
      });
    } finally {
      setSaving(false);
    }
  };

  const deletePurchase = async (purchase) => {
    const confirmed = window.confirm(
      `Are you sure you want to delete the purchase from ${purchase.date}?`
    );

    if (!confirmed) return;

    setDeletingId(purchase.id);
    setMessage({ kind: '', text: '' });

    try {
      await api.delete(`/admin/purchases/${purchase.id}`);

      setMessage({
        kind: 'ok',
        text: 'Purchase deleted successfully.'
      });

      await reload();
    } catch (error) {
      setMessage({
        kind: 'error',
        text: errMsg(error)
      });
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div className="purchases-page">

      {/* =====================================
          HERO SECTION
      ====================================== */}

      <section className="purchases-hero">

        <div className="purchases-hero-content">

          <div className="purchases-eyebrow">
            <span className="purchases-status-dot" />
            WATER RESOURCE MANAGEMENT
          </div>

          <h1>
            Bulk Water <span>Purchases</span>
          </h1>

          <p>
            Track tanker deliveries, municipal supply and other
            water purchases. Keep your water procurement and
            expenditure organized.
          </p>

          <div className="purchases-hero-tags">
            <span>Purchase Tracking</span>
            <span>Expense Management</span>
            <span>Water Inventory</span>
          </div>

        </div>

        <div className="purchases-hero-animation">
          <DotLottieReact
            src="/animations/water-wave.lottie"
            loop
            autoplay
          />
        </div>

      </section>

      {/* =====================================
          SUMMARY CARDS
      ====================================== */}

      <section className="purchases-stats">

        <div className="purchase-stat-card">
          <div className="purchase-stat-icon blue">
            <DotLottieReact
              src="/animations/water-drop.lottie"
              loop
              autoplay
            />
          </div>

          <div className="purchase-stat-content">
            <p>Total Water Purchased</p>
            <h3>{num(summary.volume, 2)} kL</h3>
            <span>Across recorded purchases</span>
          </div>
        </div>

        <div className="purchase-stat-card">
          <div className="purchase-stat-icon purple">
            <span>₹</span>
          </div>

          <div className="purchase-stat-content">
            <p>Total Expenditure</p>
            <h3>{inr(summary.expenditure)}</h3>
            <span>Total procurement cost</span>
          </div>
        </div>

        <div className="purchase-stat-card">
          <div className="purchase-stat-icon green">
            <span>▤</span>
          </div>

          <div className="purchase-stat-content">
            <p>Purchase Records</p>
            <h3>{summary.count}</h3>
            <span>Total transactions</span>
          </div>
        </div>

        <div className="purchase-stat-card">
          <div className="purchase-stat-icon orange">
            <span>↗</span>
          </div>

          <div className="purchase-stat-content">
            <p>Average Cost / kL</p>
            <h3>{inr(averageCost)}</h3>
            <span>Weighted average cost</span>
          </div>
        </div>

      </section>

      {/* =====================================
          MAIN CONTENT
      ====================================== */}

      <div className="purchases-main-grid">

        {/* PURCHASE FORM */}

        <section className="purchases-form-section">

          <Card>

            <div className="purchases-section-heading">

              <div>
                <span className="purchases-section-label">
                  NEW TRANSACTION
                </span>

                <h2>Record Water Purchase</h2>

                <p>
                  Enter the details of your latest water delivery
                  or supply transaction.
                </p>
              </div>

              <div className="purchases-heading-icon">
                +
              </div>

            </div>

            <form
              className="purchases-form"
              onSubmit={addPurchase}
            >

              <div className="purchases-form-grid">

                <Field label="Purchase Date">
                  <input
                    type="date"
                    max={iso(new Date())}
                    required
                    value={form.date}
                    onChange={updateField('date')}
                  />
                </Field>

                <Field
                  label="Water Source"
                  hint="Select or enter a supply source."
                >
                  <>
                    <input
                      list="purchase-sources"
                      required
                      value={form.source}
                      onChange={updateField('source')}
                      placeholder="Enter water source"
                    />

                    <datalist id="purchase-sources">
                      <option value="Tanker delivery" />
                      <option value="Municipal supply" />
                      <option value="Borewell" />
                    </datalist>
                  </>
                </Field>

                <Field
                  label="Water Volume"
                  hint="Total quantity purchased."
                >
                  <div className="purchase-input-wrapper">
                    <input
                      type="number"
                      min="0.01"
                      step="0.01"
                      required
                      value={form.volumeKl}
                      onChange={updateField('volumeKl')}
                      placeholder="e.g. 20"
                    />
                    <span>kL</span>
                  </div>
                </Field>

                <Field
                  label="Cost per kL"
                  hint="Price charged for one kilolitre."
                >
                  <div className="purchase-input-wrapper">
                    <span>₹</span>
                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      required
                      value={form.unitCost}
                      onChange={updateField('unitCost')}
                      placeholder="e.g. 450"
                    />
                  </div>
                </Field>

                <div className="purchase-full-width">
                  <Field
                    label="Additional Notes"
                    hint="Optional information about this purchase."
                  >
                    <textarea
                      rows="3"
                      value={form.notes}
                      onChange={updateField('notes')}
                      placeholder="Enter supplier details, delivery information or other notes..."
                    />
                  </Field>
                </div>

              </div>

              {/* LIVE COST PREVIEW */}

              <div className="purchase-cost-preview">

                <div className="purchase-cost-preview-info">
                  <span>ESTIMATED PURCHASE COST</span>
                  <small>
                    Volume × Cost per kL
                  </small>
                </div>

                <strong>
                  {inr(
                    Number(form.volumeKl || 0) *
                    Number(form.unitCost || 0)
                  )}
                </strong>

              </div>

              <Msg kind={message.kind}>
                {message.text}
              </Msg>

              <button
                type="submit"
                className="purchase-submit-button"
                disabled={saving}
              >
                {saving ? (
                  <>
                    <span className="purchase-button-spinner" />
                    Saving Purchase...
                  </>
                ) : (
                  <>
                    Add Purchase
                    <span>→</span>
                  </>
                )}
              </button>

            </form>

          </Card>

          {/* HELPFUL INFORMATION */}

          <div className="purchase-help-card">

            <div className="purchase-help-icon">
              💧
            </div>

            <div>
              <h3>Keep your records accurate</h3>
              <p>
                Record every tanker delivery and bulk water
                transaction. These records help administrators
                understand procurement expenses and calculate
                water costs.
              </p>
            </div>

          </div>

        </section>

        {/* SIDE INFORMATION */}

        <aside className="purchases-side-panel">

          <div className="purchase-side-card">

            <span className="purchases-section-label">
              PROCUREMENT OVERVIEW
            </span>

            <h2>Water Supply Insights</h2>

            <div className="purchase-side-animation">
              <DotLottieReact
                src="/animations/water-saving.lottie"
                loop
                autoplay
              />
            </div>

            <div className="purchase-side-metric">
              <span>Total volume recorded</span>
              <strong>{num(summary.volume, 2)} kL</strong>
            </div>

            <div className="purchase-side-metric">
              <span>Total amount spent</span>
              <strong>{inr(summary.expenditure)}</strong>
            </div>

            <div className="purchase-side-metric">
              <span>Average cost per kL</span>
              <strong>{inr(averageCost)}</strong>
            </div>

            <div className="purchase-side-note">
              <span>ⓘ</span>
              <p>
                Purchase records can help monitor water procurement
                costs and support cost-sharing calculations.
              </p>
            </div>

          </div>

        </aside>

      </div>

      {/* =====================================
          PURCHASE HISTORY
      ====================================== */}

      <section className="purchases-history-section">

        <Card>

          <div className="purchases-section-heading history-heading">

            <div>
              <span className="purchases-section-label">
                TRANSACTION RECORDS
              </span>

              <h2>Purchase History</h2>

              <p>
                Review and manage all previously recorded water
                purchases.
              </p>
            </div>

            <div className="purchase-record-count">
              {summary.count} Records
            </div>

          </div>

          {loading && !data ? (
            <div className="purchases-loading">
              <Spinner />
              <p>Loading purchase history...</p>
            </div>
          ) : purchases.length === 0 ? (

            <div className="purchases-empty-state">

              <div className="purchases-empty-animation">
                <DotLottieReact
                  src="/animations/empty-building.lottie"
                  loop
                  autoplay
                />
              </div>

              <h3>No purchases recorded yet</h3>

              <p>
                Your water procurement history will appear here
                once you record your first purchase.
              </p>

            </div>

          ) : (

            <div className="purchases-table-wrap">

              <table className="purchases-table">

                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Water Source</th>
                    <th className="purchase-align-right">
                      Volume
                    </th>
                    <th className="purchase-align-right">
                      Cost / kL
                    </th>
                    <th className="purchase-align-right">
                      Total Cost
                    </th>
                    <th>Notes</th>
                    <th>Action</th>
                  </tr>
                </thead>

                <tbody>

                  {purchases.map((purchase) => (
                    <tr key={purchase.id}>

                      <td className="purchase-date-cell">
                        {purchase.date}
                      </td>

                      <td>
                        <div className="purchase-source-cell">
                          <span className="purchase-source-icon">
                            💧
                          </span>

                          <strong>{purchase.source}</strong>
                        </div>
                      </td>

                      <td className="purchase-align-right">
                        {num(purchase.volumeKl, 2)} kL
                      </td>

                      <td className="purchase-align-right">
                        {inr(purchase.unitCost)}
                      </td>

                      <td className="purchase-align-right">
                        <strong className="purchase-total-cell">
                          {inr(purchase.totalCost)}
                        </strong>
                      </td>

                      <td className="purchase-notes-cell">
                        {purchase.notes || '—'}
                      </td>

                      <td>
                        <button
                          type="button"
                          className="purchase-delete-button"
                          disabled={deletingId === purchase.id}
                          onClick={() => deletePurchase(purchase)}
                        >
                          {deletingId === purchase.id
                            ? 'Deleting...'
                            : 'Delete'}
                        </button>
                      </td>

                    </tr>
                  ))}

                </tbody>

              </table>

            </div>

          )}

        </Card>

      </section>

    </div>
  );
}
