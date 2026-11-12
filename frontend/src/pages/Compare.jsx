
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import { DotLottieReact } from '@lottiefiles/dotlottie-react';

import {
  Card,
  Msg,
  num,
  Stat,
  useLoad,
} from '../components/ui.jsx';

import './Compare.css';

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

export default function Compare() {
  const { data, error, loading } = useLoad('/dashboard/compare');

  // Loading state
  if (loading) {
    return (
      <div className="compare-loading">
        <WaterAnimation
          src="/animations/water-loading.lottie"
          className="compare-loading-animation"
        />
        <p>Analyzing your water consumption...</p>
      </div>
    );
  }

  // Error state
  if (error) {
    return <Msg>{error}</Msg>;
  }

  if (!data) {
    return <Msg>No comparison data available.</Msg>;
  }

  // Chart data
  const bars = [
    {
      name: 'You',
      litres: Number(data.myAvgDaily) || 0,
      fill: '#0e7490',
    },
    {
      name: `Similar size (${data.similarCount})`,
      litres: Number(data.similarAvgDaily) || 0,
      fill: '#94a3b8',
    },
    {
      name: 'Apartment average',
      litres: Number(data.apartmentAvgDaily) || 0,
      fill: '#cbd5e1',
    },
  ];

  const hasSimilarHouseholds = Number(data.similarCount) > 0;

  const better =
    Number(data.myAvgDaily) <= Number(data.similarAvgDaily);

  return (
    <div className="compare-page">

      {/* Hero Section */}
      <section className="compare-hero">
        <div className="compare-hero-content">
          <span className="compare-eyebrow">
            WATER CONSUMPTION ANALYTICS
          </span>

          <h2>How do you compare?</h2>

          <p className="muted">
            Understand your water consumption and see how efficiently
            you use water compared to similar households.
          </p>
        </div>

        <WaterAnimation
          src="/animations/water-drop.lottie"
          className="compare-hero-animation"
        />
      </section>

      {/* Statistics */}
      <div className="grid stats">
        <Stat
          label="Your daily average"
          value={`${num(data.myAvgDaily)} L`}
        />

        <Stat
          label="Similar households"
          value={
            hasSimilarHouseholds
              ? `${num(data.similarAvgDaily)} L`
              : 'n/a'
          }
          tone={hasSimilarHouseholds ? (better ? 'good' : 'warn') : undefined}
        />

        <Stat
          label="Per person / day"
          value={`${num(data.myPerPerson)} L`}
          sub={`Apartment: ${num(data.apartmentPerPerson)} L`}
        />

        <Stat
          label="Efficiency rank"
          value={
            hasSimilarHouseholds
              ? `Better than ${data.betterThanPercent}%`
              : '—'
          }
          sub="of similar-sized homes"
          tone={
            hasSimilarHouseholds
              ? (Number(data.betterThanPercent) >= 50 ? 'good' : 'warn')
              : undefined
          }
        />
      </div>

      {/* Comparison Chart */}
      <Card title="Average litres per day">
        <div className="compare-chart">
          <ResponsiveContainer width="100%" height={300}>
            <BarChart
              data={bars}
              margin={{ top: 15, right: 15, left: 5, bottom: 5 }}
            >
              <CartesianGrid
                strokeDasharray="3 3"
                vertical={false}
              />

              <XAxis
                dataKey="name"
                fontSize={12}
                tickLine={false}
              />

              <YAxis
                fontSize={12}
                tickLine={false}
                axisLine={false}
              />

              <Tooltip
                formatter={(value) => [`${value} L`, 'Daily usage']}
                contentStyle={{
                  borderRadius: '12px',
                  border: '1px solid #e2e8f0',
                }}
              />

              <Bar
                dataKey="litres"
                radius={[8, 8, 0, 0]}
                maxBarSize={75}
              >
                {bars.map((bar) => (
                  <Cell
                    key={bar.name}
                    fill={bar.fill}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        <p className="muted compare-chart-description">
          Average daily water consumption over the last 30 days.
        </p>
      </Card>

      {/* Comparison Insight */}
      {hasSimilarHouseholds ? (
        <section className={`compare-insight ${better ? 'insight-good' : 'insight-warning'}`}>
          <WaterAnimation
            src={
              better
                ? '/animations/success.lottie'
                : '/animations/water-saving.lottie'
            }
            className="compare-insight-animation"
          />

          <div className="compare-insight-content">
            <h3>
              {better
                ? 'Great job! Keep it up!'
                : 'Time to save some water!'}
            </h3>

            <p>
              {better
                ? 'You use less water than similar-sized households. Your efforts are making a difference!'
                : 'Your water consumption is higher than similar-sized households. Check for leaking taps and cisterns, and try the water-saving tips on your dashboard.'}
            </p>
          </div>
        </section>
      ) : (
        <section className="compare-insight">
          <div className="compare-insight-content">
            <h3>Not enough comparison data yet</h3>
            <p>
              We need more similar-sized households to provide
              a meaningful comparison. Keep tracking your usage!
            </p>
          </div>
        </section>
      )}

      {/* Privacy Notice */}
      <div className="compare-privacy">
        <span>🔒</span>
        <p>
          Your household comparison is anonymous. Other residents
          cannot see your individual water consumption data.
        </p>
      </div>

    </div>
  );
}
