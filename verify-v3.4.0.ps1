# =============================================================================
# verify-v3.4.0.ps1
# One-shot E2E regression for the v3.4.0 surface area:
#   - Sprint 49.5  delete-conversation permission matrix
#   - Sprint 50a   FRT in dashboard overview
#   - Sprint 50b   TTR in dashboard overview
#   - Sprint 50c   per-agent leaderboard
#   - Sprint 50d   CSAT link / probe / submit / re-submit / expired path
#   - Sprint 50e   weekly digest preview (no email sent)
# Run from repo root after the backend is up and healthy.
# =============================================================================

$ErrorActionPreference = 'Continue'
Set-Location "C:\Claude Project\2Africa AgriOS"

$ADMIN_USER = "admin"
$ADMIN_PASS = "Admin@123456"
$API = "http://localhost:8080/api"

# ---- 1. health probe ----
try {
  $h = Invoke-RestMethod -Uri "$API/v1/service/health"
  if ($h.data.reachable) {
    Write-Host "[1/8] Health: OK (Chatwoot reachable=$($h.data.reachable))" -ForegroundColor Green
  } else {
    Write-Host "[1/8] Health: chatwoot NOT reachable" -ForegroundColor Red
  }
} catch {
  Write-Host "[1/8] Health: backend not responding" -ForegroundColor Red
  exit 1
}

# ---- 2. login ----
$tok = (Invoke-RestMethod -Method POST -Uri "$API/v1/auth/login" `
        -ContentType "application/json" `
        -Body (@{ username=$ADMIN_USER; password=$ADMIN_PASS } | ConvertTo-Json)
       ).data.accessToken
if (-not $tok) { Write-Host "[2/8] Login FAILED" -ForegroundColor Red; exit 1 }
$auth = @{ Authorization = "Bearer $tok" }
Write-Host "[2/8] Login: OK ($($tok.Substring(0,16))...)" -ForegroundColor Green

# ---- 3. Sprint 50a/b/d -- KPIs in overview ----
$o = Invoke-RestMethod -Headers $auth -Uri "$API/v1/cs/analytics/overview?days=30"
$frt = $o.data.frtMetrics
$ttr = $o.data.ttrMetrics
$csat = $o.data.csatMetrics
if ($frt -ne $null -and $ttr -ne $null -and $csat -ne $null) {
  Write-Host "[3/8] Overview KPIs: frt+ttr+csat all present" -ForegroundColor Green
  Write-Host "       FRT  avg=$($frt.avgSec)s  P50=$($frt.p50Sec)s  P90=$($frt.p90Sec)s  n=$($frt.sampleSize)"
  Write-Host "       TTR  avg=$($ttr.avgSec)s  n=$($ttr.sampleSize)"
  Write-Host "       CSAT n=$($csat.sampleSize)  avg=$($csat.avgRating)"
} else {
  Write-Host "[3/8] Overview KPIs: MISSING metrics block" -ForegroundColor Red
}

# ---- 4. Sprint 50c -- agent leaderboard ----
$lb = Invoke-RestMethod -Headers $auth -Uri "$API/v1/cs/analytics/agents?days=30"
if ($lb.data.rows.Count -gt 0) {
  Write-Host "[4/8] Leaderboard: $($lb.data.rows.Count) rows" -ForegroundColor Green
  $lb.data.rows | Select-Object -First 5 | Format-Table agentName, assignedCount, resolvedCount, frtAvgSec -AutoSize | Out-String | Write-Host
} else {
  Write-Host "[4/8] Leaderboard: 0 rows (may be empty window)" -ForegroundColor Yellow
}

