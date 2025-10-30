# EasyLedger (Android, Kotlin, Compose)

A cashbook-style business app for tracking income and expenses with Google Sign-In and Supabase backend.

## Notes on OS/Device Support
- Google Play requires recent targetSdk. This project targets Android 15 (API 35) and compiles with 35.
- Minimum supported SDK is set to API 23 to support modern libraries (Compose, Google Sign-In, Supabase-kt). API 18 (requested) is not supported by these libraries; however, virtually all Samsung devices in use today are >= API 23.
- UI is built with Jetpack Compose for automatic screen size adjustments and dark/light/system themes.

## Setup
1. Create a Supabase project and enable Google provider in Authentication.
2. Set the app redirect URI (e.g., `easyledger://auth-callback`) in Supabase Auth provider settings.
3. Put secrets in `local.properties` (never commit):
	- `SUPABASE_URL=https://YOUR-PROJECT.supabase.co`
	- `SUPABASE_ANON_KEY=YOUR-ANON-KEY`
	The build injects these as `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY` used by `SupabaseProvider`.
	- Never include the Service Role key in the app or in client code. It must only be used on a trusted server.
4. Open in Android Studio (recommended) or VS Code with Android extensions.

## Build
- You’ll need JDK 17 and Android SDK installed.
- First build will download Gradle and dependencies.

## Database
- Run SQL scripts in `supabase/` in order:
	1) `schema_and_policies.sql` — tables, relationships, constraints, functions, RLS, rollups, indexes, views
	2) `storage_reports_bucket.sql` — creates the `reports` bucket and per-user access policies

## Roadmap
- Implement Google OAuth via supabase-kt with PKCE.
- Data model and repositories with PostgREST and RLS.
- Business, Sub-business, Categories, Transactions CRUD.
- Dashboard rollups per currency with instant indexed queries.
- Reports with charts and PDFs, uploaded to Supabase Storage bucket `reports` and protected with RLS storage policies.
