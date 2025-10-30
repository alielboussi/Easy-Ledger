-- Profiles table for EasyLedger
-- Stores user-facing identity and contact details synchronized with Supabase Auth

-- 1) Create table
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text not null unique,
  username text not null unique,
  date_of_birth date,
  country text,
  country_code text,
  phone text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- 2) Updated_at trigger
create or replace function public.set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at();

-- 3) RLS policies
alter table public.profiles enable row level security;

-- Each user can view their own profile
drop policy if exists "Can view own profile" on public.profiles;
create policy "Can view own profile"
  on public.profiles for select
  using (auth.uid() = id);

-- Each user can update their own profile
drop policy if exists "Can update own profile" on public.profiles;
create policy "Can update own profile"
  on public.profiles for update
  using (auth.uid() = id);

-- Inserts are done by trigger (security definer) after auth.users creation
-- Optionally, allow insert by service role only (handled outside RLS)

-- 4) Trigger to create profile on new auth user
create or replace function public.handle_new_user()
returns trigger as $$
declare
  base_username text;
  final_username text;
  dob_text text;
  parsed_dob date;
  cc text;
  phone_text text;
  country_text text;
begin
  base_username := coalesce(
    nullif(trim((new.raw_user_meta_data ->> 'username')::text), ''),
    split_part(new.email, '@', 1)
  );

  -- ensure username uniqueness by appending 8 chars of user id on conflict
  final_username := base_username;
  if exists(select 1 from public.profiles p where p.username = final_username) then
    final_username := base_username || '_' || substr(new.id::text, 1, 8);
  end if;

  dob_text := nullif(trim((new.raw_user_meta_data ->> 'date_of_birth')::text), '');
  begin
    parsed_dob := case when dob_text is null then null else to_date(dob_text, 'YYYY-MM-DD') end;
  exception when others then
    parsed_dob := null; -- ignore malformed input
  end;

  country_text := nullif(trim((new.raw_user_meta_data ->> 'country')::text), '');
  cc := nullif(trim((new.raw_user_meta_data ->> 'country_code')::text), '');
  phone_text := nullif(trim((new.raw_user_meta_data ->> 'phone')::text), '');

  insert into public.profiles(id, email, username, date_of_birth, country, country_code, phone)
  values (new.id, new.email, final_username, parsed_dob, country_text, cc, phone_text)
  on conflict (id) do nothing; -- id uniqueness guarantees single row per auth user

  return new;
end;
$$ language plpgsql security definer set search_path = public;

-- Create trigger on auth.users after insert
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();
