
import { DotLottieReact } from '@lottiefiles/dotlottie-react';
import InvoiceTable from '../components/InvoiceTable.jsx';
import { Card, Msg, Spinner, useLoad } from '../components/ui.jsx';
import './Invoices.css';

export default function Invoices() {
  const { data, error, loading, reload } = useLoad('/invoices');

  if (loading && !data) {
    return (
      <div className="invoices-loading">
        <div className="invoices-loading-animation">
          <DotLottieReact
            src="/animations/data-loading.lottie"
            loop
            autoplay
          />
        </div>
        <p>Loading your invoices...</p>
      </div>
    );
  }

  const invoices = data || [];

  const totalInvoices = invoices.length;
  const paidInvoices = invoices.filter(
    (invoice) => invoice.status === 'PAID'
  ).length;

  const pendingInvoices = invoices.filter(
    (invoice) => invoice.status !== 'PAID'
  ).length;

  const outstandingAmount = invoices
    .filter((invoice) => invoice.status !== 'PAID')
    .reduce((total, invoice) => total + Number(invoice.total || 0), 0);

  const formatCurrency = (amount) =>
    new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2
    }).format(amount);

  return (
    <div className="invoices-page">

      {/* Hero section */}
      <section className="invoices-hero">
        <div className="invoices-hero-content">
          <span className="invoices-eyebrow">
            BILLING & PAYMENTS
          </span>

          <h2>My Invoices</h2>

          <p>
            Track your water bills, review payment history, and download
            your invoices whenever you need them.
          </p>

          <div className="invoices-hero-badges">
            <span>🧾 Digital invoices</span>
            <span>🔒 Secure payments</span>
          </div>
        </div>

        <div className="invoices-hero-animation">
          <DotLottieReact
            src="/animations/water-wave.lottie"
            loop
            autoplay
          />
        </div>
      </section>

      {/* Summary cards */}
      <section className="invoices-stats">

        <Card>
          <div className="invoice-stat-card">
            <div className="invoice-stat-icon total-icon">🧾</div>
            <div>
              <span>Total invoices</span>
              <strong>{totalInvoices}</strong>
              <small>All billing records</small>
            </div>
          </div>
        </Card>

        <Card>
          <div className="invoice-stat-card">
            <div className="invoice-stat-icon paid-icon">✓</div>
            <div>
              <span>Paid invoices</span>
              <strong>{paidInvoices}</strong>
              <small>Successfully paid</small>
            </div>
          </div>
        </Card>

        <Card>
          <div className="invoice-stat-card">
            <div className="invoice-stat-icon pending-icon">⌛</div>
            <div>
              <span>Pending invoices</span>
              <strong>{pendingInvoices}</strong>
              <small>Awaiting payment</small>
            </div>
          </div>
        </Card>

        <Card>
          <div className="invoice-stat-card">
            <div className="invoice-stat-icon amount-icon">₹</div>
            <div>
              <span>Outstanding amount</span>
              <strong className="invoice-amount">
                {formatCurrency(outstandingAmount)}
              </strong>
              <small>Amount yet to be paid</small>
            </div>
          </div>
        </Card>

      </section>

      {/* Invoice history */}
      <section className="invoices-history">
        <Card>
          <div className="invoices-history-heading">
            <div>
              <span className="invoices-section-label">
                BILLING HISTORY
              </span>
              <h3>Your billing records</h3>
              <p>
                View, download, and manage your water invoices.
              </p>
            </div>

            <div className="invoices-count">
              {totalInvoices} {totalInvoices === 1 ? 'Invoice' : 'Invoices'}
            </div>
          </div>

          <Msg>{error}</Msg>

          {invoices.length === 0 ? (
            <div className="invoices-empty">
              <div className="invoices-empty-animation">
                <DotLottieReact
                  src="/animations/empty-building.lottie"
                  loop
                  autoplay
                />
              </div>

              <h3>No invoices yet</h3>

              <p>
                Your invoices will appear here once a billing cycle
                has been generated.
              </p>
            </div>
          ) : (
            <div className="invoices-table-container">
              <InvoiceTable
                invoices={data}
                onChanged={reload}
              />
            </div>
          )}
        </Card>
      </section>

      {/* Footer note */}
      <div className="invoices-footer-note">
        <span>🔐</span>
        <p>
          Your billing information and payment details are handled securely.
          Keep your downloaded invoices for future reference.
        </p>
      </div>

    </div>
  );
}
