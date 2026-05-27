-- Sprint 20.5 - Add GPS field to harvest_record (mobile capture)
ALTER TABLE `harvest_record`
  ADD COLUMN `location_gps` VARCHAR(64) NULL COMMENT 'GPS lat,lng captured at harvest time' AFTER `qty_kg`;
