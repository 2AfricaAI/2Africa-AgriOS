-- ============================================================================
-- Sprint 37: Six new built-in roles for external partners + customer self.
--
--   AGRONOMIST     - remote agronomy advisor; reads production + qc data
--   GAP_AUDITOR    - export certification auditor; reads qc + gap reports + trace
--   BANK_OFFICER   - lender / loan officer; reads finance reports + ar + cash-flow
--   LANDLORD       - landowner sharing plots; reads plots + harvests (scoped)
--   INSURANCE      - claim adjuster; reads activities + harvests + qc (scoped)
--   CUSTOMER_SELF  - end customer self-service; reads only their own orders +
--                    statements + payments via /portal
--
-- All six are is_built_in=1 so the UI Edit/Delete buttons are locked.
-- ============================================================================

INSERT INTO sys_role (code, name, data_scope, is_built_in, remark) VALUES
('AGRONOMIST',    'Agronomy advisor',       'group', 1, 'External agronomist - production + QC read'),
('GAP_AUDITOR',   'Export GAP auditor',     'all',   1, 'GlobalGAP / export auditor - QC + trace read'),
('BANK_OFFICER',  'Loan officer',           'all',   1, 'Bank / cooperative - finance read'),
('LANDLORD',      'Landowner',              'self',  1, 'Landlord seeing rented plots'),
('INSURANCE',     'Insurance adjuster',     'self',  1, 'Insurance claim adjuster'),
('CUSTOMER_SELF', 'Customer self-service',  'self',  1, 'Customer logged in via /portal');

-- ----------------------------------------------------------------------------
-- AGRONOMIST: production read + qc read + master read (varieties / inputs)
-- ----------------------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'AGRONOMIST'
   AND m.code IN ('home','master','production','qc',
                  'master.crops','master.varieties','master.inputs',
                  'production.plots','production.plans','production.activities',
                  'production.harvests','production.batches',
                  'qc.inspections','qc.trace','qc.gapReports');

-- ----------------------------------------------------------------------------
-- GAP_AUDITOR: qc + batches + activities (read) + export GAP report
-- ----------------------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'GAP_AUDITOR'
   AND m.code IN ('home','qc','production',
                  'qc.inspections','qc.trace','qc.gapReports','qc.gapReports.export',
                  'production.batches','production.activities','production.harvests');

-- ----------------------------------------------------------------------------
-- BANK_OFFICER: finance + procurement AR view (read)
-- ----------------------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'BANK_OFFICER'
   AND m.code IN ('home','finance','procurement',
                  'finance.reports','finance.ar','finance.cashFlow','finance.monthly',
                  'procurement.ap');

-- ----------------------------------------------------------------------------
-- LANDLORD: just plots + harvests (limited by sys_user_scope at service layer)
-- ----------------------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'LANDLORD'
   AND m.code IN ('home','production',
                  'production.plots','production.harvests','production.batches');

-- ----------------------------------------------------------------------------
-- INSURANCE: activities + harvests + qc inspections (read, time-windowed)
-- ----------------------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'INSURANCE'
   AND m.code IN ('home','production','qc',
                  'production.activities','production.harvests',
                  'qc.inspections','qc.trace');

-- ----------------------------------------------------------------------------
-- CUSTOMER_SELF: no desktop menus. Their entry is /portal pages which check
-- userType=CUSTOMER + linked_customer_id at the service layer.  We still bind
-- the home menu so the JWT has at least one perm string to anchor on.
-- ----------------------------------------------------------------------------
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'CUSTOMER_SELF'
   AND m.code IN ('home','sales','sales.orders','finance.ar');
