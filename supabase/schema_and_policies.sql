-- EasyLedger schema, constraints, RLS, indexes, rollups (no storage bucket here)
-- Run this first in Supabase SQL editor.

create extension if not exists pgcrypto;

-- Profiles for new users
create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    email text,
    display_name text,
    created_at timestamptz not null default now()
);

create or replace function public.handle_new_user()
returns trigger language plpgsql security definer as $$
begin
  insert into public.profiles (id, email, display_name)
  values (new.id, new.email, coalesce(new.raw_user_meta_data->>'full_name', new.email))
  on conflict (id) do nothing;
  return new;
end;$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- Businesses
create table if not exists public.businesses (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    logo_url text,
    currency_primary text not null check (length(currency_primary) between 3 and 4),
    currency_secondary text check (length(currency_secondary) between 3 and 4),
    currency_symbol text,
    currency_format text,
    created_at timestamptz not null default now(),
    unique (owner_id, name),
    check (currency_secondary is null or currency_secondary <> currency_primary)
);
create index if not exists idx_businesses_owner on public.businesses(owner_id);

-- Sub-businesses
create table if not exists public.sub_businesses (
    id uuid primary key default gen_random_uuid(),
    business_id uuid not null references public.businesses(id) on delete cascade,
    name text not null,
    created_at timestamptz not null default now(),
    unique (business_id, name)
);
create index if not exists idx_sub_businesses_biz on public.sub_businesses(business_id);

-- Categories
create table if not exists public.categories (
    id uuid primary key default gen_random_uuid(),
    business_id uuid references public.businesses(id) on delete cascade,
    sub_business_id uuid references public.sub_businesses(id) on delete cascade,
    name text not null,
    type text not null check (type in ('income','expense')),
    created_at timestamptz not null default now(),
    check ( (sub_business_id is not null) or (business_id is not null) )
);
create index if not exists idx_categories_biz on public.categories(business_id);
create index if not exists idx_categories_sub on public.categories(sub_business_id);
create index if not exists idx_categories_type on public.categories(type);

-- Transactions
create table if not exists public.transactions (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references auth.users(id) on delete cascade,
    business_id uuid not null references public.businesses(id) on delete cascade,
    sub_business_id uuid references public.sub_businesses(id) on delete set null,
    category_id uuid not null references public.categories(id) on delete restrict,
    type text not null check (type in ('income','expense')),
    currency text not null check (length(currency) between 3 and 4),
    amount numeric(18,2) not null check (amount >= 0),
    occurred_at timestamptz not null default now(),
    note text,
    created_at timestamptz not null default now()
);
create index if not exists idx_tx_owner on public.transactions(owner_id);
create index if not exists idx_tx_biz on public.transactions(business_id);
create index if not exists idx_tx_sub on public.transactions(sub_business_id);
create index if not exists idx_tx_cat on public.transactions(category_id);
create index if not exists idx_tx_type on public.transactions(type);
create index if not exists idx_tx_currency on public.transactions(currency);
create index if not exists idx_tx_owner_date on public.transactions(owner_id, occurred_at desc);
create index if not exists idx_tx_biz_currency_date on public.transactions(business_id, currency, occurred_at desc);
create index if not exists idx_tx_owner_currency_type_date on public.transactions(owner_id, currency, type, occurred_at desc);

-- Triggers: enforce currency & category scoping
create or replace function public.enforce_business_currency()
returns trigger language plpgsql as $$
declare cp text; cs text;
begin
  select currency_primary, currency_secondary into cp, cs from public.businesses b where b.id = new.business_id;
  if not (new.currency = cp or (cs is not null and new.currency = cs)) then
    raise exception 'Transaction currency % not allowed for business %', new.currency, new.business_id;
  end if;
  return new;
end;$$;

drop trigger if exists trg_tx_currency on public.transactions;
create trigger trg_tx_currency
  before insert or update on public.transactions
  for each row execute procedure public.enforce_business_currency();

