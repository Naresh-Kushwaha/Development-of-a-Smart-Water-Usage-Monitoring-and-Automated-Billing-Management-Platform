import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { Card, Msg, num, Spinner, Stat, useLoad } from '../components/ui.jsx';

export default function Compare() {
  const { data, error, loading } = useLoad('/dashboard/compare');
  if (loading) return <Spinner />;
  if (error) return <Msg>{error}</Msg>;

  const bars = [
    { name: 'You', litres: Number(data.myAvgDaily), fill: '#0e7490' },
    { name: `Similar size (${data.similarCount})`, litres: Number(data.similarAvgDaily), fill: '#94a3b8' },
    { name: 'Apartment average', litres: Number(data.apartmentAvgDaily), fill: '#cbd5e1' },
  ];
  const better = Number(data.myAvgDaily) <= Number(data.similarAvgDaily);

  return (
    <>
      <h2>How do you compare?</h2>
      <p className="muted">Average daily use over the last 30 days. Peers are anonymous - similar-sized means flats within ±20% of your area.</p>
      <div className="grid stats">
        <Stat label="Your daily average" value={`${num(data.myAvgDaily)} L`} />
        <Stat label="Similar households" value={data.similarCount ? `${num(data.similarAvgDaily)} L` : 'n/a'} tone={better ? 'good' : 'warn'} />
        <Stat label="Per person / day" value={`${num(data.myPerPerson)} L`} sub={`Apartment: ${num(data.apartmentPerPerson)} L`} />
        <Stat label="Efficiency rank" value={data.similarCount ? `Better than ${data.betterThanPercent}%` : '—'} sub="of similar-sized homes" tone={data.betterThanPercent >= 50 ? 'good' : 'warn'} />
      </div>
      <Card title="Average litres per day">
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={bars}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" fontSize={12} />
            <YAxis fontSize={12} />
            <Tooltip />
            <Bar dataKey="litres" radius={[6, 6, 0, 0]}>{bars.map((b) => <Cell key={b.name} fill={b.fill} />)}</Bar>
          </BarChart>
        </ResponsiveContainer>
        <p className="muted">{better ? '🌟 Great job - you use less than similar homes. Keep it up!' : '💧 You use more than similar homes. Check taps and cisterns for leaks and try the tips on your dashboard.'}</p>
      </Card>
    </>
  );
}
