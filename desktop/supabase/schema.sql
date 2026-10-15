-- ============================================================================
--  AppBlocker cloud schema (Supabase / Postgres)
--  Paste this into Supabase - SQL Editor - New query - Run.
--  Gives you: per-user synced block config, device registry, and a realtime
--  command channel for "activate Strict on my other device" (both directions).
-- ============================================================================

-- 1) The user's synced block configuration (one row per user, shared by all
--    their devices). `data` holds lists, exceptions, filter + schedule settings.
create table if not exists public.configs (
  user_id    uuid primary key references auth.users(id) on delete cascade,
  data       jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now(),
  updated_by text                      -- device id that last wrote it
);

-- 2) Each device the user signs in on (laptop, phone, ...).
create table if not exists public.devices (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references auth.users(id) on delete cascade,
  name       text not null,
  platform   text,                     -- 'windows' | 'macos' | 'android' | 'ios'
  last_seen  timestamptz not null default now(),
  created_at timestamptz not null default now()
);
create index if not exists devices_user_idx on public.devices(user_id);

-- 3) Remote commands between devices (e.g. phone -> laptop "strict_on").
--    target_device null = all of the user's devices.
create table if not exists public.commands (
  id            uuid primary key default gen_random_uuid(),
  user_id       uuid not null references auth.users(id) on delete cascade,
  target_device uuid references public.devices(id) on delete cascade,
  type          text not null,         -- 'strict_on' | 'strict_off' | 'lock_all' | 'ping'
  payload       jsonb not null default '{}'::jsonb,
  created_at    timestamptz not null default now(),
  consumed      boolean not null default false
);
create index if not exists commands_user_idx on public.commands(user_id, consumed);

-- ---------------------------------------------------------------------------
-- Row-level security: a user can only ever see or change their OWN rows.
-- ---------------------------------------------------------------------------
alter table public.configs  enable row level security;
alter table public.devices  enable row level security;
alter table public.commands enable row level security;

create policy "own configs"  on public.configs
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own devices"  on public.devices
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own commands" on public.commands
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ---------------------------------------------------------------------------
-- Realtime: push config changes and new commands to the user's devices.
-- (In the dashboard: Database - Replication - enable these, or run this.)
-- ---------------------------------------------------------------------------
alter publication supabase_realtime add table public.configs;
alter publication supabase_realtime add table public.commands;

-- Done. Create a user under Authentication - Users (or let the app sign up),
-- then sign in from the desktop app and your phone with the same account.
