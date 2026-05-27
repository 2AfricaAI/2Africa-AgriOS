-- ============================================================================
-- 2Africa AgriOS - Demo / Test Seed Data
--
-- *** CONVENTION (Sprint 21+) ***
--   ALL VALUES IN THIS FILE AND ALL FUTURE MIGRATIONS MUST BE ENGLISH-ONLY.
--
--   Reason 1 (target market): Kenya/Africa-facing product. Operators read
--                             English / Swahili, not Chinese.
--   Reason 2 (encoding safety): The Windows PowerShell + docker exec pipeline
--                               historically lost UTF-8 multibyte characters
--                               (Chinese -> '?'). Keeping seed data ASCII
--                               eliminates one whole class of transport bugs.
--
--   SQL comments (--) may still be in any language - they are developer
--   facing and never enter the database.
--
-- Scenario: 5 Kenyan plots + African staple/cash crops + 5 planting plans
-- Usage:
--   docker cp demo-data.sql toafrica-mysql:/tmp/
--   docker exec toafrica-mysql sh -c "mysql --default-character-set=utf8mb4 -ualberts -palberts123 toafrica_agrios < /tmp/demo-data.sql"
-- ============================================================================

USE `toafrica_agrios`;
SET NAMES utf8mb4;

-- ============================================================================
-- 0-PRE) Clean up so this script is idempotent (re-runnable)
--    Order matters due to FK semantics:
--    planting_plan → plot, planting_plan → crop, planting_plan → variety
-- ============================================================================
DELETE FROM `planting_plan`;
DELETE FROM `plot`;
DELETE FROM `variety` WHERE `crop_id` > 4;       -- keep seed varieties (id 1-4)
DELETE FROM `crop`    WHERE `id`      > 4;       -- keep seed crops (id 1-4)

ALTER TABLE `plot`          AUTO_INCREMENT = 1;
ALTER TABLE `planting_plan` AUTO_INCREMENT = 1;
ALTER TABLE `crop`          AUTO_INCREMENT = 5;  -- next new crop will be id=5
ALTER TABLE `variety`       AUTO_INCREMENT = 5;  -- next new variety will be id=5

-- ============================================================================
-- 0) Convert existing seed crops/varieties (Chinese) to English
-- ============================================================================
UPDATE `crop` SET name='Tomato',     category='Vegetable',         remark=NULL WHERE code='CR-001';
UPDATE `crop` SET name='Cucumber',   category='Vegetable',         remark=NULL WHERE code='CR-002';
UPDATE `crop` SET name='Lettuce',    category='Leafy Vegetable',   remark=NULL WHERE code='CR-003';
UPDATE `crop` SET name='Strawberry', category='Fruit',             remark=NULL WHERE code='CR-004';

UPDATE `variety` SET name='Cherry Tomato',     traits='Small, high sweetness'    WHERE crop_id=1 AND code='V-001';
UPDATE `variety` SET name='Provence Heritage', traits='Large, juicy'             WHERE crop_id=1 AND code='V-002';
UPDATE `variety` SET name='Mini Snack',        traits='Short, crisp'             WHERE crop_id=2 AND code='V-001';
UPDATE `variety` SET name='Butterhead',        traits='Soft, buttery texture'    WHERE crop_id=3 AND code='V-001';

UPDATE `packaging_spec` SET name='250g Clear Punnet',   material='PET'           WHERE code='SP-250G';
UPDATE `packaging_spec` SET name='500g Resealable Bag', material='PE'            WHERE code='SP-500G';
UPDATE `packaging_spec` SET name='1kg Gift Box',        material='Paper + Liner' WHERE code='SP-1KG';
UPDATE `packaging_spec` SET name='5kg Crate',           material='PP Crate'      WHERE code='SP-5KG';

-- ============================================================================
-- 1) Plots (5 real Kenyan locations + diverse soil/irrigation)
-- ============================================================================
INSERT INTO `plot` (`code`,`name`,`area_mu`,`location`,`soil_type`,`irrigation`,`owner_id`,`status`,`remark`) VALUES
('P-001', 'Nairobi Block A',     5.50,  'Nairobi · 1.2864S, 36.8172E',  'loam', 'drip',   1, 'active', 'Pilot greenhouse for high-value short-cycle crops (lettuce, tomato)'),
('P-002', 'Nakuru Plot 1',      12.00,  'Nakuru · 0.3031S, 36.0800E',   'sand', 'drip',   1, 'active', 'Staple grain area, maize/soybean rotation'),
('P-003', 'Eldoret Field',       8.00,  'Eldoret · 0.5143N, 35.2698E',  'clay', 'furrow', 1, 'active', 'Highland climate, suitable for soybean / tea'),
('P-004', 'Mombasa Greenhouse',  3.00,  'Mombasa · 4.0435S, 39.6682E',  'sand', 'drip',   1, 'active', 'Coastal greenhouse, off-season strawberry / vegetables'),
('P-005', 'Kisumu Lakeside',    15.00,  'Kisumu · 0.0917S, 34.7680E',   'loam', 'drip',   1, 'active', 'Lake Victoria shore, long-cycle orchard (avocado / pineapple)');

