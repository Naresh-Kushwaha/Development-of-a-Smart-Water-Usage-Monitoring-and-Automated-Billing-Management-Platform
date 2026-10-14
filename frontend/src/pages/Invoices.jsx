import InvoiceTable from '../components/InvoiceTable.jsx';
import { Card, Msg, Spinner, useLoad } from '../components/ui.jsx';

export default function Invoices() {
  const { data, error, loading } = useLoad('/invoices');
  if (loading) return <Spinner />;
  return (
    <>
      <h2>My invoices</h2>
      <Msg>{error}</Msg>
      <Card><InvoiceTable invoices={data} /></Card>
    </>
  );
}
