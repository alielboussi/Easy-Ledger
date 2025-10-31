// Supabase Edge Function: send-otp
// Deploy with: supabase functions deploy send-otp --no-verify-jwt
// Environment: Use service role key for admin client (automatically provided in Edge Functions)

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const FROM_EMAIL = Deno.env.get("FROM_EMAIL") ?? "no-reply@yourdomain.com";

function generateCode(): string {
  // 4-character alphanumeric (A-Z0-9)
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  let out = "";
  for (let i = 0; i < 4; i++) out += chars[Math.floor(Math.random() * chars.length)];
  return out;
}

serve(async (req) => {
  try {
    const { email } = await req.json();
    if (!email) return new Response(JSON.stringify({ ok: false, message: "email required" }), { status: 400 });

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

    const code = generateCode();
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString(); // 10 minutes

    // upsert record
    const { error } = await supabase.from("email_otps").insert({ email, code, expires_at: expiresAt });
    if (error) throw error;

    // Send email - using built-in mail providers is up to you; here we log or integrate with Resend/SendGrid
    // For demo, just log; replace with your mail sending integration
    console.log(`OTP for ${email}: ${code}`);

    return new Response(JSON.stringify({ ok: true }), { headers: { "Content-Type": "application/json" } });
  } catch (e) {
    console.error(e);
    return new Response(JSON.stringify({ ok: false, message: String(e?.message ?? e) }), { status: 500 });
  }
});
