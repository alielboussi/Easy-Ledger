-- Storage bucket and policies for business logos
-- Run this after schema_and_policies.sql

insert into storage.buckets (id, name, public)
values ('logos', 'logos', false)
on conflict (id) do nothing;

-- Policies for storage.objects (RLS enabled by default)
drop policy if exists "Logos read own" on storage.objects;
create policy "Logos read own" on storage.objects
for select using (
  bucket_id = 'logos' and name like auth.uid()::text || '/%'
);

drop policy if exists "Logos write own" on storage.objects;
create policy "Logos write own" on storage.objects
for insert with check (
  bucket_id = 'logos' and name like auth.uid()::text || '/%'
);

drop policy if exists "Logos update own" on storage.objects;
create policy "Logos update own" on storage.objects
for update using (
  bucket_id = 'logos' and name like auth.uid()::text || '/%'
) with check (
  bucket_id = 'logos' and name like auth.uid()::text || '/%'
);

drop policy if exists "Logos delete own" on storage.objects;
create policy "Logos delete own" on storage.objects
for delete using (
  bucket_id = 'logos' and name like auth.uid()::text || '/%'
);
