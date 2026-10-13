create table apartments (
  id bigserial primary key,
  name varchar(120) not null,
  code varchar(20) not null unique,
  address varchar(255),
  created_at timestamptz not null default now()
);

create table households (
  id bigserial primary key,
  apartment_id bigint not null references apartments(id) on delete cascade,
  unit_number varchar(20) not null,
  area_sqft numeric(10,2) not null check (area_sqft > 0),
  occupants int not null default 1 check (occupants >= 1),
  meter_number varchar(40),
  unique (apartment_id, unit_number)
);

create table users (
  id bigserial primary key,
  email varchar(160) not null unique,
  password_hash varchar(100) not null,
  full_name varchar(120) not null,
  role varchar(20) not null,
  apartment_id bigint not null references apartments(id) on delete cascade,
  household_id bigint references households(id) on delete set null,
  created_at timestamptz not null default now()
);

create table tariff_plans (
  id bigserial primary key,
  apartment_id bigint not null unique references apartments(id) on delete cascade,
  tier1_limit_kl numeric(10,2) not null default 10,
  tier1_rate numeric(10,2) not null,
  tier2_rate numeric(10,2) not null,
  fixed_charge numeric(10,2) not null default 0,
  shared_method varchar(20) not null default 'AREA',
  billing_mode varchar(20) not null default 'TIERED',
  alert_threshold_kl numeric(10,2) not null default 15,
  updated_at timestamptz not null default now()
);

create table water_usage_logs (
  id bigserial primary key,
  household_id bigint not null references households(id) on delete cascade,
  reading_date date not null,
  litres numeric(12,2) not null check (litres >= 0),
  source varchar(10) not null,
  created_at timestamptz not null default now(),
  unique (household_id, reading_date)
);
create index idx_usage_date on water_usage_logs(reading_date);

create table bulk_purchases (
  id bigserial primary key,
  apartment_id bigint not null references apartments(id) on delete cascade,
  purchase_date date not null,
  source varchar(60) not null,
  volume_kl numeric(12,2) not null check (volume_kl > 0),
  unit_cost numeric(10,2) not null check (unit_cost >= 0),
  notes varchar(255)
);

create table billing_cycles (
  id bigserial primary key,
  apartment_id bigint not null references apartments(id) on delete cascade,
  label varchar(60) not null,
  start_date date not null,
  end_date date not null,
  status varchar(12) not null default 'OPEN',
  shared_area_kl numeric(12,2) not null default 0,
  created_at timestamptz not null default now(),
  finalized_at timestamptz,
  check (end_date >= start_date)
);

create table invoices (
  id bigserial primary key,
  billing_cycle_id bigint not null references billing_cycles(id) on delete cascade,
  household_id bigint not null references households(id) on delete cascade,
  consumption_kl numeric(12,3) not null,
  base_charge numeric(12,2) not null,
  shared_charge numeric(12,2) not null,
  adjustment numeric(12,2) not null default 0,
  adjustment_note varchar(255),
  total numeric(12,2) not null,
  status varchar(10) not null default 'UNPAID',
  created_at timestamptz not null default now(),
  unique (billing_cycle_id, household_id)
);

create table notifications (
  id bigserial primary key,
  user_id bigint not null references users(id) on delete cascade,
  type varchar(20) not null,
  message varchar(500) not null,
  is_read boolean not null default false,
  created_at timestamptz not null default now()
);
create index idx_notif_user on notifications(user_id, created_at desc);
