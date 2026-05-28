-- ============================================================================
-- migration 039: Seed sys_menu + sys_role_menu for fine-grained RBAC (Sprint 35).
--
-- Naming convention for perms strings:  module:resource:action
--   e.g. master:crop:list, master:crop:add, master:crop:edit, master:crop:delete
--        finance:report:view, finance:report:export
--        system:user:list, system:user:add, system:user:reset-pwd
--
-- Three menu types:
--   type='dir'   - first-level container, no path, just groups children
--   type='menu'  - leaf menu, has path + component, gates page access
--   type='button'- action button under a menu, gates a specific operation
--
-- Idempotent: TRUNCATE both tables before re-inserting so this can be re-run
-- after schema changes without leaving orphan rows.
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE sys_role_menu;
TRUNCATE TABLE sys_menu;
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- Tier 1: directories (first-level menu groups) - id range 1..20
-- ----------------------------------------------------------------------------
INSERT INTO sys_menu (id, parent_id, code, name, type, path, icon, sort, visible) VALUES
( 1, 0, 'home',         'Home',         'menu', '/',                   'HomeFilled',     10, 1),
( 2, 0, 'master',       'Master Data',  'dir',  NULL,                  'Goods',          20, 1),
( 3, 0, 'production',   'Production',   'dir',  NULL,                  'Sunny',          30, 1),
( 4, 0, 'warehouse',    'Warehouse',    'dir',  NULL,                  'Goods',          40, 1),
( 5, 0, 'qc',           'Quality',      'dir',  NULL,                  'CircleCheck',    50, 1),
( 6, 0, 'packhouse',    'Packhouse',    'dir',  NULL,                  'Box',            60, 1),
( 7, 0, 'sales',        'Sales',        'dir',  NULL,                  'Wallet',         70, 1),
( 8, 0, 'operations',   'Operations',   'dir',  NULL,                  'Bell',           80, 1),
( 9, 0, 'finance',      'Finance',      'dir',  NULL,                  'Money',          90, 1),
(10, 0, 'procurement',  'Procurement',  'dir',  NULL,                  'ShoppingCart',  100, 1),
(11, 0, 'system',       'System',       'dir',  NULL,                  'Setting',       200, 1);

-- ----------------------------------------------------------------------------
-- Tier 2: leaf menus + their action buttons - id range 100..999
--
-- For each menu we record: list (view) + add/edit/delete/... (actions)
-- ----------------------------------------------------------------------------

-- Master Data
INSERT INTO sys_menu (id, parent_id, code, name, type, path, perms, sort, visible) VALUES
(100, 2, 'master.crops',           'Crops',           'menu',   '/master/crops',           'master:crop:list',           10, 1),
(101, 100, 'master.crops.add',     'Add crop',        'button', NULL,                      'master:crop:add',            11, 1),
(102, 100, 'master.crops.edit',    'Edit crop',       'button', NULL,                      'master:crop:edit',           12, 1),
(103, 100, 'master.crops.delete',  'Delete crop',     'button', NULL,                      'master:crop:delete',         13, 1),

(110, 2, 'master.varieties',         'Varieties',     'menu',   '/master/varieties',       'master:variety:list',        20, 1),
(111, 110, 'master.varieties.add',   'Add variety',   'button', NULL,                      'master:variety:add',         21, 1),
(112, 110, 'master.varieties.edit',  'Edit variety',  'button', NULL,                      'master:variety:edit',        22, 1),
(113, 110, 'master.varieties.delete','Delete variety','button', NULL,                      'master:variety:delete',      23, 1),

(120, 2, 'master.pkgspec',         'Packaging specs',  'menu',   '/master/packaging-specs','master:packaging-spec:list', 30, 1),
(121, 120, 'master.pkgspec.add',   'Add spec',         'button', NULL,                     'master:packaging-spec:add',  31, 1),
(122, 120, 'master.pkgspec.edit',  'Edit spec',        'button', NULL,                     'master:packaging-spec:edit', 32, 1),
(123, 120, 'master.pkgspec.delete','Delete spec',      'button', NULL,                     'master:packaging-spec:delete',33,1),

