# =============================================================================
# verify-sprint51.ps1
# Sprint 51 (ORG model + @DataScope) E2E regression. Safe with datascope.enabled
# defaulting to false -- all existing pages continue working.
# Run from repo root.
# =============================================================================

$ErrorActionPreference = 'Continue'
Set-Location "C:\Claude Project\2Africa AgriOS"

$ADMIN_USER = "admin"
$ADMIN_PASS = "Admin@123456"
$API = "http://localhost:8080/api"

# ---- 1. health ----
try {
  $h = Invoke-RestMethod -Uri "$API/v1/service/health"
  if ($h.data.reachable) {
    Write-Host "[1/10] Health: OK" -ForegroundColor Green
  } else { Write-Host "[1/10] Health: NOT ready" -ForegroundColor Red }
} catch { Write-Host "[1/10] Health: backend down" -ForegroundColor Red; exit 1 }

# ---- 2. login ----
$tok = (Invoke-RestMethod -Method POST -Uri "$API/v1/auth/login" `
        -ContentType "application/json" `
        -Body (@{ username=$ADMIN_USER; password=$ADMIN_PASS } | ConvertTo-Json)
       ).data.accessToken
if (-not $tok) { Write-Host "[2/10] Login FAILED" -ForegroundColor Red; exit 1 }
$auth = @{ Authorization = "Bearer $tok" }
Write-Host "[2/10] Login: OK" -ForegroundColor Green

# ---- 3. org nodes 12 ----
$n = (Invoke-RestMethod -Headers $auth -Uri "$API/v1/org/nodes").data
if ($n.Count -eq 12) {
  Write-Host "[3/10] Org nodes: 12 OK" -ForegroundColor Green
} else { Write-Host "[3/10] Org nodes: expected 12, got $($n.Count)" -ForegroundColor Red }

# ---- 4. org tree nested ----
$t = (Invoke-RestMethod -Headers $auth -Uri "$API/v1/org/nodes/tree").data
$rootName = $t[0].name
if ($rootName -match "Albert") {
  Write-Host "[4/10] Org tree root: $rootName OK" -ForegroundColor Green
} else { Write-Host "[4/10] Org tree root unexpected: $rootName" -ForegroundColor Red }

# ---- 5. subtree of Albert's Farm (id=10) ----
$st = (Invoke-RestMethod -Headers $auth -Uri "$API/v1/org/nodes/10/subtree-ids").data
if ($st.Count -eq 7) {
  Write-Host "[5/10] Albert's Farm subtree: 7 ids OK" -ForegroundColor Green
} else { Write-Host "[5/10] Subtree: expected 7, got $($st.Count)" -ForegroundColor Red }

# ---- 6. Kang's primary node = 10 ----
$pn = (Invoke-RestMethod -Headers $auth -Uri "$API/v1/org/users/by-user/6/primary-node-id").data
if ($pn -eq 10) {
  Write-Host "[6/10] Kang primary node = 10 OK" -ForegroundColor Green
} else { Write-Host "[6/10] Kang primary unexpected: $pn" -ForegroundColor Red }

# ---- 7. physical node delete rejected ----
$rejected = $false
try {
  $r = Invoke-RestMethod -Method DELETE -Headers $auth -Uri "$API/v1/org/nodes/10"
  if ($r.code -eq 400) { $rejected = $true }
} catch { $rejected = $true }
Write-Host "[7/10] Physical node delete rejected: $rejected" -ForegroundColor $(if($rejected){'Green'}else{'Red'})

# ---- 8. tags ----
$tags = (Invoke-RestMethod -Headers $auth -Uri "$API/v1/org/tags").data
Write-Host "[8/10] Tags seeded: $($tags.Count) (expect >= 5)" -ForegroundColor $(if($tags.Count -ge 5){'Green'}else{'Yellow'})

# ---- 9. CS analytics still works (v3.4.0 surface preserved) ----
$o = Invoke-RestMethod -Headers $auth -Uri "$API/v1/cs/analytics/overview?days=30"
if ($o.data.frtMetrics -ne $null) {
  Write-Host "[9/10] CS analytics FRT still present OK" -ForegroundColor Green
} else { Write-Host "[9/10] CS analytics broken!" -ForegroundColor Red }

# ---- 10. data_access_audit empty (datascope.enabled=false default) ----
$rowQ = & docker compose -f backend/docker-compose.yml exec -T mysql `
        mysql -uroot -proot123456 toafrica_agrios -sN -e `
        "SELECT COUNT(*) FROM data_access_audit;"
if ("$rowQ".Trim() -eq "0") {
  Write-Host "[10/10] data_access_audit empty (datascope off) OK" -ForegroundColor Green
} else {
  Write-Host "[10/10] data_access_audit has $rowQ rows (audit may be enabled)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Sprint 51 verification complete ===" -ForegroundColor Cyan
