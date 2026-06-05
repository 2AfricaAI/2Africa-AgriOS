# 2Africa AgriOS — Development State at Pause

> Paused on **2026-06-05** at **v3.5.0-rc2**.
> Use this file to resume development with zero ramp-up.

---

## 1. What's locked in production-ready state

| Tag | Date | Scope |
|---|---|---|
| **v3.4.0** | 2026-06-04 | CS Analytics + CSAT + Weekly Digest + SUPER_ADMIN delete-conversation |
| **v3.5.0-rc1** | 2026-06-05 | Sprint 51: ORG model + @DataScope subsystem |
| **v3.5.0-rc2** | 2026-06-05 | Sprint 52 backend + JWT slim-token hotfix (this pause) |

Production-acceptable for v3.4.0 surface (CS-Core, ORG management, Workflow engine REST). Workflow has no UI yet, so end users cannot interact with it from the browser; backend modules can still call `WorkflowEngine.submit()` programmatically.

---

## 2. What's in v3.5.0-rc2 (this封版)

### A. JWT slim-token hotfix (was bloating to ~3KB)
- New `PermissionCacheService` — Redis `auth:perms:<uid>` cache, 1h TTL
- `AuthService.login` no longer puts perms into JWT (just roles + scope)
- `JwtAuthFilter` resolves perms from cache on each request; back-compat falls back to JWT claim if present (legacy tokens still work to TTL)
- **Effect: token size 3300 chars → 312 chars (10.6× smaller)**

### B. Sprint 52 -- Workflow engine (backend complete, no UI)
- `migration 051_workflow_engine.sql` — 5 tables: `wf_definition / wf_instance / wf_step / wf_audit / wf_delegation` + 3 builtin templates (hr.leave_request / admin.expense / finance.payment_out) + 5 permissions + DB triggers making `wf_audit` append-only
- 5 entities / 5 mappers under `backend/.../workflow/{entity,mapper}`
- `WorkflowSchema` + `WorkflowSchemaParser` — JSON DSL with snake_case Jackson naming
- `WorkflowEngine` — submit, advance, resolveAssignee (incl. delegation), evaluateCondition (`amount > N` grammar)
- `WorkflowAuditService` — auto-fills IP/UA, every action writes here
- `WorkflowDelegationService` — resolveDelegatee with module scope
- `WorkflowStepService` — 5 actions: approve, reject, returnTo, delegate, withdraw + 3 read helpers (pendingForUser, countPendingForUser, stepsOfInstance)
- `WorkflowController` — 8 REST endpoints under `/v1/wf/*`, all behind `@PreAuthorize('wf:instance:...')`
- `WorkflowSlaScheduler` — `@Scheduled(cron = "0 */5 * * * *")` finds SLA breaches and escalates by writing `escalated_to_id` (no notification fired yet)

### C. Small fixes bundled
- `OrgTreePage.vue` (Sprint 51 follow-up): `listUsers` response shape was `list` not `rows`; fixed `users.map is not a function`

---

## 3. What's explicitly NOT done (deferred Day 5)

| Item | Where it will land |
|---|---|
| Frontend workflow badge (red dot in business module nav) | Sprint 53 HR Day 1 — first module to need it |
| Inline approve/reject buttons on business detail pages | Sprint 53 HR — wired into hr_leave_request first |
| `verify-sprint52.ps1` E2E script | Sprint 53 will combine with HR verify |
| Chatwoot/SMS outbound notification on SLA escalation | Sprint 53+ when HR notification needs surface |
| `WorkflowDelegationController` for users to manage their own delegations | Defer until HR sprint actually needs it |

These are all UI/notification polish. The engine is functionally complete and callable from any module.

---

## 4. Open decisions locked

8 decisions on ORG model (PRD-ORG-v0.2 § 0):
1. Node names English single-value
2. Single primary manager
3. Physical nodes never deletable
4. Cross-farm payroll by-day attribution
5. data_scope='all' role + audit log
6. 8-type node enum (incl. PROCESSING)
7. Zero-touch migration accepted
8. Independent decision-maker

8 strategic decisions on v3.5.0 direction:
- HR/Admin/Legal first; Finance + AI Brain pushed to v3.6.0
- AI Provider: mixed (Claude analysis / OpenAI customer service)
- AI Brain timing: v3.6.0
- 90-day metrics: HR-focused (7 indicators)
- Partner Network: v4.0.0
- Workflow engine: full done (this Sprint 52)

---

## 5. Sprint 53 -- starting point when resuming