(130, 2, 'master.warehouses',         'Warehouses',     'menu',   '/master/warehouses',    'master:warehouse:list',      40, 1),
(131, 130, 'master.warehouses.add',   'Add warehouse',  'button', NULL,                    'master:warehouse:add',       41, 1),
(132, 130, 'master.warehouses.edit',  'Edit warehouse', 'button', NULL,                    'master:warehouse:edit',      42, 1),
(133, 130, 'master.warehouses.delete','Delete warehouse','button',NULL,                    'master:warehouse:delete',    43, 1),

(140, 2, 'master.inputs',         'Input items',      'menu',   '/master/input-items',     'master:input-item:list',     50, 1),
(141, 140, 'master.inputs.add',   'Add input item',   'button', NULL,                      'master:input-item:add',      51, 1),
(142, 140, 'master.inputs.edit',  'Edit input item',  'button', NULL,                      'master:input-item:edit',     52, 1),
(143, 140, 'master.inputs.delete','Delete input item','button', NULL,                      'master:input-item:delete',   53, 1),

(150, 2, 'master.inputStock', 'Input stock', 'menu', '/master/input-stock', 'master:input-stock:list', 60, 1),
(151, 2, 'master.stockLog',   'Stock log',   'menu', '/master/stock-log',   'master:stock-log:list',   70, 1);

-- Production
INSERT INTO sys_menu (id, parent_id, code, name, type, path, perms, sort, visible) VALUES
(200, 3, 'production.plots',         'Plots',         'menu',   '/production/plots',         'production:plot:list',          10, 1),
(201, 200, 'production.plots.add',   'Add plot',      'button', NULL,                        'production:plot:add',           11, 1),
(202, 200, 'production.plots.edit',  'Edit plot',     'button', NULL,                        'production:plot:edit',          12, 1),
(203, 200, 'production.plots.delete','Delete plot',   'button', NULL,                        'production:plot:delete',        13, 1),

(210, 3, 'production.plans',         'Planting plans','menu',   '/production/planting-plans','production:planting-plan:list', 20, 1),
(211, 210, 'production.plans.add',   'Add plan',      'button', NULL,                        'production:planting-plan:add',  21, 1),
(212, 210, 'production.plans.edit',  'Edit plan',     'button', NULL,                        'production:planting-plan:edit', 22, 1),
(213, 210, 'production.plans.delete','Delete plan',   'button', NULL,                        'production:planting-plan:delete',23,1),

(220, 3, 'production.activities',         'Activities',     'menu',   '/production/activities',  'production:activity:list',    30, 1),
(221, 220, 'production.activities.add',   'Record activity','button', NULL,                      'production:activity:add',     31, 1),
(222, 220, 'production.activities.edit',  'Edit activity',  'button', NULL,                      'production:activity:edit',    32, 1),
(223, 220, 'production.activities.delete','Delete activity','button', NULL,                      'production:activity:delete',  33, 1),

(230, 3, 'production.harvests',       'Harvests',     'menu',   '/production/harvests',  'production:harvest:list',     40, 1),
(231, 230, 'production.harvests.add', 'Record harvest','button',NULL,                     'production:harvest:add',      41, 1),
(232, 230, 'production.harvests.edit','Edit harvest', 'button', NULL,                     'production:harvest:edit',     42, 1),

(240, 3, 'production.batches',        'Batches',      'menu',   '/production/batches',   'production:batch:list',       50, 1),
(241, 240, 'production.batches.split','Split batch',  'button', NULL,                     'production:batch:split',      51, 1);

-- Warehouse
INSERT INTO sys_menu (id, parent_id, code, name, type, path, perms, sort, visible) VALUES
(300, 4, 'warehouse.inbound',          'Inbound',        'menu',   '/warehouse/inbound',  'warehouse:inbound:list',     10, 1),
(301, 300, 'warehouse.inbound.add',    'New inbound',    'button', NULL,                  'warehouse:inbound:add',      11, 1),
(302, 300, 'warehouse.inbound.confirm','Confirm inbound','button', NULL,                  'warehouse:inbound:confirm',  12, 1),