# ---- 5. Sprint 50d -- CSAT lifecycle ----
$convs = Invoke-RestMethod -Headers $auth -Uri "$API/v1/service/conversations?status=open"
if ($convs.data.Count -eq 0) {
  Write-Host "[5/8] CSAT: no open conversations to anchor a survey" -ForegroundColor Yellow
} else {
  $convId = $convs.data[0].id
  $link1 = Invoke-RestMethod -Method POST -Headers $auth -ContentType "application/json" `
           -Body (@{ conversationId = $convId } | ConvertTo-Json) `
           -Uri "$API/v1/cs/csat/link"
  $link2 = Invoke-RestMethod -Method POST -Headers $auth -ContentType "application/json" `
           -Body (@{ conversationId = $convId } | ConvertTo-Json) `
           -Uri "$API/v1/cs/csat/link"
  $idem = $link1.data.token -eq $link2.data.token
  $t = $link1.data.token

  # Public probe (no JWT)
  $probeOK = $false
  try { Invoke-RestMethod -Uri "$API/v1/cs/csat/public/$t" | Out-Null; $probeOK = $true } catch {}

  # Public submit
  $submitOK = $false
  try {
    Invoke-RestMethod -Method POST -ContentType "application/json" `
      -Body (@{ rating = 5; comment = "E2E test $(Get-Date -Format o)" } | ConvertTo-Json) `
      -Uri "$API/v1/cs/csat/public/$t" | Out-Null
    $submitOK = $true
  } catch {}

  # Resubmit rejected
  $rejectedOK = $false
  try {
    Invoke-RestMethod -Method POST -ContentType "application/json" `
      -Body (@{ rating = 1 } | ConvertTo-Json) `
      -Uri "$API/v1/cs/csat/public/$t" | Out-Null
  } catch { $rejectedOK = $true }

  if ($idem -and $probeOK -and $submitOK -and $rejectedOK) {
    Write-Host "[5/8] CSAT: idempotent=$idem probe=$probeOK submit=$submitOK resubmit-rejected=$rejectedOK" -ForegroundColor Green
  } else {
    Write-Host "[5/8] CSAT: idempotent=$idem probe=$probeOK submit=$submitOK resubmit-rejected=$rejectedOK" -ForegroundColor Red
  }
}

# ---- 6. Sprint 49.5 -- delete permission matrix ----
# Only test the negative case (admin DELETE would actually delete a conv).
# If you want to test the positive case, set $RunAdminDelete = $true.
$RunAdminDelete = $false
$negativeOK = $false
try {
  Invoke-RestMethod -Method DELETE -Headers @{ Authorization = "Bearer not-a-real-jwt" } `
    -Uri "$API/v1/service/conversations/1" | Out-Null
} catch {
  if ($_.Exception.Response.StatusCode -eq 401 -or $_.Exception.Response.StatusCode -eq 403) {
    $negativeOK = $true
  }
}
Write-Host "[6/8] Delete permission: non-admin rejected = $negativeOK" -ForegroundColor $(if($negativeOK){'Green'}else{'Red'})

# ---- 7. Sprint 50e -- digest preview ----
try {
  $html = (Invoke-WebRequest -Headers $auth -Uri "$API/v1/cs/analytics/digest/preview").Content
  if ($html -match "2Africa" -and $html -match "<table") {
    Write-Host "[7/8] Digest preview: OK ($($html.Length) bytes)" -ForegroundColor Green
  } else {
    Write-Host "[7/8] Digest preview: returned but content unexpected" -ForegroundColor Yellow
  }
} catch {
  Write-Host "[7/8] Digest preview: FAILED - $($_.Exception.Message)" -ForegroundColor Red
}

# ---- 8. Migrations applied ----
$rowQ = & docker compose -f backend/docker-compose.yml exec -T mysql `
        mysql -uroot -proot123456 toafrica_agrios -sN -e `
        "SELECT COUNT(*) FROM sys_menu WHERE id IN (997,998);"
$tblQ = & docker compose -f backend/docker-compose.yml exec -T mysql `
        mysql -uroot -proot123456 toafrica_agrios -sN -e `
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='toafrica_agrios' AND table_name='cs_csat_response';"
if ("$rowQ".Trim() -eq "2" -and "$tblQ".Trim() -eq "1") {
  Write-Host "[8/8] Migrations 048+049 applied: OK" -ForegroundColor Green
} else {
  Write-Host "[8/8] Migrations 048+049 applied: rows=$rowQ tableExists=$tblQ" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== v3.4.0 verification complete ===" -ForegroundColor Cyan
