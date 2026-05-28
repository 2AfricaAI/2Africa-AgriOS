-- Sprint 26 / FEFO — shelf life + inventory expiry
-- 1) Crop default shelf life (per-crop, e.g. leafy 5d, fruit 14d)
-- 2) Variety override (nullable; falls back to crop default at packing time)
-- 3) inventory.expiry_date — populated by PackingService at insert time
--                             (pack_date + resolved_shelf_life)
-- 4) Backfill existing inventory.expiry_date using prod_date + 7d as a safe default.
--    Real values are written as new packings happen.

USE agrios;

-- ------------------------------------------------------------
-- crop: per-crop default shelf life
-- ------------------------------------------------------------
ALTER TABLE `crop`
    ADD COLUMN `shelf_life_days` INT NULL DEFAULT NULL
        COMMENT 'Default shelf life (days) after packing for this crop' AFTER `cycle_days`;

-- Sensible defaults for seeded crops (fresh produce / Kenya context).
-- These are conservative — variety-level overrides are encouraged.
UPDATE `crop` SET `shelf_life_days` = 5   WHERE `code` IN ('SPINACH', 'KALE', 'LETTUCE', 'CABBAGE');
UPDATE `crop` SET `shelf_life_days` = 7   WHERE `code` IN ('TOMATO', 'CUCUMBER', 'BEAN');
UPDATE `crop` SET `shelf_life_days` = 14  WHERE `code` IN ('AVOCADO', 'MANGO', 'ONION', 'PEPPER', 'CHILI');
UPDATE `crop` SET `shelf_life_days` = 30  WHERE `code` IN ('POTATO', 'CARROT', 'SWEETPOTATO');
-- Anything not matched stays NULL → PackingService treats NULL as 'no expiry tracking'.

-- ------------------------------------------------------------
-- variety: nullable override
-- ------------------------------------------------------------
ALTER TABLE `variety`
    ADD COLUMN `shelf_life_days` INT NULL DEFAULT NULL
        COMMENT 'Override of crop.shelf_life_days; NULL = use crop default' AFTER `traits`;

-- ------------------------------------------------------------
-- inventory: expiry_date
-- ------------------------------------------------------------
ALTER TABLE `inventory`
    ADD COLUMN `expiry_date` DATE NULL DEFAULT NULL
        COMMENT 'Best-before date = pack_date + shelf_life. NULL = no expiry tracking' AFTER `prod_date`,
    ADD KEY `idx_expiry` (`expiry_date`);

-- Backfill: use prod_date + crop/variety shelf life when resolvable;
-- otherwise prod_date + 7 days as a safety default for legacy rows.
UPDATE `inventory` inv
JOIN `sku` sk ON inv.sku_id = sk.id
LEFT JOIN `crop`    c ON sk.crop_id    = c.id
LEFT JOIN `variety` v ON sk.variety_id = v.id
SET inv.expiry_date = DATE_ADD(
    inv.prod_date,
    INTERVAL COALESCE(v.shelf_life_days, c.shelf_life_days, 7) DAY
)
WHERE inv.expiry_date IS NULL
  AND inv.prod_date IS NOT NULL;