(310, 4, 'warehouse.outbound',          'Outbound',       'menu',   '/warehouse/outbound', 'warehouse:outbound:list',    20, 1),
(311, 310, 'warehouse.outbound.pick',   'Pick',           'button', NULL,                  'warehouse:outbound:pick',    21, 1),
(312, 310, 'warehouse.outbound.confirm','Confirm shipped','button', NULL,                  'warehouse:outbound:confirm', 22, 1),

(320, 4, 'warehouse.stocktake',         'Stocktake',     'menu',   '/warehouse/stocktake', 'warehouse:stocktake:list',   30, 1),
(321, 320, 'warehouse.stocktake.add',   'New stocktake', 'button', NULL,                   'warehouse:stocktake:add',    31, 1),
(322, 320, 'warehouse.stocktake.confirm','Confirm count','button', NULL,                   'warehouse:stocktake:confirm',32, 1),

(330, 4, 'warehouse.transfer',          'Transfer',      'menu',   '/warehouse/transfer',  'warehouse:transfer:list',    40, 1),
(331, 330, 'warehouse.transfer.add',    'New transfer',  'button', NULL,                   'warehouse:transfer:add',     41, 1),
(332, 330, 'warehouse.transfer.confirm','Confirm transfer','button',NULL,                  'warehouse:transfer:confirm', 42, 1),

(340, 4, 'warehouse.scrap',          'Scrap',           'menu',   '/warehouse/scrap',     'warehouse:scrap:list',     50, 1),
(341, 340, 'warehouse.scrap.add',    'New scrap',       'button', NULL,                   'warehouse:scrap:add',      51, 1),

(350, 4, 'warehouse.reports',        'Warehouse reports','menu',  '/warehouse/reports',   'warehouse:report:view',    60, 1);

-- QC
INSERT INTO sys_menu (id, parent_id, code, name, type, path, perms, sort, visible) VALUES
(400, 5, 'qc.inspections',           'QC inspections', 'menu',   '/qc/inspections',     'qc:inspection:list',  10, 1),
(401, 400, 'qc.inspections.add',     'New inspection', 'button', NULL,                  'qc:inspection:add',   11, 1),
(402, 400, 'qc.inspections.submit',  'Submit result',  'button', NULL,                  'qc:inspection:submit',12, 1),

(410, 5, 'qc.trace',     'Traceability',   'menu',   '/qc/trace',        'qc:trace:view',  20, 1),

(420, 5, 'qc.complaints',           'Complaints',  'menu',   '/qc/complaints',     'qc:complaint:list',   30, 1),
(421, 420, 'qc.complaints.add',     'New complaint','button',NULL,                  'qc:complaint:add',    31, 1),
(422, 420, 'qc.complaints.edit',    'Update complaint','button',NULL,               'qc:complaint:edit',   32, 1),

(430, 5, 'qc.recalls',              'Recalls',      'menu',   '/qc/recalls',       'qc:recall:list',      40, 1),
(431, 430, 'qc.recalls.add',        'New recall',   'button', NULL,                 'qc:recall:add',       41, 1),
(432, 430, 'qc.recalls.export',     'Export PDF',   'button', NULL,                 'qc:recall:export',    42, 1),

(440, 5, 'qc.gapReports',           'GAP reports',  'menu',   '/qc/gap-reports',   'qc:gap-report:view',  50, 1),
(441, 440, 'qc.gapReports.export',  'Export GAP',   'button', NULL,                 'qc:gap-report:export',51, 1);

-- Packhouse
INSERT INTO sys_menu (id, parent_id, code, name, type, path, perms, sort, visible) VALUES
(500, 6, 'packhouse.packings',       'Packings',  'menu',   '/packhouse/packings',  'packhouse:packing:list', 10, 1),
(501, 500, 'packhouse.packings.add', 'New packing','button',NULL,                   'packhouse:packing:add',  11, 1),