PRD: `docs/PRD-HR-ADMIN-LEGAL-WORKFLOW-v0.2.md` § 4 (HR module)

Sprint 53 Day 1 plan:
- migration 052: `hr_employee` (PII-encrypted fields), `hr_contract`, `hr_contract_file` (MinIO WORM bucket reference)
- Permissions seed: `hr:employee:list / edit / view-pii`, `hr:contract:upload / view`
- Entity + Mapper + Service for employee onboarding
- @MaskOnSerialize AOP for PII fields (Sprint 53 Day 2)

When resuming: read this file + `docs/PRD-HR-ADMIN-LEGAL-WORKFLOW-v0.2.md` § 4, then run:

```powershell
cd "C:\Claude Project\2Africa AgriOS"
git pull
docker compose -f backend/docker-compose.yml up -d --build backend
& ".\verify-sprint51.ps1"  # confirm Sprint 51 surface still green
# then start migration 052
```

---

## 6. Roadmap snapshot at pause

```
✅ Sprint 38-48  Production/Warehouse/Sales/QC/CS foundation
✅ Sprint 49-50  CS Analytics + CSAT + Digest → v3.4.0
✅ Sprint 49.5   SUPER_ADMIN delete conversation hotfix
✅ Sprint 51     ORG model + @DataScope → v3.5.0-rc1
✅ Sprint 52     Workflow engine backend → v3.5.0-rc2 (paused)
⏸ Sprint 53     HR Day 1-5: employee + contract           ← resume here
⏸ Sprint 54     HR Day 6-10: attendance + rate card + piece-work
⏸ Sprint 55     HR Day 11-15: payroll + PAYE/NHIF/NSSF + M-Pesa B2C
⏸ Sprint 56     HR Day 16-20: leave + training + KPI (via workflow)
⏸ Sprint 57     HR Day 21-25: temp/seasonal worker handling
⏸ Sprint 58-59  Admin: assets / expenses / docs / licenses
⏸ Sprint 60-61  Legal: contracts / audit / DPA → v3.5.0
⏸ v3.6.0+       Finance & P&L + AI Farm Brain (PRD V2.0 phase 2-4)
⏸ v4.0.0+       Partner Network (PRD V2.0 phase 5)
```

---

## 7. Tech debt parked (review when resuming)

| # | Item | Severity |
|---|---|---|
| 1 | Workflow audit DB trigger only blocks at trigger level — DBA with SUPER privilege can still bypass. Sprint 60 will revoke SUPER from app DB user. | low |
| 2 | DataScope subsystem is OFF by default (`agrios.datascope.enabled=false`). Grey-out activation needs production validation. | medium |
| 3 | OrgUser primary uniqueness only enforced at service level, not DB. Race condition possible under concurrent assigns. | low |
| 4 | Workflow engine condition grammar is intentionally minimal (`amount > N`). Rich JEXL DSL deferred to Sprint 53+ if HR needs more. | low |
| 5 | JWT back-compat path in JwtAuthFilter reads legacy perms claim. Remove after all sessions older than the access token TTL (2h) have expired. | low — auto-resolves |
| 6 | `data_access_audit` rows are never archived. Sprint 60 audit-archive job will move > 6 month rows to MinIO. | low |

---

## 8. Files newly added during pause window (uncommitted before commit)

See `.git-commit-pause.txt` for the verbatim commit message.

```
22 files changed:
  + backend/.../framework/security/PermissionCacheService.java
  + backend/.../system/service/AuthService.java        (modified)
  + backend/.../framework/security/JwtAuthFilter.java  (modified)
  + migrations/051_workflow_engine.sql
  + migrations/051_workflow_engine_rollback.sql
  + backend/.../workflow/entity/{WfDefinition,WfInstance,WfStep,WfAudit,WfDelegation}.java
  + backend/.../workflow/mapper/{WfDefinitionMapper,WfInstanceMapper,WfStepMapper,WfAuditMapper,WfDelegationMapper}.java
  + backend/.../workflow/dsl/{WorkflowSchema,WorkflowSchemaParser}.java
  + backend/.../workflow/service/{WorkflowAuditService,WorkflowDelegationService,WorkflowEngine,WorkflowStepService,WorkflowSlaScheduler}.java
  + backend/.../workflow/controller/WorkflowController.java
  + frontend/src/views/org/OrgTreePage.vue              (Sprint 51 follow-up fix)
  + CHANGELOG.md                                         (entry for v3.5.0-rc2)
  + docs/STATE-AT-PAUSE-2026-06-05.md                    (this file)
```
