
import { useEffect, useState } from 'react';
import { DotLottieReact } from '@lottiefiles/dotlottie-react';

import api, { errMsg } from '../../api.js';
import {
  Card,
  Field,
  Msg,
  Spinner,
  useLoad
} from '../../components/ui.jsx';

import './Tariff.css';

const INITIAL_FORM = {
  tier1LimitKl: '',
  tier1Rate: '',
  tier2Rate: '',
  fixedCharge: '',
  alertThresholdKl: '',
  sharedMethod: 'AREA',
  billingMode: 'TIERED'
};

const formatCurrency = (value) =>
  `₹${Number(value || 0).toLocaleString('en-IN', {
    maximumFractionDigits: 2
  })}`;

export default function Tariff() {
  const { data, loading, error, reload } = useLoad('/admin/tariff');

  const [form, setForm] = useState(INITIAL_FORM);
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState({
    kind: '',
    text: ''
  });

  useEffect(() => {
    if (data) {
      setForm({
        ...INITIAL_FORM,
        ...data
      });
    }
  }, [data]);

  const updateField = (field) => (event) => {
    const value = event.target.value;

    setForm((previous) => ({
      ...previous,
      [field]: value
    }));

    setMsg({ kind: '', text: '' });
  };

  const saveTariff = async (event) => {
    event.preventDefault();

    setSaving(true);
    setMsg({ kind: '', text: '' });

    try {
      const payload = {
        ...form,
        tier1LimitKl: Number(form.tier1LimitKl),
        tier1Rate: Number(form.tier1Rate),
        tier2Rate: Number(form.tier2Rate),
        fixedCharge: Number(form.fixedCharge),
        alertThresholdKl: Number(form.alertThresholdKl)
      };

      await api.put('/admin/tariff', payload);

      setMsg({
        kind: 'ok',
        text: 'Tariff configuration saved successfully!'
      });

      if (reload) {
        reload();
      }
    } catch (error) {
      setMsg({
        kind: 'error',
        text: errMsg(error)
      });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="tariff-loading">
        <Spinner />
        <p>Loading tariff configuration...</p>
      </div>
    );
  }

  if (error && !data) {
    return (
      <div className="tariff-error">
        <Msg kind="error">
          Unable to load tariff configuration.
        </Msg>
        <button
          type="button"
          className="tariff-retry"
          onClick={reload}
        >
          Try again
        </button>
      </div>
    );
  }

  const tier1Limit = Number(form.tier1LimitKl || 0);
  const tier1Rate = Number(form.tier1Rate || 0);
  const tier2Rate = Number(form.tier2Rate || 0);
  const fixedCharge = Number(form.fixedCharge || 0);
  const alertThreshold = Number(form.alertThresholdKl || 0);

  const exampleConsumption = 15;

  const tier1Cost =
    Math.min(exampleConsumption, tier1Limit) * tier1Rate;

  const tier2Cost =
    Math.max(exampleConsumption - tier1Limit, 0) * tier2Rate;

  const estimatedBill =
    fixedCharge + tier1Cost + tier2Cost;

  return (
    <div className="tariff-page">

      {/* Header */}
      <section className="tariff-hero">

        <div className="tariff-hero-content">
          <div className="tariff-eyebrow">
            <span className="tariff-status-dot" />
            BILLING MANAGEMENT
          </div>

          <h1>
            Tariff <span>Configuration</span>
          </h1>

          <p>
            Manage water pricing, billing methods and consumption
            thresholds from one place.
          </p>

          <div className="tariff-hero-tags">
            <span>Water Management</span>
            <span>Smart Billing</span>
            <span>Admin Settings</span>
          </div>
        </div>

        <div className="tariff-animation">
          <DotLottieReact
            src="/animations/water-flow.lottie"
            loop
            autoplay
          />
        </div>

      </section>

      {/* Overview cards */}
      <section className="tariff-stats">

        <div className="tariff-stat-card">
          <div className="tariff-stat-icon blue">
            <span>₹</span>
          </div>

          <div>
            <p>Tier 1 Rate</p>
            <h3>{formatCurrency(tier1Rate)}</h3>
            <span>Per kilolitre</span>
          </div>
        </div>

        <div className="tariff-stat-card">
          <div className="tariff-stat-icon purple">
            <span>↗</span>
          </div>

          <div>
            <p>Tier 2 Rate</p>
            <h3>{formatCurrency(tier2Rate)}</h3>
            <span>Per kilolitre</span>
          </div>
        </div>

        <div className="tariff-stat-card">
          <div className="tariff-stat-icon green">
            <span>⌂</span>
          </div>

          <div>
            <p>Fixed Charge</p>
            <h3>{formatCurrency(fixedCharge)}</h3>
            <span>Per household</span>
          </div>
        </div>

        <div className="tariff-stat-card">
          <div className="tariff-stat-icon orange">
            <span>!</span>
          </div>

          <div>
            <p>Alert Threshold</p>
            <h3>{alertThreshold} kL</h3>
            <span>Monthly consumption</span>
          </div>
        </div>

      </section>

      {/* Main content */}
      <form onSubmit={saveTariff} className="tariff-main-grid">

        <div className="tariff-settings">

          {/* Pricing configuration */}
          <Card>
            <div className="tariff-section-heading">
              <div>
                <span className="tariff-section-label">
                  PRICING RULES
                </span>
                <h2>Water Rate Configuration</h2>
                <p>
                  Define the pricing structure applied to household
                  water consumption.
                </p>
              </div>

              <div className="tariff-heading-icon">₹</div>
            </div>

            <div className="tariff-fields-grid">

              <Field
                label="Tier-1 Limit"
                hint="Base rate applies up to this consumption."
              >
                <div className="tariff-input-wrapper">
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={form.tier1LimitKl}
                    onChange={updateField('tier1LimitKl')}
                  />
                  <span>kL</span>
                </div>
              </Field>

              <Field label="Tier-1 Rate" hint="Price per kilolitre.">
                <div className="tariff-input-wrapper">
                  <span>₹</span>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={form.tier1Rate}
                    onChange={updateField('tier1Rate')}
                  />
                </div>
              </Field>

              <Field
                label="Tier-2 Rate"
                hint="Applied to consumption above Tier-1."
              >
                <div className="tariff-input-wrapper">
                  <span>₹</span>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={form.tier2Rate}
                    onChange={updateField('tier2Rate')}
                  />
                </div>
              </Field>

              <Field
                label="Fixed Charge"
                hint="Fixed amount charged per household."
              >
                <div className="tariff-input-wrapper">
                  <span>₹</span>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    required
                    value={form.fixedCharge}
                    onChange={updateField('fixedCharge')}
                  />
                </div>
              </Field>

            </div>
          </Card>

          {/* Billing configuration */}
          <Card>
            <div className="tariff-section-heading">
              <div>
                <span className="tariff-section-label">
                  BILLING PREFERENCES
                </span>
                <h2>Billing Configuration</h2>
                <p>
                  Choose how household bills and shared expenses
                  are calculated.
                </p>
              </div>

              <div className="tariff-heading-icon">⚙</div>
            </div>

            <div className="tariff-fields-grid">

              <Field
                label="Billing Mode"
                hint="Select the calculation method."
              >
                <select
                  value={form.billingMode}
                  onChange={updateField('billingMode')}
                >
                  <option value="TIERED">Tiered Tariff</option>
                  <option value="COST_SHARING">
                    Cost Sharing (Proportional)
                  </option>
                </select>
              </Field>

              <Field
                label="Shared-Area Split"
                hint="How garden, pool and lobby usage is divided."
              >
                <select
                  value={form.sharedMethod}
                  onChange={updateField('sharedMethod')}
                >
                  <option value="AREA">By Flat Size</option>
                  <option value="OCCUPANCY">By Occupancy</option>
                </select>
              </Field>

            </div>

            <div className="tariff-info-note">
              <span>ⓘ</span>
              <p>
                <strong>Cost sharing:</strong> The actual bulk water
                cost is distributed according to household
                consumption. Unmetered units fall back to flat area.
              </p>
            </div>
          </Card>

          {/* Alert configuration */}
          <Card>
            <div className="tariff-section-heading">
              <div>
                <span className="tariff-section-label">
                  CONSUMPTION MONITORING
                </span>
                <h2>Overuse Alert Settings</h2>
                <p>
                  Set the monthly consumption limit that triggers
                  water usage alerts.
                </p>
              </div>

              <div className="tariff-heading-icon">♧</div>
            </div>

            <div className="tariff-alert-field">
              <Field
                label="Monthly Alert Threshold"
                hint="Households exceeding this value can receive alerts."
              >
                <div className="tariff-input-wrapper">
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={form.alertThresholdKl}
                    onChange={updateField('alertThresholdKl')}
                  />
                  <span>kL / month</span>
                </div>
              </Field>

              <div className="tariff-threshold-preview">
                <div>
                  <span>Current threshold</span>
                  <strong>{alertThreshold} kL</strong>
                </div>
                <div className="tariff-threshold-track">
                  <span />
                </div>
              </div>
            </div>
          </Card>

          {/* Save */}
          <div className="tariff-save-area">
            <Msg kind={msg.kind}>
              {msg.text}
            </Msg>

            <button
              type="submit"
              className="tariff-save-button"
              disabled={saving}
            >
              {saving ? 'Saving...' : 'Save Configuration'}
              {!saving && <span>→</span>}
            </button>
          </div>

        </div>

        {/* Preview sidebar */}
        <aside className="tariff-preview">

          <div className="tariff-preview-header">
            <div>
              <span className="tariff-section-label">
                LIVE PREVIEW
              </span>
              <h2>Bill Estimation</h2>
            </div>

            <span className="tariff-live-indicator">
              <span />
              LIVE
            </span>
          </div>

          <div className="tariff-preview-water">
            <div className="tariff-water-circle">
              <span>15</span>
              <small>kL</small>
            </div>
            <p>Example monthly consumption</p>
          </div>

          <div className="tariff-preview-breakdown">

            <div>
              <span>Fixed Charge</span>
              <strong>{formatCurrency(fixedCharge)}</strong>
            </div>

            <div>
              <span>Tier-1 Cost</span>
              <strong>{formatCurrency(tier1Cost)}</strong>
            </div>

            <div>
              <span>Tier-2 Cost</span>
              <strong>{formatCurrency(tier2Cost)}</strong>
            </div>

            <div className="tariff-preview-total">
              <span>Estimated Total</span>
              <strong>{formatCurrency(estimatedBill)}</strong>
            </div>

          </div>

          <div className="tariff-preview-note">
            <span>💡</span>
            <p>
              This is an illustrative estimate for a household
              consuming 15 kL. Shared charges and adjustments
              are not included.
            </p>
          </div>

          <div className="tariff-preview-footer">
            <span>Billing mode</span>
            <strong>
              {form.billingMode === 'TIERED'
                ? 'Tiered Tariff'
                : 'Cost Sharing'}
            </strong>
          </div>

        </aside>

      </form>

    </div>
  );
}