(510, 6, 'packhouse.inventory',      'Inventory', 'menu',   '/packhouse/inventory', 'packhouse:inventory:list',20, 1);

-- Sales
INSERT INTO sys_menu (id, parent_id, code, name, type, path, perms, sort, visible) VALUES
(600, 7, 'sales.customers',         'Customers',       'menu',   '/sales/customers',  'sales:customer:list',  10, 1),
(601, 600, 'sales.customers.add',   'Add customer',    'button', NULL,                'sales:customer:add',   11, 1),
(602, 600, 'sales.customers.edit',  'Edit customer',   'button', NULL,                'sales:customer:edit',  12, 1),
(603, 600, 'sales.customers.statement','View statement','button',NULL,                'sales:customer:statement',13, 1),

(610, 7, 'sales.orders',          'Orders',           'menu',   '/sales/orders',     'sales:order:list',     20, 1),
(611, 610, 'sales.orders.add',    'Create order',     'button', NULL,                'sales:order:add',      21, 1),
(612, 610, 'sales.orders.edit',   'Edit order',       'button', NULL,                'sales:order:edit',     22, 1),
(613, 610, 'sales.orders.confirm','Confirm order',    'button', NULL,                'sales:order:confirm',  23, 1),
(614, 610, 'sales.orders.payment','Record payment',   'button', NULL,                'sales:order:payment',  24, 1);

-- Operations
INSERT INTO sys_menu (id, parent_id, code, name, type, path, perms, sort, visible) VALUES
(700, 8, 'operations.actionBoard',         'Action board', 'menu',   '/operations/action-board', 'operations:action-board:list',   10, 1),
(701, 700, 'operations.actionBoard.done',  'Mark done',    'button', NULL,                       'operations:action-board:done',   11, 1),
(702, 700, 'operations.actionBoard.dismiss','Dismiss',     'button', NULL,                       'operations:action-board:dismiss',12, 1);

-- Finance
INSERT INTO sys_menu (id, parent_id, code, name, type, path, perms, sort, visible) VALUES
(800, 9, 'finance.reports',        'Financial reports','menu',   '/finance/reports',  'finance:report:view',   10, 1),
(801, 800, 'finance.reports.export','Export P&L',      'button', NULL,                'finance:report:export', 11, 1),

(810, 9, 'finance.ar',             'Accounts receivable','menu','/finance/ar',        'finance:ar:view',       20, 1),
(811, 810, 'finance.ar.collect',   'Collection log',   'button', NULL,                'finance:ar:collect',    21, 1),

(820, 9, 'finance.cashFlow',       'Cash flow forecast','menu',  '/finance/cash-flow','finance:cash-flow:view',30, 1),
(830, 9, 'finance.monthly',        'Monthly report',  'menu',   '/finance/monthly',  'finance:monthly:view',  40, 1);

-- Procurement
INSERT INTO sys_menu (id, parent_id, code, name, type, path, perms, sort, visible) VALUES
(900, 10,'procurement.suppliers',         'Suppliers',     'menu',   '/procurement/suppliers',  'procurement:supplier:list',  10, 1),
(901, 900,'procurement.suppliers.add',    'Add supplier',  'button', NULL,                      'procurement:supplier:add',   11, 1),
(902, 900,'procurement.suppliers.edit',   'Edit supplier', 'button', NULL,                      'procurement:supplier:edit',  12, 1),

(910, 10,'procurement.orders',         'Purchase orders','menu',   '/procurement/orders',     'procurement:po:list',        20, 1),
(911, 910,'procurement.orders.add',    'New PO',         'button', NULL,                      'procurement:po:add',         21, 1),
(912, 910,'procurement.orders.edit',   'Edit PO',        'button', NULL,                      'procurement:po:edit',        22, 1),
(913, 910,'procurement.orders.confirm','Confirm PO',     'button', NULL,                      'procurement:po:confirm',     23, 1),
(914, 910,'procurement.orders.receive','Receive PO',     'button', NULL,                      'procurement:po:receive',     24, 1),

