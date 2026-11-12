
import { useState } from 'react';

import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import { DotLottieReact } from '@lottiefiles/dotlottie-react';

import api, { errMsg } from '../api.js';
import InvoiceTable from '../components/InvoiceTable.jsx';

import {
  Card,
  Field,
  inr,
  iso,
  Msg,
  num,
  Stat,
  useLoad,
} from '../components/ui.jsx';

import './Dashboard.css';

// Reusable Lottie animation component
function WaterAnimation({ src, className = '' }) {
  return (
    <div className={className}>
      <DotLottieReact
        src={src}
        loop
        autoplay
      />
    </div>
  );
}

export default function Dashboard() {
  const { data, error, loading, reload } = useLoad('/dashboard');

  const [form, setForm] = useState({
    date: iso(new Date()),
    litres: '',
  });

  const [msg, setMsg] = useState({
    kind: '',
    text: '',
  });

  // Loading animation
  if (loading && !data) {
    return (
      <div className="dashboard-loading">
        <WaterAnimation
          src="/animations/water-flow.lottie"
          className="dashboard-loading-animation"
        />
        <h3>Preparing your dashboard...</h3>
        <p>Fetching your water consumption data.</p>
      </div>
    );
  }

  if (error) {
    return <Msg>{error}</Msg>;
  }

  if (!data) {
    return <Msg>No dashboard data available.</Msg>;
  }

  // Save water consumption reading
  const submit = async (e) => {
    e.preventDefault();

    try {
      await api.post('/usage', {
        date: form.date,
        litres: Number(form.litres),
      });

      setMsg({
        kind: 'ok',
        text: 'Water usage reading saved successfully!',
      });

      setForm({
        ...form,
        litres: '',
      });

      reload();
    } catch (ex) {
      setMsg({
        kind: 'error',
        text: errMsg(ex),
      });
    }
  };

  const last = data.daily[data.daily.length - 1];

  const pct = data.thresholdKl
    ? Math.min(
        100,
        (Number(data.monthToDateKl) / Number(data.thresholdKl)) * 100
      )
    : 0;

  const c = data.cycle;

  return (
    <div className="dashboard-page">

      {/* Welcome Hero Section */}
      <section className="dashboard-hero">
        <div className="dashboard-hero-content">
          <span className="dashboard-eyebrow">
            SMART WATER MONITORING
          </span>

          <h2>Hello, unit {data.unitNumber} 👋</h2>

          <p>
            Monitor your water consumption, track your bills,
            and make every drop count.
          </p>

          <div className="dashboard-hero-tag">
            <span className="status-dot"></span>
            Your water dashboard is active
          </div>
        </div>

        <WaterAnimation
          src="/animations/water-wave.lottie"
          className="dashboard-hero-animation"
        />
      </section>

      {/* Dashboard Statistics */}
      <div className="grid stats">

        <Stat
          label="Latest reading"
          value={last ? `${num(last.litres, 0)} L` : '—'}
          sub={last?.date}
        />

        <Stat
          label="This month"
          value={`${num(data.monthToDateKl, 2)} kL`}
          sub={`Alert at ${num(data.thresholdKl, 1)} kL`}
          tone={
            pct >= 100
              ? 'bad'
              : pct >= 80
                ? 'warn'
                : ''
          }
        />

        <Stat
          label="Current cycle"
          value={c ? `${num(c.consumptionKl, 2)} kL` : '—'}
          sub={c?.label ?? 'No open cycle'}
        />

        <Stat
          label="Bill preview"
          value={c ? inr(c.estimatedTotal) : '—'}
          sub={
            c
              ? `Water ${inr(c.estimatedBase)} + shared ${inr(c.estimatedShared)}`
              : ''
          }
        />

      </div>

      {/* Monthly Usage Meter */}
      <section className="dashboard-usage-section">
        <div className="dashboard-usage-heading">
          <div>
            <h3>Monthly usage limit</h3>
            <p className="muted">
              {pct.toFixed(0)}% of your monthly alert threshold used
            </p>
          </div>

          <strong>{pct.toFixed(0)}%</strong>
        </div>

        <div
          className="meter"
          title={`${pct.toFixed(0)}% of your monthly alert threshold`}
        >
          <i
            style={{ width: pct + '%' }}
            className={
              pct >= 100
                ? 'bad'
                : pct >= 80
                  ? 'warn'
                  : ''
            }
          />
        </div>
      </section>

      {/* Consumption Charts */}
      <div className="grid two">

        <Card title="Daily consumption (last 30 days, litres)">
          <ResponsiveContainer width="100%" height={260}>
            <LineChart data={data.daily}>
              <CartesianGrid strokeDasharray="3 3" />

              <XAxis
                dataKey="date"
                tickFormatter={(d) => d.slice(5)}
                fontSize={12}
              />

              <YAxis fontSize={12} />

              <Tooltip />

              <Line
                type="monotone"
                dataKey="litres"
                stroke="#0e7490"
                strokeWidth={3}
                dot={false}
                activeDot={{ r: 6 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </Card>

        <Card title="Monthly consumption (kL)">
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={data.monthly}>
              <CartesianGrid strokeDasharray="3 3" />

              <XAxis
                dataKey="month"
                fontSize={12}
              />

              <YAxis fontSize={12} />

              <Tooltip />

              {data.thresholdKl && (
                <ReferenceLine
                  y={Number(data.thresholdKl)}
                  stroke="#dc2626"
                  strokeDasharray="4 4"
                  label={{
                    value: 'alert',
                    fontSize: 11,
                    fill: '#dc2626',
                  }}
                />
              )}

              <Bar
                dataKey="kl"
                fill="#06b6d4"
                radius={[6, 6, 0, 0]}
              />
            </BarChart>
          </ResponsiveContainer>
        </Card>

      </div>

      {/* Usage Form and Water Saving Tips */}
      <div className="grid two">

        <Card title="Log your meter usage">
          <form className="inline" onSubmit={submit}>

            <Field label="Date">
              <input
                type="date"
                max={iso(new Date())}
                required
                value={form.date}
                onChange={(e) =>
                  setForm({
                    ...form,
                    date: e.target.value,
                  })
                }
              />
            </Field>

            <Field label="Litres used that day">
              <input
                type="number"
                min="0"
                step="0.1"
                required
                value={form.litres}
                onChange={(e) =>
                  setForm({
                    ...form,
                    litres: e.target.value,
                  })
                }
              />
            </Field>

            <button className="primary" type="submit">
              Save reading
            </button>

          </form>

          <div className="dashboard-message">
            <Msg kind={msg.kind}>{msg.text}</Msg>

            {msg.kind === 'ok' && (
              <WaterAnimation
                src="/animations/water-success.lottie"
                className="dashboard-success-animation"
              />
            )}
          </div>
        </Card>

        <Card title="Water-saving tips">
          <div className="dashboard-tips-header">
            <div>
              <h3>Save water, save money</h3>
              <p className="muted">
                Small changes can make a big difference.
              </p>
            </div>

            <WaterAnimation
              src="/animations/water-leaf.lottie"
              className="dashboard-tips-animation"
            />
          </div>

          <ul className="tips">
            {data.tips.map((t) => (
              <li key={t}>{t}</li>
            ))}
          </ul>
        </Card>

      </div>

      {/* Invoice History */}
      <Card title="Invoice history">
        <InvoiceTable invoices={data.invoices} />
      </Card>

    </div>
  );
}