create or replace function public.enforce_category_scoping()
returns trigger language plpgsql as $$
declare cat_type text; cat_biz uuid; cat_sub uuid;
begin
  select type, business_id, sub_business_id into cat_type, cat_biz, cat_sub from public.categories where id = new.category_id;
  if cat_type is null then
    raise exception 'Category % not found', new.category_id;
  end if;
  if cat_type <> new.type then
    raise exception 'Transaction type % must match category type %', new.type, cat_type;
  end if;
  if cat_sub is not null then
    if new.sub_business_id is distinct from cat_sub then
      raise exception 'Category is scoped to a different sub-business';
    end if;
  end if;
  if cat_biz is not null and new.business_id <> cat_biz then
    raise exception 'Category belongs to a different business';
  end if;
  return new;
end;$$;

drop trigger if exists trg_tx_category on public.transactions;
create trigger trg_tx_category
  before insert or update on public.transactions
  for each row execute procedure public.enforce_category_scoping();

-- Rollups
create table if not exists public.business_rollups (
    business_id uuid not null references public.businesses(id) on delete cascade,
    currency text not null,
    total_income numeric(20,2) not null default 0,
    total_expense numeric(20,2) not null default 0,
    updated_at timestamptz not null default now(),
    primary key (business_id, currency)
);
create index if not exists idx_rollups_biz_cur on public.business_rollups(business_id, currency);

create or replace function public.update_business_rollup(p_business_id uuid, p_currency text, p_income_delta numeric, p_expense_delta numeric)
returns void language plpgsql as $$
begin
  insert into public.business_rollups (business_id, currency, total_income, total_expense)
  values (p_business_id, p_currency, coalesce(p_income_delta,0), coalesce(p_expense_delta,0))
  on conflict (business_id, currency)
  do update set total_income = public.business_rollups.total_income + coalesce(excluded.total_income,0),
                total_expense = public.business_rollups.total_expense + coalesce(excluded.total_expense,0),
                updated_at = now();
end;$$;

create or replace function public.trg_tx_rollup_ins()
returns trigger language plpgsql as $$
begin
  if new.type = 'income' then
    perform public.update_business_rollup(new.business_id, new.currency, new.amount, 0);
  else
    perform public.update_business_rollup(new.business_id, new.currency, 0, new.amount);
  end if;
  return new;
end;$$;

drop trigger if exists trg_tx_rollup_insert on public.transactions;
create trigger trg_tx_rollup_insert
  after insert on public.transactions
  for each row execute procedure public.trg_tx_rollup_ins();

create or replace function public.trg_tx_rollup_del()
returns trigger language plpgsql as $$
begin
  if old.type = 'income' then
    perform public.update_business_rollup(old.business_id, old.currency, -old.amount, 0);
  else
    perform public.update_business_rollup(old.business_id, old.currency, 0, -old.amount);
  end if;
  return old;
end;$$;

drop trigger if exists trg_tx_rollup_delete on public.transactions;
create trigger trg_tx_rollup_delete
  after delete on public.transactions
  for each row execute procedure public.trg_tx_rollup_del();

create or replace function public.trg_tx_rollup_upd()
returns trigger language plpgsql as $$
begin
  if old.type = 'income' then
    perform public.update_business_rollup(old.business_id, old.currency, -old.amount, 0);
  else
    perform public.update_business_rollup(old.business_id, old.currency, 0, -old.amount);
  end if;
  if new.type = 'income' then
    perform public.update_business_rollup(new.business_id, new.currency, new.amount, 0);
  else
    perform public.update_business_rollup(new.business_id, new.currency, 0, new.amount);
  end if;
  return new;
end;$$;

drop trigger if exists trg_tx_rollup_update on public.transactions;
create trigger trg_tx_rollup_update
  after update on public.transactions
  for each row execute procedure public.trg_tx_rollup_upd();

-- Owner totals view
create or replace view public.owner_currency_totals as
select b.owner_id,
       r.currency,
       sum(r.total_income) as total_income,
       sum(r.total_expense) as total_expense
from public.business_rollups r
join public.businesses b on b.id = r.business_id
group by 1,2;