(920, 10,'procurement.ap',             'Accounts payable','menu',  '/procurement/ap',         'procurement:ap:view',        30, 1),
(921, 920,'procurement.ap.pay',        'Record payment', 'button', NULL,                      'procurement:ap:pay',         31, 1);

-- System
INSERT INTO sys_menu (id, parent_id, code, name, type, path, perms, sort, visible) VALUES
(1000, 11,'system.users',             'Users',          'menu',   '/system/users',   'system:user:list',      10, 1),
(1001, 1000,'system.users.add',       'Add user',       'button', NULL,              'system:user:add',       11, 1),
(1002, 1000,'system.users.edit',      'Edit user',      'button', NULL,              'system:user:edit',      12, 1),
(1003, 1000,'system.users.resetPwd',  'Reset password', 'button', NULL,              'system:user:reset-pwd', 13, 1),
(1004, 1000,'system.users.status',    'Enable/disable', 'button', NULL,              'system:user:status',    14, 1),
(1005, 1000,'system.users.delete',    'Delete user',    'button', NULL,              'system:user:delete',    15, 1),

(1010, 11,'system.roles',             'Roles',          'menu',   '/system/roles',   'system:role:list',      20, 1),
(1011, 1010,'system.roles.assignMenu','Assign menus',   'button', NULL,              'system:role:assign-menu',21,1);

-- ----------------------------------------------------------------------------
-- Role -> Menu bindings
--
-- SUPER_ADMIN: code-level bypass in AuthService, but we still bind every menu
-- so the UI 'assign menus' tree shows a fully-checked state.
--
-- MANAGER: everything except System / role assignment.
--
-- LEADER (production team lead): production + warehouse view + qc + action-board.
--
-- PACKHOUSE: packhouse + inventory + qc inspections.
--
-- SALES: customers + orders + AR.
--
-- WORKER: read-only access to production list pages + activity/harvest record
-- on mobile.  WORKER cannot delete or edit other users' records.
-- ----------------------------------------------------------------------------

-- SUPER_ADMIN: bind every menu we just inserted
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'SUPER_ADMIN';

-- MANAGER: everything except System (parent dir 11)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'MANAGER'
   AND m.id <> 11
   AND m.parent_id <> 11
   AND NOT EXISTS (SELECT 1 FROM sys_menu p WHERE p.id = m.parent_id AND p.parent_id = 11);

-- LEADER: production + harvests + warehouse view + QC + action-board + home
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'LEADER'
   AND (
        m.id IN (1, 3, 4, 5, 8)
     OR m.parent_id IN (3, 8)
     OR m.code LIKE 'production.%'
     OR m.code LIKE 'operations.actionBoard%'
     OR m.code IN ('warehouse.inbound','warehouse.outbound','warehouse.reports','qc.inspections','qc.trace')
   );

-- PACKHOUSE: packhouse + inventory + QC inspections + home
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'PACKHOUSE'
   AND (
        m.id IN (1, 5, 6)
     OR m.parent_id = 6
     OR m.code LIKE 'packhouse.%'
     OR m.code LIKE 'qc.inspections%'
     OR m.code = 'qc.trace'
   );

-- SALES: customers + orders + AR + home
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'SALES'
   AND (
        m.id IN (1, 7, 9)
     OR m.parent_id = 7
     OR m.code LIKE 'sales.%'
     OR m.code LIKE 'finance.ar%'
     OR m.code = 'finance.reports'
   );

-- WORKER: home + production list-only access (no add/edit/delete buttons)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r CROSS JOIN sys_menu m
 WHERE r.code = 'WORKER'
   AND (
        m.id IN (1, 3)
     OR m.code IN ('production.plots','production.activities','production.harvests',
                   'production.activities.add','production.harvests.add')
   );
