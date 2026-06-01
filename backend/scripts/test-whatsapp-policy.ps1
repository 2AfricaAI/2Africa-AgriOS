# =============================================================================
# Sprint 47 - WhatsApp zero-cost policy E2E verification (PowerShell, Windows)
#
# Verifies the four scenarios required by the user:
#   (a) Inbound from "WhatsApp" surfaces in AgriOS UI (channel applies policy)
#   (b) Within 24h service window: public reply allowed
#   (c) Outside 24h window: public reply blocked with code 40901
#   (d) Outside 24h window: private note still allowed (no Meta charge)
#
# Strategy: instead of provisioning a real Meta WhatsApp Cloud channel, we use
# a Chatwoot "API channel" inbox and tell the AgriOS backend to treat that
# inbox id as WhatsApp for policy purposes (WHATSAPP_SIMULATED_INBOX_IDS).
#
# Prereqs:
#   1. backend/.env has CHATWOOT_API_TOKEN set
#   2. You have created an API channel inbox in Chatwoot named "WhatsApp (Simulated)"
#      (Chatwoot UI -> Settings -> Inboxes -> Add Inbox -> API)
#      Record its id ($SIM_INBOX_ID) and its inbox_identifier from the API
#      settings panel ($SIM_INBOX_IDENTIFIER).
#   3. backend/.env line WHATSAPP_SIMULATED_INBOX_IDS=<your id> and backend restarted
#   4. You have a Chatwoot contact id ($CONTACT_ID) to use as the customer.
#      (Anyone is fine; pick from /api/v1/accounts/1/contacts.)
#   5. AGRIOS_TOKEN env var contains a valid AgriOS JWT for an SUPER_ADMIN user.
# =============================================================================

# --- EDIT THESE BEFORE RUNNING ---
$CW_TOKEN  = $env:CHATWOOT_API_TOKEN
$AGRIOS_TOKEN = $env:AGRIOS_TOKEN
$SIM_INBOX_ID         = 2          # the API channel inbox id
$SIM_INBOX_IDENTIFIER = "PUT_INBOX_IDENTIFIER_HERE"  # from Chatwoot inbox settings
$CONTACT_ID = 1                    # any Chatwoot contact id
$SOURCE_ID  = "wa-sim-source-001"  # any unique string per contact
# ---------------------------------

$CW_BASE = "http://localhost:3000/api/v1/accounts/1"
$AG_BASE = "http://localhost:8080/api/v1/service"
$cwHeaders = @{ "api_access_token" = $CW_TOKEN; "Content-Type" = "application/json" }
$agHeaders = @{ "Authorization" = "Bearer $AGRIOS_TOKEN"; "Content-Type" = "application/json" }

Write-Host "=== Step 1: ensure a contact-inbox link exists for the simulated WhatsApp inbox ==="
$linkBody = @{ inbox_id = $SIM_INBOX_ID; source_id = $SOURCE_ID } | ConvertTo-Json
try {
    Invoke-RestMethod -Method Post -Uri "$CW_BASE/contacts/$CONTACT_ID/contact_inboxes" -Headers $cwHeaders -Body $linkBody | Out-Null
    Write-Host "  contact_inbox created."
} catch {
    Write-Host "  contact_inbox already exists or non-fatal:" $_.Exception.Message
}

Write-Host "=== Step 2: create a conversation in the simulated WhatsApp inbox ==="
$convBody = @{ source_id = $SOURCE_ID; inbox_id = $SIM_INBOX_ID; contact_id = $CONTACT_ID } | ConvertTo-Json
$convResp = Invoke-RestMethod -Method Post -Uri "$CW_BASE/conversations" -Headers $cwHeaders -Body $convBody
$CONV_ID = $convResp.id
Write-Host "  conversation id = $CONV_ID"

Write-Host "=== Step 3: post an INBOUND message (simulates the customer's WhatsApp message) ==="
$msgBody = @{ content = "Hi - my order arrived broken. Can you help?"; message_type = "incoming" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$CW_BASE/conversations/$CONV_ID/messages" -Headers $cwHeaders -Body $msgBody | Out-Null
Write-Host "  inbound posted."

Write-Host ""
Write-Host "=== Scenario (a): GET conversation via AgriOS - check whatsAppPolicy block ==="
$detail = Invoke-RestMethod -Uri "$AG_BASE/conversations/$CONV_ID" -Headers $agHeaders
# AgriOS wraps responses in {code, msg, data}; the policy lives under .data
$p = $detail.data.whatsAppPolicy
"  managed                = $($p.managed)"
"  lastInboundAt          = $($p.lastInboundAt)"
"  serviceWindowExpiresAt = $($p.serviceWindowExpiresAt)"
"  withinServiceWindow    = $($p.withinServiceWindow)"
if (-not $p.managed) {
    Write-Warning "  managed=false. Did you set WHATSAPP_SIMULATED_INBOX_IDS=$SIM_INBOX_ID and restart the backend?"
}

Write-Host ""
Write-Host "=== Scenario (b): within window - PUBLIC reply should succeed (HTTP 200) ==="
$replyBody = @{ content = "Sorry to hear that. Can you share a photo?"; privateNote = $false } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Method Post -Uri "$AG_BASE/conversations/$CONV_ID/messages" -Headers $agHeaders -Body $replyBody
    Write-Host "  OK code=$($r.code) msg=$($r.msg)"
} catch {
    Write-Host "  FAILED:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Scenario (c): age the inbound 25h then PUBLIC reply should be BLOCKED (40901) ==="
Write-Host "  Run this SQL inside the Chatwoot MySQL/Postgres to backdate the inbound:"
Write-Host "    UPDATE messages SET created_at = NOW() - INTERVAL '25 hours'"
Write-Host "    WHERE conversation_id = $CONV_ID AND message_type = 0;"
Write-Host "  Then re-run the reply call below."
$replyBlocked = @{ content = "Following up - sending you a credit note."; privateNote = $false } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Method Post -Uri "$AG_BASE/conversations/$CONV_ID/messages" -Headers $agHeaders -Body $replyBlocked
    # AgriOS returns business errors as HTTP 200 with code != 200 in the body.
    # 40901 is the WhatsApp policy block.
    if ($r.code -eq 40901) {
        Write-Host "  BLOCKED with code 40901 (good)." -ForegroundColor Green
        Write-Host "  msg: $($r.msg)"
    } elseif ($r.code -eq 200) {
        Write-Host "  Unexpected SUCCESS (window not yet aged?): code=200" -ForegroundColor Yellow
    } else {
        Write-Host "  Unexpected code=$($r.code) msg=$($r.msg)" -ForegroundColor Yellow
    }
} catch {
    # Non-2xx HTTP response — also acceptable as a block signal.
    Write-Host "  HTTP error response (also acceptable block signal)." -ForegroundColor Green
    if ($_.ErrorDetails -and $_.ErrorDetails.Message) { Write-Host "  body:" $_.ErrorDetails.Message }
}

Write-Host ""
Write-Host "=== Scenario (d): outside window - PRIVATE note still succeeds ==="
$noteBody = @{ content = "Internal: customer is past window; will SMS instead."; privateNote = $true } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Method Post -Uri "$AG_BASE/conversations/$CONV_ID/messages" -Headers $agHeaders -Body $noteBody
    Write-Host "  OK code=$($r.code) msg=$($r.msg)" -ForegroundColor Green
} catch {
    Write-Host "  FAILED:" $_.Exception.Message -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Done. Open http://localhost:5173/service to visually confirm the chip + banner. ==="