-- Filtered totals function
create or replace function public.get_totals(
  p_owner uuid,
  p_start timestamptz default '1970-01-01',
  p_end timestamptz default now(),
  p_business uuid default null,
  p_sub uuid default null,
  p_category uuid default null
)
returns table(currency text, total_income numeric, total_expense numeric) language sql stable as $$
  select t.currency,
         sum(case when t.type='income' then t.amount else 0 end) as total_income,
         sum(case when t.type='expense' then t.amount else 0 end) as total_expense
  from public.transactions t
  where t.owner_id = p_owner
    and t.occurred_at >= p_start and t.occurred_at <= p_end
    and (p_business is null or t.business_id = p_business)
    and (p_sub is null or t.sub_business_id = p_sub)
    and (p_category is null or t.category_id = p_category)
  group by t.currency
  order by t.currency;
$$;

-- Enable RLS
alter table public.profiles enable row level security;
alter table public.businesses enable row level security;
alter table public.sub_businesses enable row level security;
alter table public.categories enable row level security;
alter table public.transactions enable row level security;
alter table public.business_rollups enable row level security;

-- Profiles policies
drop policy if exists "Select own profile" on public.profiles;
create policy "Select own profile" on public.profiles for select using (id = auth.uid());
drop policy if exists "Update own profile" on public.profiles;
create policy "Update own profile" on public.profiles for update using (id = auth.uid());

-- Businesses policies
drop policy if exists "Select own businesses" on public.businesses;
create policy "Select own businesses" on public.businesses for select using (owner_id = auth.uid());
drop policy if exists "Modify own businesses" on public.businesses;
create policy "Modify own businesses" on public.businesses for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());

-- Sub-businesses policies
drop policy if exists "Select sub by owner" on public.sub_businesses;
create policy "Select sub by owner" on public.sub_businesses for select using (
  exists (select 1 from public.businesses b where b.id = sub_businesses.business_id and b.owner_id = auth.uid())
);
drop policy if exists "Modify sub by owner" on public.sub_businesses;
create policy "Modify sub by owner" on public.sub_businesses for all using (
  exists (select 1 from public.businesses b where b.id = sub_businesses.business_id and b.owner_id = auth.uid())
) with check (
  exists (select 1 from public.businesses b where b.id = sub_businesses.business_id and b.owner_id = auth.uid())
);

-- Categories policies
drop policy if exists "Select categories by owner" on public.categories;
create policy "Select categories by owner" on public.categories for select using (
  (business_id is not null and exists (select 1 from public.businesses b where b.id = categories.business_id and b.owner_id = auth.uid()))
  or
  (sub_business_id is not null and exists (select 1 from public.sub_businesses s join public.businesses b on b.id = s.business_id where s.id = categories.sub_business_id and b.owner_id = auth.uid()))
);
drop policy if exists "Modify categories by owner" on public.categories;
create policy "Modify categories by owner" on public.categories for all using (
  (business_id is not null and exists (select 1 from public.businesses b where b.id = categories.business_id and b.owner_id = auth.uid()))
  or
  (sub_business_id is not null and exists (select 1 from public.sub_businesses s join public.businesses b on b.id = s.business_id where s.id = categories.sub_business_id and b.owner_id = auth.uid()))
) with check (
  (business_id is not null and exists (select 1 from public.businesses b where b.id = categories.business_id and b.owner_id = auth.uid()))
  or
  (sub_business_id is not null and exists (select 1 from public.sub_businesses s join public.businesses b on b.id = s.business_id where s.id = categories.sub_business_id and b.owner_id = auth.uid()))
);

-- Transactions policies
drop policy if exists "Select own transactions" on public.transactions;
create policy "Select own transactions" on public.transactions for select using (owner_id = auth.uid());
drop policy if exists "Modify own transactions" on public.transactions;
create policy "Modify own transactions" on public.transactions for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());

-- Rollups policies
drop policy if exists "Select rollups by owner" on public.business_rollups;
create policy "Select rollups by owner" on public.business_rollups for select using (
  exists (select 1 from public.businesses b where b.id = business_rollups.business_id and b.owner_id = auth.uid())
);

-- Convenience view for current user
create or replace view public.my_currency_totals as
select currency, total_income, total_expense
from public.owner_currency_totals where owner_id = auth.uid();
