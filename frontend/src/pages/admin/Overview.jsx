
import { useRef, useState } from 'react';

import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import { DotLottieReact } from '@lottiefiles/dotlottie-react';

import api, { errMsg } from '../../api.js';

import {
  Card,
  daysAgo,
  iso,
  Msg,
  num,
  Stat,
  useLoad,
} from '../../components/ui.jsx';

import './Overview.css';

// Reusable Lottie animation component
function AdminAnimation({ src, className = '' }) {
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

export default function Overview() {
  const [range, setRange] = useState({
    from: daysAgo(29),
    to: iso(new Date()),
  });

  const {
    data,
    error,
    loading,
    reload,
  } = useLoad(
    `/admin/usage/comparison?from=${range.from}&to=${range.to}`
  );

  const households = useLoad('/admin/households');

  const fileRef = useRef(null);

  const [csv, setCsv] = useState(null);
  const [alertMsg, setAlertMsg] = useState('');
  const [err, setErr] = useState('');

  const [isUploading, setIsUploading] = useState(false);
  const [isRunningAlerts, setIsRunningAlerts] = useState(false);

  // Upload CSV meter readings
  const upload = async (e) => {
    e.preventDefault();

    const file = fileRef.current?.files?.[0];

    if (!file) return;

    const fd = new FormData();
    fd.append('file', file);

    setErr('');
    setCsv(null);
    setIsUploading(true);

    try {
      const response = await api.post('/admin/usage/upload', fd);

      setCsv(response.data);
      reload();
    } catch (ex) {
      setErr(errMsg(ex));
    } finally {
      setIsUploading(false);
    }
  };

  // Run overuse and anomaly detection
  const runAlerts = async () => {
    setErr('');
    setAlertMsg('');
    setIsRunningAlerts(true);

    try {
      const response = await api.post('/admin/alerts/run');

      setAlertMsg(
        `Check complete: ${response.data.overuse} new overuse alert(s), ${response.data.anomalies} anomaly flag(s).`
      );
    } catch (ex) {
      setErr(errMsg(ex));
    } finally {
      setIsRunningAlerts(false);
    }
  };

  // Download CSV template
  const template = () => {
    const rows = (households.data || [])
      .filter((h) => h.metered)
      .map((h) => `${h.unitNumber},${iso(new Date())},`);

    const blob = new Blob(
      [`unit_number,date,litres\n${rows.join('\n')}\n`],
      { type: 'text/csv' }
    );

    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');

    a.href = url;
    a.download = 'meter-readings-template.csv';
    a.click();

    URL.revokeObjectURL(url);
  };

  // Usage statistics
  const usageData = data || [];

  const total = usageData.reduce(
    (sum, item) => sum + Number(item.kl || 0),
    0
  );

  const top = usageData
    .slice()
    .sort((a, b) => Number(b.kl) - Number(a.kl))[0];

  return (
    <div className="admin-overview">

      {/* Admin Hero */}
      <section className="overview-hero">
        <div className="overview-hero-content">
          <span className="overview-eyebrow">
            ADMIN CONTROL CENTER
          </span>

          <h2>Apartment overview</h2>

          <p>
            Monitor household consumption, manage meter readings,
            and identify unusual water usage across your apartment.
          </p>

          <div className="overview-hero-tag">
            <span className="overview-status-dot" />
            Water monitoring analytics
          </div>
        </div>

        <AdminAnimation
          src="/animations/admin-analytics.lottie"
          className="overview-hero-animation"
        />
      </section>

      {/* Usage by Household */}
      <Card
        title="Usage by household"
        right={
          <div className="inline overview-date-filters">
            <input
              type="date"
              value={range.from}
              max={range.to}
              onChange={(e) =>
                setRange({
                  ...range,
                  from: e.target.value,
                })
              }
            />

            <input
              type="date"
              value={range.to}
              min={range.from}
              max={iso(new Date())}
              onChange={(e) =>
                setRange({
                  ...range,
                  to: e.target.value,
                })
              }
            />
          </div>
        }
      >
        <Msg>{error}</Msg>

        {loading && !data ? (
          <div className="overview-loading">
            <AdminAnimation
              src="/animations/data-loading.lottie"
              className="overview-loading-animation"
            />
            <p>Analyzing household consumption...</p>
          </div>
        ) : (
          <>
            {/* Statistics */}
            <div className="grid stats">
              <Stat
                label="Total metered"
                value={`${num(total, 1)} kL`}
              />

              <Stat
                label="Households"
                value={usageData.length}
              />

              <Stat
                label="Highest user"
                value={top ? top.unitNumber : '—'}
                sub={top ? `${num(top.kl, 1)} kL` : ''}
              />
            </div>

            {/* Household Consumption Chart */}
            <div className="overview-chart">
              <ResponsiveContainer width="100%" height={300}>
                <BarChart
                  data={usageData}
                  margin={{
                    top: 15,
                    right: 15,
                    left: 5,
                    bottom: 5,
                  }}
                >
                  <CartesianGrid
                    strokeDasharray="3 3"
                    vertical={false}
                  />

                  <XAxis
                    dataKey="unitNumber"
                    fontSize={12}
                    tickLine={false}
                  />

                  <YAxis
                    fontSize={12}
                    tickLine={false}
                    axisLine={false}
                  />

                  <Tooltip
                    formatter={(value) => [
                      `${value} kL`,
                      'Consumption',
                    ]}
                  />

                  <Legend />

                  <Bar
                    dataKey="kl"
                    name="Consumption (kL)"
                    fill="#06b6d4"
                    radius={[6, 6, 0, 0]}
                    maxBarSize={65}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </>
        )}
      </Card>

      {/* CSV Upload and Alerts */}
      <div className="grid two">

        {/* CSV Upload */}
        <Card title="Upload meter readings (CSV)">
          <div className="overview-card-heading">
            <div>
              <p className="muted">
                Import daily meter readings for multiple households.
              </p>
            </div>

            <AdminAnimation
              src="/animations/csv-upload.lottie"
              className="overview-card-animation"
            />
          </div>

          <form className="stack" onSubmit={upload}>
            <input
              ref={fileRef}
              type="file"
              accept=".csv,text/csv"
              required
            />

            <small className="muted">
              Columns: <code>unit_number,date,litres</code>
              {' '} - one row per unit per day. Duplicates are skipped.
            </small>

            <div className="row overview-actions">
              <button
                className="primary"
                type="submit"
                disabled={isUploading}
              >
                {isUploading ? 'Uploading...' : 'Upload CSV'}
              </button>

              <button
                type="button"
                className="ghost"
                onClick={template}
              >
                Download template
              </button>
            </div>
          </form>

          {isUploading && (
            <div className="overview-inline-loading">
              <AdminAnimation
                src="/animations/data-loading.lottie"
                className="overview-small-animation"
              />
              <span>Importing meter readings...</span>
            </div>
          )}

          {csv && (
            <>
              <Msg kind={csv.errors?.length ? 'warn' : 'ok'}>
                Imported {csv.imported}, skipped {csv.duplicates} duplicate(s)
                {csv.errors?.length
                  ? `, ${csv.errors.length} error(s):`
                  : '.'}

                {csv.errors?.slice(0, 8).map((item, index) => (
                  <div key={`${index}-${item}`}>
                    • {item}
                  </div>
                ))}
              </Msg>

              <AdminAnimation
                src="/animations/upload-success.lottie"
                className="overview-success-animation"
              />
            </>
          )}
        </Card>

        {/* Alerts and Anomaly Detection */}
        <Card title="Alerts & anomaly detection">
          <div className="overview-alert-header">
            <div className="overview-alert-description">
              <p className="muted">
                Checks run automatically every morning. Run them now
                after uploading new data.
              </p>
            </div>

            <AdminAnimation
              src="/animations/security-alert.lottie"
              className="overview-alert-animation"
            />
          </div>

          <div className="overview-alert-features">
            <div>
              <span>01</span>
              <p>Overuse alerts</p>
            </div>

            <div>
              <span>02</span>
              <p>Leak and anomaly flags</p>
            </div>

            <div>
              <span>03</span>
              <p>Admin email reports</p>
            </div>
          </div>

          <p className="muted overview-alert-note">
            Anomaly detection flags usage greater than 2σ above
            a household's average.
          </p>

          <button
            className="primary"
            onClick={runAlerts}
            disabled={isRunningAlerts}
          >
            {isRunningAlerts ? 'Running checks...' : 'Run checks now'}
          </button>

          {isRunningAlerts && (
            <div className="overview-inline-loading">
              <AdminAnimation
                src="/animations/data-loading.lottie"
                className="overview-small-animation"
              />
              <span>Checking household usage...</span>
            </div>
          )}

          {alertMsg && (
            <>
              <Msg kind="ok">{alertMsg}</Msg>

              <AdminAnimation
                src="/animations/alert-success.lottie"
                className="overview-success-animation"
              />
            </>
          )}
        </Card>

      </div>

      {/* General Error Message */}
      <Msg>{err}</Msg>

    </div>
  );
}
