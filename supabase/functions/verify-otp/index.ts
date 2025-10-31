// Supabase Edge Function: verify-otp
// Deploy with: supabase functions deploy verify-otp --no-verify-jwt

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

serve(async (req) => {
  try {
    const { email, code } = await req.json();
    if (!email || !code) return new Response(JSON.stringify({ ok: false, message: "email and code required" }), { status: 400 });

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

    const { data, error } = await supabase
      .from("email_otps")
      .select("id, expires_at, used")
      .eq("email", email)
      .eq("code", code)
      .order("id", { ascending: false })
      .limit(1);

    if (error) throw error;
    const rec = data?.[0];
    if (!rec) return new Response(JSON.stringify({ ok: false, message: "Invalid code" }), { status: 200 });

    if (rec.used) return new Response(JSON.stringify({ ok: false, message: "Code already used" }), { status: 200 });

    if (new Date(rec.expires_at).getTime() < Date.now()) {
      return new Response(JSON.stringify({ ok: false, message: "Code expired" }), { status: 200 });
    }

    // mark used
    const { error: updErr } = await supabase.from("email_otps").update({ used: true }).eq("id", rec.id);
    if (updErr) throw updErr;

    return new Response(JSON.stringify({ ok: true }), { headers: { "Content-Type": "application/json" } });
  } catch (e) {
    console.error(e);
    return new Response(JSON.stringify({ ok: false, message: String(e?.message ?? e) }), { status: 500 });
  }
});
