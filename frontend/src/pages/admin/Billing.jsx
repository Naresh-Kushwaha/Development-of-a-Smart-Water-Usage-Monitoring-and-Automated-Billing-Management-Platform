
import { useEffect, useState } from 'react';
import { DotLottieReact } from '@lottiefiles/dotlottie-react';

import api, { errMsg } from '../../api.js';
import InvoiceTable from '../../components/InvoiceTable.jsx';
import {
  Card,
  Field,
  iso,
  Msg,
  Spinner,
  StatusPill,
  useLoad
} from '../../components/ui.jsx';

import './Billing.css';

function getCurrentMonthDates() {
  const now = new Date();

  const first = new Date(
    now.getFullYear(),
    now.getMonth(),
    1
  );

  const last = new Date(
    now.getFullYear(),
    now.getMonth() + 1,
    0
  );

  return {
    startDate: iso(
      new Date(first.getTime() - first.getTimezoneOffset() * 60000)
    ),
    endDate: iso(
      new Date(last.getTime() - last.getTimezoneOffset() * 60000)
    )
  };
}

export default function Billing() {
  const {
    data: cycles,
    loading,
    reload
  } = useLoad('/admin/cycles');

  const [selectedCycle, setSelectedCycle] = useState(null);
  const [invoices, setInvoices] = useState(null);
  const [invoiceLoading, setInvoiceLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [actionId, setActionId] = useState(null);
  const [err, setErr] = useState('');

  const now = new Date();
  const dates = getCurrentMonthDates();

  const [form, setForm] = useState({
    label: now.toLocaleString('en', {
      month: 'long',
      year: 'numeric'
    }),
    startDate: dates.startDate,
    endDate: dates.endDate,
    sharedAreaKl: 0
  });

  const updateField = (field) => (event) => {
    setForm((previous) => ({
      ...previous,
      [field]: event.target.value
    }));

    setErr('');
  };

  // Load invoices for the selected billing cycle.
  const loadInvoices = async (id) => {
    if (!id) {
      setInvoices(null);
      return;
    }

    setInvoiceLoading(true);
    setErr('');

    try {
      const response = await api.get(
        `/admin/cycles/${id}/invoices`
      );

      setInvoices(response.data);
    } catch (error) {
      setInvoices([]);
      setErr(errMsg(error));
    } finally {
      setInvoiceLoading(false);
    }
  };

  // Load invoices whenever the selected cycle changes.
  useEffect(() => {
    if (selectedCycle) {
      loadInvoices(selectedCycle);
    } else {
      setInvoices(null);
    }
  }, [selectedCycle]);

  // Select the latest available cycle by default.
  useEffect(() => {
    if (!selectedCycle && cycles?.length) {
      setSelectedCycle(cycles[0].id);
    }
  }, [cycles, selectedCycle]);

  const currentCycle = cycles?.find(
    (cycle) => cycle.id === selectedCycle
  );

  // Shared action handler for finalize and archive.
  const runAction = async (id, action) => {
    setErr('');
    setActionId(id);

    try {
      await action();

      await reload();

      if (selectedCycle === id) {
        await loadInvoices(id);
      }
    } catch (error) {
      setErr(errMsg(error));
    } finally {
      setActionId(null);
    }
  };

  // Create a new billing cycle.
  const openCycle = async (event) => {
    event.preventDefault();

    setErr('');
    setSaving(true);

    try {
      const response = await api.post('/admin/cycles', {
        ...form,
        sharedAreaKl: Number(form.sharedAreaKl)
      });

      const createdCycle = response.data;

      await reload();

      setSelectedCycle(createdCycle.id);

      setErr('');

      // Prepare the next cycle form with a fresh label and dates.
      const nextDates = getCurrentMonthDates();

      setForm({
        label: '',
        startDate: nextDates.startDate,
        endDate: nextDates.endDate,
        sharedAreaKl: 0
      });
    } catch (error) {
      setErr(errMsg(error));
    } finally {
      setSaving(false);
    }
  };

  const finalizeCycle = (cycle) => {
    const confirmed = window.confirm(
      `Finalize "${cycle.label}"?\n\nInvoices will be generated and residents notified.`
    );

    if (!confirmed) return;

    runAction(cycle.id, () =>
      api.post(`/admin/cycles/${cycle.id}/finalize`)
    );
  };

  const archiveCycle = (cycle) => {
    const confirmed = window.confirm(
      `Archive "${cycle.label}"?`
    );

    if (!confirmed) return;

    runAction(cycle.id, () =>
      api.post(`/admin/cycles/${cycle.id}/archive`)
    );
  };

  const refreshInvoices = () => {
    if (selectedCycle) {
      loadInvoices(selectedCycle);
    }
  };

  return (
    <div className="billing-page">

      {/* =====================================
          HERO SECTION
      ====================================== */}

      <section className="billing-hero">

        <div className="billing-hero-content">

          <div className="billing-eyebrow">
            <span className="billing-status-dot" />
            SMART BILLING MANAGEMENT
          </div>

          <h1>
            Billing <span>Cycles</span>
          </h1>

          <p>
            Manage monthly billing periods, calculate household
            invoices, review charges and keep track of payment
            activity.
          </p>

          <div className="billing-hero-tags">
            <span>Cycle Management</span>
            <span>Invoice Generation</span>
            <span>Payment Tracking</span>
          </div>

        </div>

        <div className="billing-hero-animation">
          <DotLottieReact
            src="/animations/admin-analytics.lottie"
            loop
            autoplay
          />
        </div>

      </section>

      {/* =====================================
          SUMMARY CARDS
      ====================================== */}

      <section className="billing-stats">

        <div className="billing-stat-card">
          <div className="billing-stat-icon blue">
            <span>▤</span>
          </div>

          <div>
            <p>Total Cycles</p>
            <h3>{cycles?.length ?? 0}</h3>
            <span>Recorded billing periods</span>
          </div>
        </div>

        <div className="billing-stat-card">
          <div className="billing-stat-icon green">
            <span>✓</span>
          </div>

          <div>
            <p>Finalized Cycles</p>
            <h3>
              {cycles?.filter(
                (cycle) => cycle.status === 'FINALIZED'
              ).length ?? 0}
            </h3>
            <span>Invoices generated</span>
          </div>
        </div>

        <div className="billing-stat-card">
          <div className="billing-stat-icon orange">
            <span>◷</span>
          </div>

          <div>
            <p>Open Cycles</p>
            <h3>
              {cycles?.filter(
                (cycle) => cycle.status === 'OPEN'
              ).length ?? 0}
            </h3>
            <span>Awaiting finalization</span>
          </div>
        </div>

        <div className="billing-stat-card">
          <div className="billing-stat-icon purple">
            <span>▣</span>
          </div>

          <div>
            <p>Selected Cycle</p>
            <h3>
              {currentCycle ? currentCycle.label : '—'}
            </h3>
            <span>Currently viewing</span>
          </div>
        </div>

      </section>

      {/* =====================================
          CREATE NEW CYCLE
      ====================================== */}

      <section className="billing-create-section">

        <Card>

          <div className="billing-section-heading">

            <div>
              <span className="billing-section-label">
                CREATE BILLING PERIOD
              </span>

              <h2>Open a New Cycle</h2>

              <p>
                Define the billing period and shared-area water
                consumption for this cycle.
              </p>
            </div>

            <div className="billing-heading-icon">
              +
            </div>

          </div>

          <form
            className="billing-form"
            onSubmit={openCycle}
          >

            <div className="billing-form-grid">

              <Field
                label="Cycle Label"
                hint="Example: November 2026"
              >
                <input
                  required
                  value={form.label}
                  onChange={updateField('label')}
                  placeholder="Enter cycle name"
                />
              </Field>

              <Field label="Start Date">
                <input
                  type="date"
                  required
                  value={form.startDate}
                  onChange={updateField('startDate')}
                />
              </Field>

              <Field label="End Date">
                <input
                  type="date"
                  required
                  min={form.startDate}
                  value={form.endDate}
                  onChange={updateField('endDate')}
                />
              </Field>

              <Field
                label="Shared-Area Usage"
                hint="Water used in gardens, pools and lobbies."
              >
                <div className="billing-input-wrapper">
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    required
                    value={form.sharedAreaKl}
                    onChange={updateField('sharedAreaKl')}
                  />
                  <span>kL</span>
                </div>
              </Field>

            </div>

            <Msg kind={err ? 'error' : ''}>
              {err}
            </Msg>

            <div className="billing-form-footer">

              <p>
                <span>ⓘ</span>
                Once finalized, invoices will be generated
                for the cycle.
              </p>

              <button
                type="submit"
                className="billing-primary-button"
                disabled={saving}
              >
                {saving ? 'Opening Cycle...' : 'Open New Cycle'}
                {!saving && <span>→</span>}
              </button>

            </div>

          </form>

        </Card>

      </section>

      {/* =====================================
          BILLING CYCLE HISTORY
      ====================================== */}

      <section className="billing-cycles-section">

        <Card>

          <div className="billing-section-heading">

            <div>
              <span className="billing-section-label">
                BILLING HISTORY
              </span>

              <h2>All Billing Cycles</h2>

              <p>
                View, finalize or archive your billing periods.
              </p>
            </div>

            <div className="billing-record-count">
              {cycles?.length ?? 0} Cycles
            </div>

          </div>

          {loading && !cycles ? (

            <div className="billing-loading">
              <Spinner />
              <p>Loading billing cycles...</p>
            </div>

          ) : !cycles?.length ? (

            <div className="billing-empty-state">

              <div className="billing-empty-animation">
                <DotLottieReact
                  src="/animations/empty-building.lottie"
                  loop
                  autoplay
                />
              </div>

              <h3>No billing cycles yet</h3>

              <p>
                Create your first billing cycle to start generating
                household invoices.
              </p>

            </div>

          ) : (

            <div className="billing-table-wrap">

              <table className="billing-table">

                <thead>
                  <tr>
                    <th>Cycle Label</th>
                    <th>Billing Period</th>
                    <th className="billing-align-right">
                      Shared Usage
                    </th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>

                <tbody>

                  {cycles.map((cycle) => (
                    <tr
                      key={cycle.id}
                      className={
                        cycle.id === selectedCycle
                          ? 'billing-selected-row'
                          : ''
                      }
                    >

                      <td>
                        <strong>{cycle.label}</strong>
                      </td>

                      <td className="billing-period-cell">
                        {cycle.startDate} <span>→</span> {cycle.endDate}
                      </td>

                      <td className="billing-align-right">
                        {Number(cycle.sharedAreaKl || 0).toFixed(2)} kL
                      </td>

                      <td>
                        <StatusPill status={cycle.status} />
                      </td>

                      <td>
                        <div className="billing-actions">

                          <button
                            type="button"
                            className="billing-action-button view"
                            onClick={() => setSelectedCycle(cycle.id)}
                          >
                            View
                          </button>

                          {cycle.status === 'OPEN' && (
                            <button
                              type="button"
                              className="billing-action-button finalize"
                              disabled={actionId === cycle.id}
                              onClick={() => finalizeCycle(cycle)}
                            >
                              {actionId === cycle.id
                                ? 'Working...'
                                : 'Finalize'}
                            </button>
                          )}

                          {cycle.status === 'FINALIZED' && (
                            <button
                              type="button"
                              className="billing-action-button archive"
                              disabled={actionId === cycle.id}
                              onClick={() => archiveCycle(cycle)}
                            >
                              {actionId === cycle.id
                                ? 'Working...'
                                : 'Archive'}
                            </button>
                          )}

                        </div>
                      </td>

                    </tr>
                  ))}

                </tbody>

              </table>

            </div>

          )}

        </Card>

      </section>

      {/* =====================================
          SELECTED CYCLE INVOICES
      ====================================== */}

      {currentCycle && (

        <section className="billing-invoices-section">

          <Card>

            <div className="billing-section-heading billing-invoice-heading">

              <div>
                <span className="billing-section-label">
                  {currentCycle.status === 'OPEN'
                    ? 'LIVE BILLING PREVIEW'
                    : 'GENERATED INVOICES'}
                </span>

                <h2>{currentCycle.label}</h2>

                <p>
                  {currentCycle.status === 'OPEN'
                    ? 'Review estimated household charges before finalizing this cycle.'
                    : 'Review household invoices, adjustments and payment status.'}
                </p>
              </div>

              <div className="billing-invoice-status">
                <StatusPill status={currentCycle.status} />
              </div>

            </div>

            {err && (
              <div className="billing-invoice-error">
                <Msg kind="error">{err}</Msg>
              </div>
            )}

            {invoiceLoading ? (
              <div className="billing-loading">
                <Spinner />
                <p>Loading invoices...</p>
              </div>
            ) : (
              <InvoiceTable
                invoices={invoices}
                admin
                onChanged={refreshInvoices}
              />
            )}

          </Card>

        </section>

      )}

    </div>
  );
}
