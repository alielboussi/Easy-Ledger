-- Email OTPs table and policies for Easy Ledger
create table if not exists public.email_otps (
  id bigserial primary key,
  user_id uuid,
  email text not null,
  code text not null,
  expires_at timestamptz not null,
  used boolean not null default false,
  created_at timestamptz not null default now()
);

-- Helpful indexes
create index if not exists email_otps_email_idx on public.email_otps (email);
create index if not exists email_otps_expires_idx on public.email_otps (expires_at);
create index if not exists email_otps_used_idx on public.email_otps (used);

alter table public.email_otps enable row level security;

-- RLS policies: allow selecting your own records; inserts/deletes are performed by Edge Functions using the service role
-- Postgres doesn't support "create policy if not exists", so we drop-then-create for idempotency
drop policy if exists "select_own_email_otps" on public.email_otps;
create policy "select_own_email_otps" on public.email_otps for select
using (
  auth.email() = email
);

drop policy if exists "update_own_email_otps" on public.email_otps;
create policy "update_own_email_otps" on public.email_otps for update
using (
  auth.email() = email
);

-- Inserts and deletes will be performed by Edge Functions with service role; no public insert/delete policy
