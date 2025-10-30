-- Storage bucket and policies for reports
-- Run this after schema_and_policies.sql

insert into storage.buckets (id, name, public)
values ('reports', 'reports', false)
on conflict (id) do nothing;

-- Policies for storage.objects (RLS enabled by default)
drop policy if exists "Reports read own" on storage.objects;
create policy "Reports read own" on storage.objects
for select using (
  bucket_id = 'reports' and name like auth.uid()::text || '/%'
);

drop policy if exists "Reports write own" on storage.objects;
create policy "Reports write own" on storage.objects
for insert with check (
  bucket_id = 'reports' and name like auth.uid()::text || '/%'
);

drop policy if exists "Reports update own" on storage.objects;
create policy "Reports update own" on storage.objects
for update using (
  bucket_id = 'reports' and name like auth.uid()::text || '/%'
) with check (
  bucket_id = 'reports' and name like auth.uid()::text || '/%'
);

drop policy if exists "Reports delete own" on storage.objects;
create policy "Reports delete own" on storage.objects
for delete using (
  bucket_id = 'reports' and name like auth.uid()::text || '/%'
);