-- ============================================================================
-- 2) New crops (African staples + cash crops + export fruits)
--    Existing seed IDs: 1=Tomato, 2=Cucumber, 3=Lettuce, 4=Strawberry
--    These will be:     5-10
-- ============================================================================
INSERT INTO `crop` (`code`,`name`,`category`,`unit`,`cycle_days`,`remark`) VALUES
('CR-005', 'Maize',     'Grain',          'kg',  120, 'East Africa''s no.1 staple grain'),
('CR-006', 'Avocado',   'Fruit Tree',     'kg',  365, 'Kenya''s top export, stable pricing'),
('CR-007', 'Tea',       'Cash Crop',      'kg',  730, 'Kenya''s flagship export, highland tea'),
('CR-008', 'Coffee',    'Cash Crop',      'kg', 1095, 'High-altitude AA-grade specialty coffee'),
('CR-009', 'Soybean',   'Oil Crop',       'kg',  130, 'Protein supplement / rotation partner'),
('CR-010', 'Pineapple', 'Tropical Fruit', 'kg',  540, 'Cannery & fresh dual-use');

-- ============================================================================
-- 3) New varieties (referencing new crop IDs 5-10)
-- ============================================================================
INSERT INTO `variety` (`crop_id`,`code`,`name`,`traits`) VALUES
-- Maize (crop_id=5)
(5,  'V-001', 'H614',         'KARI hybrid, high yield, drought tolerant'),
(5,  'V-002', 'WH505',        'White maize, long kernel, staple market favorite'),
-- Avocado (crop_id=6)
(6,  'V-001', 'Hass',         'Dark thick skin, export grade'),
(6,  'V-002', 'Fuerte',       'Green thin skin, local market'),
-- Tea (crop_id=7)
(7,  'V-001', 'TRFK 31/8',    'TRFK improved, high-aroma broadleaf'),
-- Coffee (crop_id=8)
(8,  'V-001', 'SL28',         'Classic Kenya specialty, Blue Mountain-like profile'),
(8,  'V-002', 'Ruiru 11',     'CBD-resistant improved variety'),
-- Soybean (crop_id=9)
(9,  'V-001', 'SB19',         'High protein, early maturing'),
-- Pineapple (crop_id=10)
(10, 'V-001', 'MD2',          'High sweetness, export mainstream');

-- ============================================================================
-- 4) Planting plans (5 plans across 4 status states + realistic scenarios)
-- ============================================================================
INSERT INTO `planting_plan` (`code`,`plot_id`,`crop_id`,`variety_id`,`area_mu`,`plan_start_date`,`plan_harvest_date`,`target_yield_kg`,`status`,`remark`) VALUES
-- Pilot greenhouse short crops - draft
('PL-26-0001', 1,  1, 1,     2.00, '2026-06-01', '2026-09-01',   5000.00, 'draft',       'Nairobi Block A tomato / cherry, post-rainy season start'),
-- Maize main season - planned
('PL-26-0002', 2,  5, 5,    12.00, '2026-04-01', '2026-09-15',  50000.00, 'planned',     'Nakuru main maize season, H614 hybrid (staple)'),
-- Soybean rotation - in progress
('PL-26-0003', 3,  9, 12,    8.00, '2026-05-01', '2026-10-01',  30000.00, 'in_progress', 'Eldoret soybean, rotation after last year''s maize (nitrogen recovery)'),
-- Off-season coastal strawberry - harvested
('PL-26-0004', 4,  4, NULL,  3.00, '2026-03-01', '2026-07-01',   8000.00, 'harvested',   'Mombasa off-season strawberry, harvest completed July, yield above target'),
-- Long-cycle orchard - in progress
('PL-26-0005', 5,  6, 7,    15.00, '2026-01-01', '2027-06-01', 100000.00, 'in_progress', 'Kisumu avocado Hass, long-cycle orchard, first-year fruit set in progress');

-- ============================================================================
-- Verification
-- ============================================================================
SELECT '=== plots ===' AS '';
SELECT id, code, name, area_mu, soil_type, irrigation FROM plot ORDER BY