-- One row per Razorpay order created for an invoice.
create table payments (
  id bigserial primary key,
  invoice_id bigint not null references invoices(id) on delete cascade,
  razorpay_order_id varchar(60) not null unique,
  razorpay_payment_id varchar(60),
  amount numeric(12,2) not null,
  currency varchar(3) not null default 'INR',
  status varchar(12) not null default 'CREATED',
  created_at timestamptz not null default now(),
  paid_at timestamptz
);
create index idx_payments_invoice on payments(invoice_id);
