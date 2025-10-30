-- Storage bucket and policies for reports
-- Run this after schema_and_policies.sql

insert into storage.buckets (id, name, public)
values ('reports', 'reports', false)
on conflict (id) do nothing;

-- Policies for storage.objects (RLS enabled by default)
create policy if not exists "Reports read own" on storage.objects
for select using (
  bucket_id = 'reports' and name like auth.uid()::text || '/%'
);

create policy if not exists "Reports write own" on storage.objects
for insert with check (
  bucket_id = 'reports' and name like auth.uid()::text || '/%'
);

create policy if not exists "Reports update own" on storage.objects
for update using (
  bucket_id = 'reports' and name like auth.uid()::text || '/%'
) with check (
  bucket_id = 'reports' and name like auth.uid()::text || '/%'
);

create policy if not exists "Reports delete own" on storage.objects
for delete using (
  bucket_id = 'reports' and name like auth.uid()::text || '/%'
);
