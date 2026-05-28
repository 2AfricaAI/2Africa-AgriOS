-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: toafrica_agrios
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `action_item`
--

DROP TABLE IF EXISTS `action_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `action_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `severity` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'medium',
  `category` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `owner_role` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ref_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ref_id` bigint DEFAULT NULL,
  `ref_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'open',
  `due_date` date DEFAULT NULL,
  `data_snapshot` json DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `resolved_at` datetime DEFAULT NULL,
  `resolved_by` bigint DEFAULT NULL,
  `resolved_remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_ref` (`rule_code`,`ref_type`,`ref_id`),
  KEY `idx_status_category` (`status`,`category`),
  KEY `idx_owner_role` (`owner_role`),
  KEY `idx_severity` (`severity`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `action_item`
--

LOCK TABLES `action_item` WRITE;
/*!40000 ALTER TABLE `action_item` DISABLE KEYS */;
INSERT INTO `action_item` VALUES (1,'R-PROD-02','medium','week_risk','Overstock ΓÇö Strawberry ┬╖ A ┬╖ 250g Clear Punnet (270.0 days of supply on hand)','Consider delaying next harvest or running a promo.','production','sku',2,'SKU-CR-004-NA-A-SP-250G','open','2026-06-03','{\"sku_id\": 2, \"sold_30d\": \"10.000\", \"avg_daily\": \"0.3333\", \"days_supply\": \"270.0\", \"qty_avail_total\": \"90.000\"}','2026-05-26 00:54:43','2026-05-26 00:54:43',NULL,NULL,NULL),(2,'R-CUST-01','low','followup','Silent customer ΓÇö Quiet Restaurant Ltd','No order ever placed (90 days since onboarding). Suggest follow-up call.','sales','customer',2,'CUS-99999','auto_resolved','2026-05-29','{\"customer_id\": 2, \"days_silent\": 90, \"last_order_date\": null}','2026-05-26 00:55:59','2026-05-26 00:55:59','2026-05-26 03:10:58',NULL,'Auto-resolved: rule no longer triggers'),(3,'R-INV-01','high','today','Aging inventory ΓÇö Soybean SB19 ┬╖ A ┬╖ 5kg Crate (90 units, 61 days old)','From batch B-20260529-P-003-01, produced 2026-03-27. Suggest urgent sale.','sales','inventory',1,'B-20260529-P-003-01','open','2026-05-27','{\"sku_id\": 1, \"days_old\": 61, \"prod_date\": \"2026-03-27\", \"qty_avail\": \"90\"}','2026-05-26 00:55:59','2026-05-26 00:55:59',NULL,NULL,NULL),(4,'R-PROD-02','medium','week_risk','Overstock ΓÇö Soybean SB19 ┬╖ A ┬╖ 5kg Crate (270.0 days of supply on hand)','Consider delaying next harvest or running a promo.','production','sku',1,'SKU-CR-009-V-001-A-SP-5KG','open','2026-06-03','{\"sku_id\": 1, \"sold_30d\": \"10.000\", \"avg_daily\": \"0.3333\", \"days_supply\": \"270.0\", \"qty_avail_total\": \"90.000\"}','2026-05-26 03:10:58','2026-05-26 03:10:58',NULL,NULL,NULL),(5,'R-PROD-02','medium','week_risk','Overstock ΓÇö Tomato Cherry Tomato ┬╖ A ┬╖ 1kg Gift Box (120.0 days of supply on hand)','Consider delaying next harvest or running a promo.','production','sku',3,'SKU-CR-001-V-001-A-SP-1KG','open','2026-06-03','{\"sku_id\": 3, \"sold_30d\": \"20.000\", \"avg_daily\": \"0.6667\", \"days_supply\": \"120.0\", \"qty_avail_total\": \"80.000\"}','2026-05-26 03:10:58','2026-05-26 03:10:58',NULL,NULL,NULL),(6,'R-AR-01','high','today','AR overdue ΓÇö Joy Food ┬╖ SO-20260525-001 (31 days)','Order SO-20260525-001 is 31 days overdue. Outstanding KES 10000.00 (paid 0.00 of 10000.00). Due 2026-04-26.','finance','order',1,'SO-20260525-001','open','2026-05-27','{\"paid\": 0.0, \"total\": 10000.0, \"currency\": \"KES\", \"due_date\": \"2026-04-26\", \"order_id\": 1, \"customer_id\": 1, \"outstanding\": 10000.0, \"days_overdue\": 31, \"customer_name\": \"Joy Food\"}','2026-05-26 04:23:16','2026-05-26 04:23:16',NULL,NULL,NULL),(7,'R-AP-01','high','today','AP overdue ΓÇö Athi River Fertilizers ┬╖ PO-20260526-0003 (35 days)','PO PO-20260526-0003 to supplier Athi River Fertilizers is 35 days overdue. Outstanding KES 30000.00 (paid 42500.00 of 72500.00). Due 2026-04-22. Pay soon to avoid supply disruption.','finance','purchase_order',3,'PO-20260526-0003','auto_resolved','2026-05-27','{\"paid\": 42500.0, \"po_id\": 3, \"total\": 72500.0, \"currency\": \"KES\", \"due_date\": \"2026-04-22\", \"outstanding\": 30000.0, \"supplier_id\": 2, \"days_overdue\": 35, \"supplier_name\": \"Athi River Fertilizers\"}','2026-05-27 05:21:20','2026-05-27 05:21:20','2026-05-27 05:32:23',NULL,'Auto-resolved: rule no longer triggers'),(8,'R-CASH-01','medium','week_risk','Cash gap ΓÇö minimum reserve needed: 450000.00 KES (Week 2)','Without additional inflows, cumulative net cash will reach -450000.00 KES by week 2 (2026-06-07). 13-week total: inflow 50000.00, outflow 500000.00, net -450000.00. Ensure cash reserves of at least 450000.00 KES, or accelerate AR collection / delay AP.','ceo','cash_flow',0,'CASH-13W','open','2026-05-27','{\"opening\": 0, \"min_date\": \"2026-06-07\", \"min_week\": 1, \"net_flow\": -450000.0, \"min_balance\": -450000.0, \"total_inflow\": 50000.0, \"total_outflow\": 500000.0}','2026-05-27 06:30:12','2026-05-27 06:30:12',NULL,NULL,NULL),(9,'R-AP-02','low','week_risk','Large AP ΓÇö Athi River Fertilizers ┬╖ 427500.00 KES payable','Supplier Athi River Fertilizers has 427500.00 KES outstanding payable (15-30d: 0.00, 30+d: 0.00). Ensure cash reserves or contact supplier ΓÇö late payment risks supply disruption.','finance','supplier',2,'SUP-00002','open','2026-05-30','{\"aging_1530\": 0.0, \"outstanding\": 427500.0, \"supplier_id\": 2, \"aging_30plus\": 0.0}','2026-05-27 06:54:29','2026-05-27 06:54:29',NULL,NULL,NULL);
/*!40000 ALTER TABLE `action_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `activity`
--

DROP TABLE IF EXISTS `activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_uuid` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'σ╣éτ¡ëΘö«',
  `plot_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `activity_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'sow/fertilize/spray/weed/water/prune/other',
  `occur_date` date NOT NULL,
  `operator_id` bigint NOT NULL COMMENT 'staff_id',
  `photos` json DEFAULT NULL COMMENT 'OSS URL µò░τ╗ä',
  `location_gps` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `labor_cost` decimal(12,2) NOT NULL DEFAULT '0.00',
  `labor_po_item_id` bigint DEFAULT NULL COMMENT 'σà│ΦüöΣ║║σ╖Ñ PO Φíî',
  `water_cost` decimal(12,2) NOT NULL DEFAULT '0.00',
  `water_po_item_id` bigint DEFAULT NULL COMMENT 'σà│Φüöµ░┤Φ┤╣ PO Φíî',
  `electricity_cost` decimal(12,2) NOT NULL DEFAULT '0.00',
  `electricity_po_item_id` bigint DEFAULT NULL COMMENT 'σà│Φüöτö╡Φ┤╣ PO Φíî',
  `fertilizer_cost` decimal(12,2) NOT NULL DEFAULT '0.00',
  `fertilizer_po_item_id` bigint DEFAULT NULL COMMENT 'σà│ΦüöΦéÑµûÖ PO Φíî',
  `other_cost` decimal(12,2) NOT NULL DEFAULT '0.00',
  `other_po_item_id` bigint DEFAULT NULL COMMENT 'σà│Φüöσà╢Σ╗û PO Φíî',
  `cost_currency` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'KES',
  `audit_status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  `auditor_id` bigint DEFAULT NULL,
  `audited_at` datetime DEFAULT NULL,
  `audit_remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `client_uuid` (`client_uuid`),
  KEY `idx_plot_date` (`plot_id`,`occur_date`),
  KEY `idx_plan` (`plan_id`),
  KEY `idx_operator` (`operator_id`),
  KEY `idx_audit_status` (`audit_status`),
  KEY `idx_activity_fertilizer_po` (`fertilizer_po_item_id`),
  KEY `idx_activity_labor_po` (`labor_po_item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σå£Σ║ïΦ«░σ╜ò';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `activity`
--

LOCK TABLES `activity` WRITE;
/*!40000 ALTER TABLE `activity` DISABLE KEYS */;
INSERT INTO `activity` VALUES (1,'c6695c89-15e0-4295-ba55-a20d5070f12e',2,2,'sow','2026-05-25',1,'[3]','','',0.00,NULL,0.00,NULL,0.00,NULL,0.00,NULL,0.00,NULL,'KES','approved',1,'2026-05-25 18:17:10','','2026-05-25 18:17:06',NULL,'2026-05-25 18:17:06'),(2,'32bf726d-8bad-4a5d-953f-f7540ba077c3',1,1,'water','2026-05-26',1,'[]','','',50.00,NULL,50.00,NULL,0.00,NULL,0.00,NULL,50.00,NULL,'KES','approved',1,'2026-05-26 01:52:43','','2026-05-26 01:12:54',NULL,'2026-05-26 01:12:54'),(3,'97510f91-39b4-47e8-bba9-44a1a400f0ab',4,4,'fertilize','2026-05-01',1,'[]','','',100.00,NULL,20.00,NULL,0.00,NULL,500.00,NULL,20.00,NULL,'KES','approved',1,'2026-05-26 01:53:40','','2026-05-26 01:53:36',NULL,'2026-05-26 01:53:36'),(4,'9eecaec9-b88d-4d94-99ab-5c4c199a0f83',1,1,'fertilize','2026-05-26',1,'[]','','',500.00,NULL,100.00,NULL,100.00,NULL,50000.00,1,0.00,NULL,'KES','pending',NULL,NULL,NULL,'2026-05-27 04:50:51',NULL,'2026-05-27 04:50:51'),(5,'9304d14f-6ea0-4c96-82c2-5b08a230cea0',3,3,'fertilize','2026-05-27',1,'[4]','-1.273487,36.799179','',0.00,NULL,0.00,NULL,0.00,NULL,0.00,NULL,0.00,NULL,'KES','pending',NULL,NULL,NULL,'2026-05-27 08:10:21',NULL,'2026-05-27 08:10:21'),(6,'bb98739a-13ce-4a12-bf3a-4b898c98de49',3,3,'fertilize','2026-05-27',1,'[5]','-1.273536,36.799160','',0.00,NULL,0.00,NULL,0.00,NULL,0.00,NULL,0.00,NULL,'KES','pending',NULL,NULL,NULL,'2026-05-27 08:21:33',NULL,'2026-05-27 08:21:33'),(7,'bb24bcc5-1b02-408f-84fa-762686ba130c',2,2,'spray','2026-05-28',1,'[]','','',100.00,NULL,10.00,NULL,0.00,NULL,0.00,NULL,50.00,NULL,'KES','approved',1,'2026-05-28 14:41:09','','2026-05-28 14:39:41',NULL,'2026-05-28 14:39:41'),(8,'4ae2831c-eb30-48f3-94de-0ade9116e8ee',2,2,'spray','2026-05-28',1,'[]','','',100.00,NULL,0.00,NULL,0.00,NULL,0.00,NULL,0.00,NULL,'KES','approved',1,'2026-05-28 15:11:43','','2026-05-28 15:11:39',NULL,'2026-05-28 15:11:39');
/*!40000 ALTER TABLE `activity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `activity_input`
--

DROP TABLE IF EXISTS `activity_input`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `activity_input` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `input_id` bigint NOT NULL,
  `qty` decimal(12,3) NOT NULL,
  `unit` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
  `cost` decimal(12,2) DEFAULT NULL,
  `currency` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'KES',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_activity` (`activity_id`),
  KEY `idx_input` (`input_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σå£Σ║ï-µèòσàÑσôüµÿÄτ╗å';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `activity_input`
--

LOCK TABLES `activity_input` WRITE;
/*!40000 ALTER TABLE `activity_input` DISABLE KEYS */;
INSERT INTO `activity_input` VALUES (1,8,18,1.000,'L',200.00,'KES','2026-05-28 15:11:39');
/*!40000 ALTER TABLE `activity_input` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `batch`
--

DROP TABLE IF EXISTS `batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'B-20260520-P001-01',
  `parent_batch_id` bigint DEFAULT NULL COMMENT 'µïåσêåσ£║µÖ»',
  `plot_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `crop_id` bigint NOT NULL,
  `variety_id` bigint DEFAULT NULL,
  `harvest_record_id` bigint NOT NULL,
  `harvest_date` date NOT NULL,
  `qty_kg` decimal(12,3) NOT NULL,
  `qty_remain_kg` decimal(12,3) NOT NULL COMMENT 'σë⌐Σ╜Öµ£¬σñäτÉåΘçÅ',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/packed/sold_out/lost',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_plot_date` (`plot_id`,`harvest_date`),
  KEY `idx_status` (`status`),
  KEY `idx_code` (`code`),
  KEY `idx_parent` (`parent_batch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='µë╣µ¼í - σà¿Θô╛Φ╖»Σ╕╗Θö«';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `batch`
--

LOCK TABLES `batch` WRITE;
/*!40000 ALTER TABLE `batch` DISABLE KEYS */;
INSERT INTO `batch` VALUES (1,'B-20260527-P-004-01',NULL,4,4,4,NULL,1,'2026-05-27',100.000,74.000,'processing',NULL,'2026-05-25 18:34:55',NULL,'2026-05-25 18:34:55',NULL),(2,'B-20260529-P-003-01',NULL,3,3,9,12,2,'2026-05-29',2000.000,1500.000,'packed',NULL,'2026-05-25 18:36:43',NULL,'2026-05-25 18:36:43',NULL),(3,'B-20260525-P-001-01',NULL,1,1,1,1,3,'2026-05-25',100.000,0.000,'packed',NULL,'2026-05-26 01:54:49',NULL,'2026-05-26 01:54:49',NULL),(4,'B-20260529-P-002-01',NULL,2,2,5,5,4,'2026-05-29',100.000,100.000,'pending',NULL,'2026-05-28 14:53:27',NULL,'2026-05-28 14:53:27',NULL),(5,'B-20260528-P-002-01',NULL,2,2,5,5,5,'2026-05-28',100.000,100.000,'pending',NULL,'2026-05-28 14:55:13',NULL,'2026-05-28 14:55:13',NULL),(6,'B-20260605-P-002-01',NULL,2,2,5,5,6,'2026-06-05',100.000,100.000,'pending',NULL,'2026-05-28 15:13:37',NULL,'2026-05-28 15:13:37',NULL);
/*!40000 ALTER TABLE `batch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `collection_log`
--

DROP TABLE IF EXISTS `collection_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `collection_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint NOT NULL COMMENT '?????? id',
  `order_id` bigint DEFAULT NULL COMMENT '?????? id (??????, ??????????????????)',
  `log_date` date NOT NULL COMMENT '???????????????',
  `channel` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'phone / whatsapp / sms / email / visit / other',
  `contact_person` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '?????????????????????',
  `outcome` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'promised / refused / no_answer / disputed / paid / other',
  `promised_date` date DEFAULT NULL COMMENT '????????????????????? (??????????????????)',
  `promised_amount` decimal(14,2) DEFAULT NULL COMMENT '??????????????????',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '???????????? / ??????',
  `next_action_date` date DEFAULT NULL COMMENT '???????????????',
  `operator_id` bigint NOT NULL COMMENT '????????? (user_id)',
  `operator_name` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '?????? - ???????????????, ????????????',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL COMMENT '??????',
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`,`log_date`),
  KEY `idx_order` (`order_id`),
  KEY `idx_next_action` (`next_action_date`),
  KEY `idx_promised_date` (`promised_date`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='?????????????????? - Sprint 16';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `collection_log`
--

LOCK TABLES `collection_log` WRITE;
/*!40000 ALTER TABLE `collection_log` DISABLE KEYS */;
INSERT INTO `collection_log` VALUES (1,1,NULL,'2026-05-25','phone','Joy','promised','2026-06-01',50000.00,'σ«óµê╖µë┐Φ»║µ£êσ║òσëìΣ╗ÿµ╕à','2026-05-30',1,'admin','2026-05-26 04:10:48','2026-05-26 04:10:48',NULL),(2,1,NULL,'2026-05-26','phone','Mr. Deng','promised','2026-05-26',12000.00,'','2026-05-28',1,'admin','2026-05-26 13:51:49','2026-05-26 13:51:49',NULL);
/*!40000 ALTER TABLE `collection_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `complaint`
--

DROP TABLE IF EXISTS `complaint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `complaint` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) NOT NULL COMMENT 'COMPL-YYYYMMDD-NNNN',
  `reported_at` datetime NOT NULL,
  `customer_id` bigint DEFAULT NULL COMMENT 'NULL = internal QC complaint',
  `order_id` bigint DEFAULT NULL,
  `batch_id` bigint DEFAULT NULL,
  `sku_id` bigint DEFAULT NULL,
  `category` varchar(32) NOT NULL COMMENT 'quality / quantity / late / safety / wrong_product / other',
  `severity` varchar(16) NOT NULL DEFAULT 'medium' COMMENT 'low / medium / high / critical',
  `channel` varchar(16) NOT NULL DEFAULT 'phone' COMMENT 'phone / email / app / onsite / other',
  `description` text NOT NULL,
  `photo_ids` json DEFAULT NULL COMMENT 'File IDs for evidence photos',
  `status` varchar(24) NOT NULL DEFAULT 'open' COMMENT 'open / investigating / resolved / closed / escalated_to_recall',
  `resolution` text,
  `resolution_amount` decimal(12,2) DEFAULT NULL COMMENT 'Refund or credit amount, KES',
  `reported_by_id` bigint DEFAULT NULL,
  `resolved_at` datetime DEFAULT NULL,
  `resolved_by_id` bigint DEFAULT NULL,
  `recall_id` bigint DEFAULT NULL COMMENT 'Set when escalated to a recall',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_status` (`status`),
  KEY `idx_reported_at` (`reported_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Customer / QC complaints';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `complaint`
--

LOCK TABLES `complaint` WRITE;
/*!40000 ALTER TABLE `complaint` DISABLE KEYS */;
INSERT INTO `complaint` VALUES (1,'COMPL-20260528-0001','2026-05-28 13:29:21',1,NULL,NULL,NULL,'quality','medium','email','quality issues.',NULL,'resolved',NULL,NULL,1,'2026-05-28 21:30:50',1,NULL,'2026-05-28 21:30:22','2026-05-28 21:30:22');
/*!40000 ALTER TABLE `complaint` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `crop`
--

DROP TABLE IF EXISTS `crop`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `crop` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'σÅ╢ΦÅ£/µ₧£Φö¼/µá╣ΦîÄ...',
  `unit` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'kg',
  `cycle_days` int DEFAULT NULL COMMENT 'σ╕╕ΦºäτöƒΘò┐σæ¿µ£ƒ',
  `shelf_life_days` int DEFAULT NULL COMMENT 'Default shelf life (days) after packing for this crop',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Σ╜£τë⌐';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `crop`
--

LOCK TABLES `crop` WRITE;
/*!40000 ALTER TABLE `crop` DISABLE KEYS */;
INSERT INTO `crop` VALUES (1,'CR-001','Tomato','Vegetable','kg',90,NULL,NULL,1,'2026-05-25 16:05:09','2026-05-25 16:24:04'),(2,'CR-002','Cucumber','Vegetable','kg',60,NULL,NULL,1,'2026-05-25 16:05:09','2026-05-25 16:24:04'),(3,'CR-003','Lettuce','Leafy Vegetable','kg',45,NULL,NULL,1,'2026-05-25 16:05:09','2026-05-25 16:24:04'),(4,'CR-004','Strawberry','Fruit','kg',120,NULL,NULL,1,'2026-05-25 16:05:09','2026-05-25 16:24:04'),(5,'CR-005','Maize','Grain','kg',120,NULL,'East Africa\'s no.1 staple grain',1,'2026-05-27 08:23:29',NULL),(6,'CR-006','Avocado','Fruit Tree','kg',365,NULL,'Kenya\'s top export, stable pricing',1,'2026-05-27 08:23:29',NULL),(7,'CR-007','Tea','Cash Crop','kg',730,NULL,'Kenya\'s flagship export, highland tea',1,'2026-05-27 08:23:29',NULL),(8,'CR-008','Coffee','Cash Crop','kg',1095,NULL,'High-altitude AA-grade specialty coffee',1,'2026-05-27 08:23:29',NULL),(9,'CR-009','Soybean','Oil Crop','kg',130,NULL,'Protein supplement / rotation partner',1,'2026-05-27 08:23:29',NULL),(10,'CR-010','Pineapple','Tropical Fruit','kg',540,NULL,'Cannery & fresh dual-use',1,'2026-05-27 08:23:29',NULL);
/*!40000 ALTER TABLE `crop` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customer`
--

DROP TABLE IF EXISTS `customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'supermarket/restaurant/ecommerce/...',
  `contact_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `credit_level` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'A/B/C/D',
  `credit_days` int NOT NULL DEFAULT '0',
  `payment_terms` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_collection_date` date DEFAULT NULL COMMENT '?????????????????????',
  `next_action_date` date DEFAULT NULL COMMENT '???????????????',
  `since_date` date DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σ«óµê╖';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer`
--

LOCK TABLES `customer` WRITE;
/*!40000 ALTER TABLE `customer` DISABLE KEYS */;
INSERT INTO `customer` VALUES (1,'CUS-00001','Joy Food','wholesale','Mr. Deng','+254 794556665','A',7,'Weekly','2026-05-26','2026-05-28','2026-05-01','active','','2026-05-25 20:55:10',NULL,'2026-05-25 20:55:10',NULL),(2,'CUS-99999','Quiet Restaurant Ltd','restaurant','','','B',7,'Weekly',NULL,NULL,'2026-02-25','active','','2026-05-26 00:55:47',NULL,'2026-05-26 03:25:11',NULL);
/*!40000 ALTER TABLE `customer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customer_address`
--

DROP TABLE IF EXISTS `customer_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_address` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  `recipient` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `province` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `district` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address_line` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_default` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σ«óµê╖σ£░σ¥Çτ░┐';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer_address`
--

LOCK TABLES `customer_address` WRITE;
/*!40000 ALTER TABLE `customer_address` DISABLE KEYS */;
/*!40000 ALTER TABLE `customer_address` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `daily_report`
--

DROP TABLE IF EXISTS `daily_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_report` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `staff_id` bigint NOT NULL,
  `report_date` date NOT NULL,
  `done_summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `issues` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `next_plan` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_date` (`staff_id`,`report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='µùÑµèÑ';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `daily_report`
--

LOCK TABLES `daily_report` WRITE;
/*!40000 ALTER TABLE `daily_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `daily_report` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fulfillment`
--

DROP TABLE IF EXISTS `fulfillment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fulfillment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SH-20260520-001',
  `order_id` bigint NOT NULL,
  `picker_id` bigint DEFAULT NULL,
  `plan_ship_at` datetime DEFAULT NULL,
  `ship_at` datetime DEFAULT NULL,
  `delivered_at` datetime DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/picking/ready/shipped/delivered/cancelled',
  `ship_method` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'self/logistics',
  `track_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `driver_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `driver_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `vehicle_no` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_order` (`order_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σç║σ║ôσìò';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fulfillment`
--

LOCK TABLES `fulfillment` WRITE;
/*!40000 ALTER TABLE `fulfillment` DISABLE KEYS */;
INSERT INTO `fulfillment` VALUES (1,'SH-20260525-001',1,1,NULL,'2026-05-26 01:56:16',NULL,'shipped','self','','','','',NULL,'2026-05-25 23:24:16',NULL,'2026-05-25 23:24:16'),(2,'SH-20260525-002',2,1,NULL,'2026-05-26 00:01:34',NULL,'shipped','self','KE000244433434','James','+254 794556665','KCA 123A',NULL,'2026-05-25 23:28:15',NULL,'2026-05-25 23:28:15'),(3,'SH-20260526-001',3,1,NULL,'2026-05-26 01:56:59',NULL,'shipped','self','','','','',NULL,'2026-05-26 01:56:56',NULL,'2026-05-26 01:56:56');
/*!40000 ALTER TABLE `fulfillment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fulfillment_item`
--

DROP TABLE IF EXISTS `fulfillment_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fulfillment_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fulfillment_id` bigint NOT NULL,
  `order_item_id` bigint NOT NULL,
  `inventory_id` bigint NOT NULL,
  `batch_id` bigint NOT NULL COMMENT 'σåùΣ╜Ö∩╝îΦ┐╜µ║»σèáΘÇƒ',
  `sku_id` bigint NOT NULL,
  `qty` decimal(12,3) NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ful` (`fulfillment_id`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_inv` (`inventory_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σç║σ║ôµÿÄτ╗å - σÉ½Φ┐╜µ║»Θô╛Φ╖»';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fulfillment_item`
--

LOCK TABLES `fulfillment_item` WRITE;
/*!40000 ALTER TABLE `fulfillment_item` DISABLE KEYS */;
INSERT INTO `fulfillment_item` VALUES (1,1,1,1,2,1,10.000,'2026-05-25 23:24:16'),(2,2,2,2,1,2,10.000,'2026-05-25 23:28:15'),(3,3,4,3,3,3,20.000,'2026-05-26 01:56:56');
/*!40000 ALTER TABLE `fulfillment_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `grading`
--

DROP TABLE IF EXISTS `grading`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `grading` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `grade_date` date NOT NULL,
  `qty_a_kg` decimal(12,3) NOT NULL DEFAULT '0.000',
  `qty_b_kg` decimal(12,3) NOT NULL DEFAULT '0.000',
  `qty_c_kg` decimal(12,3) NOT NULL DEFAULT '0.000',
  `qty_loss_kg` decimal(12,3) NOT NULL DEFAULT '0.000',
  `operator_id` bigint NOT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_batch` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σêåτ║º';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `grading`
--

LOCK TABLES `grading` WRITE;
/*!40000 ALTER TABLE `grading` DISABLE KEYS */;
/*!40000 ALTER TABLE `grading` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `harvest_record`
--

DROP TABLE IF EXISTS `harvest_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `harvest_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HV-20260520-001',
  `client_uuid` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `plot_id` bigint NOT NULL,
  `plan_id` bigint NOT NULL,
  `crop_id` bigint NOT NULL,
  `variety_id` bigint DEFAULT NULL,
  `batch_id` bigint NOT NULL COMMENT 'Φç¬σè¿τöƒµêÉτÜäµë╣µ¼í',
  `harvest_date` date NOT NULL,
  `qty_kg` decimal(12,3) NOT NULL,
  `location_gps` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'GPS lat,lng captured at harvest time',
  `operator_id` bigint NOT NULL,
  `photos` json DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  UNIQUE KEY `client_uuid` (`client_uuid`),
  KEY `idx_plot_date` (`plot_id`,`harvest_date`),
  KEY `idx_plan` (`plan_id`),
  KEY `idx_batch` (`batch_id`),
  CONSTRAINT `harvest_record_chk_1` CHECK ((`qty_kg` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Θççµö╢Φ«░σ╜ò';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `harvest_record`
--

LOCK TABLES `harvest_record` WRITE;
/*!40000 ALTER TABLE `harvest_record` DISABLE KEYS */;
INSERT INTO `harvest_record` VALUES (1,'HV-20260527-001','2a55c097-9410-4b87-bc2c-703ba6043664',4,4,4,NULL,1,'2026-05-27',100.000,NULL,1,'[]','','2026-05-25 18:34:55',NULL,'2026-05-25 18:34:55'),(2,'HV-20260529-001','4000baaf-e6de-47c9-974d-306f199e409b',3,3,9,12,2,'2026-05-29',2000.000,NULL,1,'[]','','2026-05-25 18:36:43',NULL,'2026-05-25 18:36:43'),(3,'HV-20260525-001','fa9c8e01-ac6a-41d4-8ed8-c48ce39b607b',1,1,1,1,3,'2026-05-25',100.000,NULL,1,'[]','','2026-05-26 01:54:49',NULL,'2026-05-26 01:54:49'),(4,'HV-20260529-002','c5a27ae4-8c36-4d43-a383-4fc1ef71285c',2,2,5,5,4,'2026-05-29',100.000,NULL,1,'[]','','2026-05-28 14:53:27',NULL,'2026-05-28 14:53:27'),(5,'HV-20260528-001','034a0a0e-63f7-444d-a9d1-26b7460fae20',2,2,5,5,5,'2026-05-28',100.000,NULL,1,'[]','','2026-05-28 14:55:13',NULL,'2026-05-28 14:55:13'),(6,'HV-20260605-001','e0d4a184-061c-4f52-864d-7320d77dcf36',2,2,5,5,6,'2026-06-05',100.000,NULL,1,'[]','','2026-05-28 15:13:37',NULL,'2026-05-28 15:13:37');
/*!40000 ALTER TABLE `harvest_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `input`
--

DROP TABLE IF EXISTS `input`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `input` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'fertilizer/pesticide/seed/film/other',
  `unit` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
  `supplier` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `safety_days` int DEFAULT '0' COMMENT 'σ«ëσà¿Θù┤ΘÜöµ£ƒ∩╝êσñ⌐∩╝ë',
  `stock_qty` decimal(12,3) NOT NULL DEFAULT '0.000',
  `status` tinyint(1) NOT NULL DEFAULT '1',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='µèòσàÑσôü';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `input`
--

LOCK TABLES `input` WRITE;
/*!40000 ALTER TABLE `input` DISABLE KEYS */;
/*!40000 ALTER TABLE `input` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `input_item`
--

DROP TABLE IF EXISTS `input_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `input_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) NOT NULL COMMENT 'II-0001',
  `name` varchar(128) NOT NULL COMMENT '??? e.g. ?? 46%',
  `name_en` varchar(128) DEFAULT NULL COMMENT '??? e.g. Urea 46%',
  `input_type` varchar(32) NOT NULL COMMENT 'fertilizer/pesticide/seed/film/labor/other',
  `category_l2` varchar(64) DEFAULT NULL COMMENT 'L2 sub-category (fertilizer:nitrogen/phosphate/compound/organic; pesticide:herbicide/insecticide/fungicide; seed:vegetable/fruit/flower)',
  `spec` varchar(128) DEFAULT NULL COMMENT '??, e.g. 50kg/bag, 1L/bottle',
  `pack_qty` decimal(14,3) DEFAULT NULL COMMENT 'Quantity per pack (e.g., 50 for 50kg/bag)',
  `pack_unit_label` varchar(32) DEFAULT NULL COMMENT 'Pack label (bag/bottle/box/can/sack/...)',
  `unit` varchar(16) NOT NULL DEFAULT 'kg' COMMENT 'kg/L/pack/box/pcs',
  `active_ingredient` varchar(128) DEFAULT NULL COMMENT '???? (??????), e.g. Chlorantraniliprole 200g/L',
  `registration_no` varchar(64) DEFAULT NULL COMMENT '?????? (Kenya PCPB ???)',
  `phi_days` int NOT NULL DEFAULT '0' COMMENT 'Pre-Harvest Interval - ???????, ? pesticide ???',
  `default_supplier_id` bigint DEFAULT NULL COMMENT '????? FK -> supplier.id (??)',
  `default_warehouse_id` bigint DEFAULT NULL COMMENT 'FK -> location_warehouse.id (default storage location)',
  `min_stock_qty` decimal(14,3) DEFAULT NULL COMMENT 'Reorder alert threshold in base unit (R-INV-04 will use this)',
  `status` varchar(16) NOT NULL DEFAULT 'active' COMMENT 'active/inactive',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_input_type` (`input_type`),
  KEY `idx_default_supplier` (`default_supplier_id`),
  KEY `idx_status` (`status`),
  KEY `idx_input_warehouse` (`default_warehouse_id`),
  KEY `idx_input_category_l2` (`category_l2`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='?????? (Phase 4)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `input_item`
--

LOCK TABLES `input_item` WRITE;
/*!40000 ALTER TABLE `input_item` DISABLE KEYS */;
INSERT INTO `input_item` VALUES (16,'II-0001','Urea 46-0-0','','fertilizer','','50kg/bag',NULL,'','kg','N 46%','',0,NULL,7,NULL,'active','Kenya - YARA mainstream nitrogen fertilizer','2026-05-27 15:11:32',NULL,'2026-05-28 02:23:44',NULL),(17,'II-0002','NPK 17-17-17',NULL,'fertilizer',NULL,'50kg/bag',NULL,NULL,'kg','N 17%, P2O5 17%, K2O 17%',NULL,0,NULL,NULL,NULL,'active','Base fertilizer for maize and coffee','2026-05-27 15:11:32',NULL,NULL,NULL),(18,'II-0004','Chlorantraniliprole 200g/L','','pesticide','','250ml/bottle',NULL,'','L','Chlorantraniliprole 200g/L','PCPB(CR)5678',7,NULL,NULL,NULL,'active','Insecticide for avocado and maize pests - PHI 7 days','2026-05-27 15:11:32',NULL,'2026-05-28 01:59:53',NULL),(19,'II-0005','Maize Seed H614','','seed','','5kg/bag',NULL,'','pcs','','',0,1,2,NULL,'active','KARI hybrid - mainstream maize variety','2026-05-27 15:11:32',NULL,'2026-05-28 04:50:01',NULL),(20,'II-0006','Black Mulch Film',NULL,'construction',NULL,'100m/roll',NULL,NULL,'pcs',NULL,NULL,0,NULL,NULL,NULL,'active','Weed suppression and water retention','2026-05-27 15:11:32',NULL,'2026-05-27 18:45:24',NULL);
/*!40000 ALTER TABLE `input_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `input_stock`
--

DROP TABLE IF EXISTS `input_stock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `input_stock` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `input_item_id` bigint NOT NULL COMMENT 'FK -> input_item.id',
  `warehouse_id` bigint NOT NULL COMMENT 'FK -> location_warehouse.id (leaf stockable node)',
  `qty_on_hand` decimal(14,3) NOT NULL DEFAULT '0.000' COMMENT 'Actual quantity in stock (base unit)',
  `qty_reserved` decimal(14,3) NOT NULL DEFAULT '0.000' COMMENT 'Reserved quantity (pending outbound, not yet deducted)',
  `last_stock_at` datetime DEFAULT NULL COMMENT 'Timestamp of last stock movement (in or out)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_warehouse` (`input_item_id`,`warehouse_id`),
  KEY `idx_warehouse` (`warehouse_id`),
  KEY `idx_last_stock` (`last_stock_at` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Input item stock level per warehouse ├óΓé¼ΓÇ¥ Sprint 22.2';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `input_stock`
--

LOCK TABLES `input_stock` WRITE;
/*!40000 ALTER TABLE `input_stock` DISABLE KEYS */;
INSERT INTO `input_stock` VALUES (1,16,7,100.000,0.000,'2026-05-28 02:25:03','2026-05-28 02:25:03','2026-05-28 02:25:03'),(2,17,12,-20.000,0.000,'2026-05-28 03:23:58','2026-05-28 03:23:58','2026-05-28 03:23:58'),(3,19,2,10.000,0.000,'2026-05-28 05:27:59','2026-05-28 05:27:59','2026-05-28 05:27:59');
/*!40000 ALTER TABLE `input_stock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `input_stock_log`
--

DROP TABLE IF EXISTS `input_stock_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `input_stock_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `input_item_id` bigint NOT NULL COMMENT 'FK -> input_item.id',
  `warehouse_id` bigint NOT NULL COMMENT 'FK -> location_warehouse.id',
  `direction` enum('IN','OUT') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'IN = inbound, OUT = outbound',
  `qty` decimal(14,3) NOT NULL COMMENT 'Always positive; direction determines sign on stock',
  `reason_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'po_receive / activity_consume / stocktake_adjust / damage / return_in / transfer_in / transfer_out / manual',
  `reference_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Polymorphic: purchase_order / activity / stocktake / damage_report / transfer',
  `reference_id` bigint DEFAULT NULL COMMENT 'ID of the referenced entity',
  `qty_after` decimal(14,3) NOT NULL COMMENT 'Stock qty_on_hand AFTER this movement (snapshot for audit trail)',
  `operator_id` bigint DEFAULT NULL COMMENT 'FK -> sys_user.id (who performed the action)',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_log_item` (`input_item_id`,`created_at` DESC),
  KEY `idx_log_warehouse` (`warehouse_id`,`created_at` DESC),
  KEY `idx_log_reason` (`reason_type`),
  KEY `idx_log_ref` (`reference_type`,`reference_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Input stock movement log ├óΓé¼ΓÇ¥ Sprint 22.3 (audit, immutable)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `input_stock_log`
--

LOCK TABLES `input_stock_log` WRITE;
/*!40000 ALTER TABLE `input_stock_log` DISABLE KEYS */;
INSERT INTO `input_stock_log` VALUES (1,16,7,'IN',100.000,'po_receive','purchase_order',6,100.000,NULL,'PO PO-20260527-0003 received','2026-05-28 02:25:03'),(2,17,12,'OUT',20.000,'activity_consume','warehouse_outbound',1,-20.000,NULL,'Outbound OUT-20260528-0001','2026-05-28 03:23:58'),(3,19,2,'IN',10.000,'po_receive','warehouse_inbound',1,10.000,NULL,'Inbound IN-20260528-0001','2026-05-28 05:27:59');
/*!40000 ALTER TABLE `input_stock_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory`
--

DROP TABLE IF EXISTS `inventory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sku_id` bigint NOT NULL,
  `batch_id` bigint NOT NULL,
  `grade` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
  `location_id` bigint NOT NULL,
  `qty_avail` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT 'σÅ»σö«',
  `qty_locked` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT 'Θöüσ«Ü',
  `qty_in_transit` decimal(12,3) NOT NULL DEFAULT '0.000' COMMENT 'σ£¿ΘÇö',
  `unit` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pack',
  `prod_date` date NOT NULL COMMENT 'τöƒΣ║ºµùÑµ£ƒ∩╝êτ╗ºµë┐µë╣µ¼í∩╝ë',
  `expiry_date` date DEFAULT NULL COMMENT 'Best-before date = pack_date + shelf_life. NULL = no expiry tracking',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'normal' COMMENT 'normal/frozen/lost',
  `version` int NOT NULL DEFAULT '0' COMMENT 'Σ╣ÉΦºéΘöüτëêµ£¼',
  `last_op_at` datetime DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_batch_loc` (`sku_id`,`batch_id`,`grade`,`location_id`),
  KEY `idx_sku_avail` (`sku_id`,`qty_avail`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_prod_date` (`prod_date`),
  KEY `idx_expiry` (`expiry_date`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σ║ôσ¡ÿ - τ╗ÅΦÉÑΦèéτé╣';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory`
--

LOCK TABLES `inventory` WRITE;
/*!40000 ALTER TABLE `inventory` DISABLE KEYS */;
INSERT INTO `inventory` VALUES (1,1,2,'A',1,90.000,0.000,0.000,'pack','2026-03-27','2026-04-03','normal',2,'2026-05-26 01:56:16','2026-05-25 19:20:01','2026-05-28 18:09:58'),(2,2,1,'A',4,90.000,0.000,0.000,'pack','2026-05-27','2026-06-03','normal',2,'2026-05-26 00:01:34','2026-05-25 23:22:18','2026-05-28 18:09:58'),(3,3,3,'A',1,80.000,0.000,0.000,'pack','2026-05-25','2026-06-01','normal',2,'2026-05-26 01:56:59','2026-05-26 01:55:29','2026-05-28 18:09:58'),(4,4,1,'A',1,1.000,0.000,0.000,'pack','2026-05-27','2026-06-03','normal',0,'2026-05-28 04:43:16','2026-05-28 04:43:15','2026-05-28 18:09:58');
/*!40000 ALTER TABLE `inventory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_adjust_log`
--

DROP TABLE IF EXISTS `inventory_adjust_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_adjust_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `inventory_id` bigint NOT NULL,
  `adjust_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'in/out/lock/unlock/loss/audit',
  `reason_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'count_gain/count_loss/damage/manual',
  `qty_before` decimal(12,3) NOT NULL,
  `qty_change` decimal(12,3) NOT NULL,
  `qty_after` decimal(12,3) NOT NULL,
  `field_name` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'qty_avail/qty_locked/...',
  `ref_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'order/fulfillment/packing/manual',
  `ref_id` bigint DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `operator_id` bigint NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_inv_time` (`inventory_id`,`created_at`),
  KEY `idx_ref` (`ref_type`,`ref_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σ║ôσ¡ÿΦ░âµò┤µùÑσ┐ù';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_adjust_log`
--

LOCK TABLES `inventory_adjust_log` WRITE;
/*!40000 ALTER TABLE `inventory_adjust_log` DISABLE KEYS */;
INSERT INTO `inventory_adjust_log` VALUES (1,1,'in','packing',0.000,100.000,100.000,'qty_avail','packing',1,'Packing PK-20260525-001',1,'2026-05-25 19:20:01'),(2,2,'in','packing',0.000,100.000,100.000,'qty_avail','packing',2,'Packing PK-20260525-002',1,'2026-05-25 23:22:18'),(3,1,'lock','picking',100.000,-10.000,90.000,'qty_avail','fulfillment',1,'Picked for order SO-20260525-001',1,'2026-05-25 23:24:16'),(4,2,'lock','picking',100.000,-10.000,90.000,'qty_avail','fulfillment',2,'Picked for order SO-20260525-002',1,'2026-05-25 23:28:14'),(5,2,'out','sale',10.000,-10.000,0.000,'qty_locked','fulfillment',2,'Shipped via fulfillment SH-20260525-002 (order SO-20260525-002)',1,'2026-05-26 00:01:33'),(6,3,'in','packing',0.000,100.000,100.000,'qty_avail','packing',3,'Packing PK-20260525-003',1,'2026-05-26 01:55:29'),(7,1,'out','sale',10.000,-10.000,0.000,'qty_locked','fulfillment',1,'Shipped via fulfillment SH-20260525-001 (order SO-20260525-001)',1,'2026-05-26 01:56:15'),(8,3,'lock','picking',100.000,-20.000,80.000,'qty_avail','fulfillment',3,'Picked for order SO-20260525-003',1,'2026-05-26 01:56:55'),(9,3,'out','sale',20.000,-20.000,0.000,'qty_locked','fulfillment',3,'Shipped via fulfillment SH-20260526-001 (order SO-20260525-003)',1,'2026-05-26 01:56:59'),(10,4,'in','packing',0.000,1.000,1.000,'qty_avail','packing',4,'Packing PK-20260527-001',1,'2026-05-28 04:43:15');
/*!40000 ALTER TABLE `inventory_adjust_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `location_warehouse`
--

DROP TABLE IF EXISTS `location_warehouse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `location_warehouse` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'σªé W01-A1',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'normal' COMMENT 'normal/cold/quarantine',
  `purpose` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'finished_goods' COMMENT 'finished_goods | seed_storage | fertilizer_storage | pesticide_storage | construction_storage | spare_parts_storage | tools_storage | packaging_storage | other_storage',
  `level` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'warehouse' COMMENT 'Hierarchy level: warehouse | zone | shelf | bin',
  `parent_id` bigint DEFAULT '0',
  `capacity_kg` decimal(12,2) DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_warehouse_purpose` (`purpose`),
  KEY `idx_warehouse_level` (`level`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Σ╗ôσ║ô/σ║ôΣ╜ì';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `location_warehouse`
--

LOCK TABLES `location_warehouse` WRITE;
/*!40000 ALTER TABLE `location_warehouse` DISABLE KEYS */;
INSERT INTO `location_warehouse` VALUES (1,'CLD-MAIN','Cold Storage - Main','normal','finished_goods','warehouse',0,NULL,1,'2026-05-25 16:05:09'),(2,'CLD-MAIN-A1','Main - Shelf A1','normal','finished_goods','shelf',1,NULL,1,'2026-05-25 16:05:09'),(3,'CLD-MAIN-A2','Main - Shelf A2','normal','finished_goods','shelf',1,NULL,1,'2026-05-25 16:05:09'),(4,'CLD-COLD','Cold Storage - Frozen','cold','finished_goods','warehouse',0,NULL,1,'2026-05-25 16:05:09'),(5,'CLD-COLD-C1','Cold - Shelf C1','cold','finished_goods','shelf',4,NULL,1,'2026-05-25 16:05:09'),(6,'AGW-SEE','Seed Storage','normal','seed_storage','warehouse',0,NULL,1,'2026-05-27 18:45:24'),(7,'AGW-FER','Fertilizer Storage','normal','fertilizer_storage','warehouse',0,NULL,1,'2026-05-27 18:45:24'),(8,'AGW-CHE','Chemical / Pesticide Locker','normal','pesticide_storage','warehouse',0,NULL,1,'2026-05-27 18:45:24'),(9,'AGW-CON','Construction Yard','normal','construction_storage','warehouse',0,NULL,1,'2026-05-27 18:45:24'),(10,'TLW-SPT','Spare Parts Cabinet','normal','spare_parts_storage','warehouse',0,NULL,1,'2026-05-27 18:45:24'),(11,'TLW-TOL','Tool Room','normal','tools_storage','warehouse',0,NULL,1,'2026-05-27 18:45:24'),(12,'AGW-PKG','Packaging Materials','normal','packaging_storage','warehouse',0,NULL,1,'2026-05-27 18:45:24');
/*!40000 ALTER TABLE `location_warehouse` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `loss_record`
--

DROP TABLE IF EXISTS `loss_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `loss_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL,
  `stage` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'grading/packing/inventory/other',
  `reason_code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `qty_kg` decimal(12,3) NOT NULL,
  `occur_at` datetime NOT NULL,
  `operator_id` bigint NOT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_occur` (`occur_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='µìƒΦÇùΦ«░σ╜ò';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `loss_record`
--

LOCK TABLES `loss_record` WRITE;
/*!40000 ALTER TABLE `loss_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `loss_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_inventory_lock`
--

DROP TABLE IF EXISTS `order_inventory_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_inventory_lock` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `order_item_id` bigint NOT NULL,
  `inventory_id` bigint NOT NULL,
  `qty_locked` decimal(12,3) NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'locked' COMMENT 'locked/released/shipped',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_inv` (`inventory_id`),
  KEY `idx_item` (`order_item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Φ«óσìò-σ║ôσ¡ÿΘöüσ«Üσà│τ│╗';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_inventory_lock`
--

LOCK TABLES `order_inventory_lock` WRITE;
/*!40000 ALTER TABLE `order_inventory_lock` DISABLE KEYS */;
INSERT INTO `order_inventory_lock` VALUES (1,1,1,1,10.000,'shipped','2026-05-25 23:24:16','2026-05-26 01:56:15'),(2,2,2,2,10.000,'shipped','2026-05-25 23:28:15','2026-05-26 00:01:33'),(3,3,4,3,20.000,'shipped','2026-05-26 01:56:56','2026-05-26 01:56:59');
/*!40000 ALTER TABLE `order_inventory_lock` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_item`
--

DROP TABLE IF EXISTS `order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `qty` decimal(12,3) NOT NULL,
  `unit_price` decimal(12,2) NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `qty_shipped` decimal(12,3) NOT NULL DEFAULT '0.000',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_sku` (`sku_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Φ«óσìòΦíî';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_item`
--

LOCK TABLES `order_item` WRITE;
/*!40000 ALTER TABLE `order_item` DISABLE KEYS */;
INSERT INTO `order_item` VALUES (1,1,1,10.000,1000.00,10000.00,10.000,'','2026-05-25 23:18:44'),(2,2,2,10.000,200.00,2000.00,10.000,'','2026-05-25 23:24:03'),(4,3,3,20.000,200.00,4000.00,20.000,'','2026-05-26 01:56:44'),(5,4,1,10.000,500.00,5000.00,0.000,'','2026-05-26 03:28:11');
/*!40000 ALTER TABLE `order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `packaging_spec`
--

DROP TABLE IF EXISTS `packaging_spec`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `packaging_spec` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'σªé 250gΘÇÅµÿÄτ¢Æ',
  `unit_net_kg` decimal(8,3) NOT NULL COMMENT 'σìòΣ╗╢σçÇΘçì kg',
  `unit_gross_kg` decimal(8,3) DEFAULT NULL COMMENT 'σìòΣ╗╢µ»¢Θçì kg',
  `material` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σîàΦúàΦºäµá╝';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `packaging_spec`
--

LOCK TABLES `packaging_spec` WRITE;
/*!40000 ALTER TABLE `packaging_spec` DISABLE KEYS */;
INSERT INTO `packaging_spec` VALUES (1,'SP-250G','250g Clear Punnet',0.250,0.280,'PET',1,'2026-05-25 16:05:09'),(2,'SP-500G','500g Resealable Bag',0.500,0.510,'PE',1,'2026-05-25 16:05:09'),(3,'SP-1KG','1kg Gift Box',1.000,1.100,'Paper + Liner',1,'2026-05-25 16:05:09'),(4,'SP-5KG','5kg Crate',5.000,5.500,'PP Crate',1,'2026-05-25 16:05:09');
/*!40000 ALTER TABLE `packaging_spec` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `packing`
--

DROP TABLE IF EXISTS `packing`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `packing` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PK-20260520-001',
  `batch_id` bigint NOT NULL,
  `grade` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
  `spec_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `qty_units` int NOT NULL,
  `net_weight_kg` decimal(12,3) NOT NULL,
  `location_id` bigint NOT NULL,
  `packed_at` datetime NOT NULL,
  `operator_id` bigint NOT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_sku` (`sku_id`),
  KEY `idx_packed_at` (`packed_at`),
  CONSTRAINT `packing_chk_1` CHECK ((`qty_units` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σîàΦúà';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `packing`
--

LOCK TABLES `packing` WRITE;
/*!40000 ALTER TABLE `packing` DISABLE KEYS */;
INSERT INTO `packing` VALUES (1,'PK-20260525-001',2,'A',4,1,100,500.000,1,'2026-05-25 11:19:47',1,'','2026-05-25 19:20:01'),(2,'PK-20260525-002',1,'A',1,2,100,25.000,4,'2026-05-25 15:22:04',1,'','2026-05-25 23:22:18'),(3,'PK-20260525-003',3,'A',3,3,100,100.000,1,'2026-05-25 17:55:07',1,'','2026-05-26 01:55:29'),(4,'PK-20260527-001',1,'A',3,4,1,1.000,1,'2026-05-27 20:42:48',1,'','2026-05-28 04:43:15');
/*!40000 ALTER TABLE `packing` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment`
--

DROP TABLE IF EXISTS `payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `order_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `currency` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'KES',
  `fx_rate` decimal(12,6) DEFAULT '1.000000',
  `amount_kes` decimal(14,2) NOT NULL,
  `method` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'cash/bank/cheque/loop_online/loop_pos',
  `payment_date` date NOT NULL,
  `reference_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pos_terminal_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'POS terminal/Till Number',
  `channel` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'mpesa/card/bank (Loop webhook)',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'cleared',
  `reconciled_by` bigint DEFAULT NULL,
  `reconciled_at` datetime DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_customer_date` (`customer_id`,`payment_date`),
  KEY `idx_method` (`method`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment`
--

LOCK TABLES `payment` WRITE;
/*!40000 ALTER TABLE `payment` DISABLE KEYS */;
/*!40000 ALTER TABLE `payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `planting_plan`
--

DROP TABLE IF EXISTS `planting_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `planting_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PL-26-0001',
  `plot_id` bigint NOT NULL,
  `crop_id` bigint NOT NULL,
  `variety_id` bigint DEFAULT NULL,
  `area_mu` decimal(10,2) NOT NULL,
  `plan_start_date` date NOT NULL,
  `plan_harvest_date` date NOT NULL,
  `actual_start_date` date DEFAULT NULL,
  `actual_finish_date` date DEFAULT NULL,
  `target_yield_kg` decimal(12,2) DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft' COMMENT 'draft/planned/in_progress/harvested/completed/cancelled',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_plot_status` (`plot_id`,`status`),
  KEY `idx_dates` (`plan_start_date`,`plan_harvest_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='τºìµñìΦ«íσêÆ';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `planting_plan`
--

LOCK TABLES `planting_plan` WRITE;
/*!40000 ALTER TABLE `planting_plan` DISABLE KEYS */;
INSERT INTO `planting_plan` VALUES (1,'PL-26-0001',1,1,1,2.00,'2026-06-01','2026-09-01',NULL,NULL,5000.00,'draft','Nairobi Block A tomato / cherry, post-rainy season start','2026-05-27 08:23:29',NULL,NULL,NULL,NULL),(2,'PL-26-0002',2,5,5,12.00,'2026-04-01','2026-09-15',NULL,NULL,50000.00,'planned','Nakuru main maize season, H614 hybrid (staple)','2026-05-27 08:23:29',NULL,NULL,NULL,NULL),(3,'PL-26-0003',3,9,12,8.00,'2026-05-01','2026-10-01',NULL,NULL,30000.00,'in_progress','Eldoret soybean, rotation after last year\'s maize (nitrogen recovery)','2026-05-27 08:23:29',NULL,NULL,NULL,NULL),(4,'PL-26-0004',4,4,NULL,3.00,'2026-03-01','2026-07-01',NULL,NULL,8000.00,'harvested','Mombasa off-season strawberry, harvest completed July, yield above target','2026-05-27 08:23:29',NULL,NULL,NULL,NULL),(5,'PL-26-0005',5,6,7,15.00,'2026-01-01','2027-06-01',NULL,NULL,100000.00,'in_progress','Kisumu avocado Hass, long-cycle orchard, first-year fruit set in progress','2026-05-27 08:23:29',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `planting_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `plot`
--

DROP TABLE IF EXISTS `plot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'P-001',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `area_mu` decimal(10,2) NOT NULL,
  `location` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'τ╗Åτ║¼σ║ªµêûµÅÅΦ┐░',
  `soil_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `irrigation` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `owner_id` bigint NOT NULL COMMENT 'staff_id',
  `allowed_crops` json DEFAULT NULL COMMENT 'σÅ»τºìΣ╜£τë⌐ id µò░τ╗ä',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT 'active/inactive/fallow',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_owner_status` (`owner_id`,`status`),
  KEY `idx_status` (`status`),
  CONSTRAINT `plot_chk_1` CHECK ((`area_mu` > 0))
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σ£░σ¥ù';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `plot`
--

LOCK TABLES `plot` WRITE;
/*!40000 ALTER TABLE `plot` DISABLE KEYS */;
INSERT INTO `plot` VALUES (1,'P-001','Nairobi Block A',5.50,'Nairobi ? 1.2864S, 36.8172E','loam','drip',1,NULL,'active','Pilot greenhouse for high-value short-cycle crops (lettuce, tomato)','2026-05-27 08:23:29',NULL,NULL,NULL,NULL),(2,'P-002','Nakuru Plot 1',12.00,'Nakuru ? 0.3031S, 36.0800E','sand','drip',1,NULL,'active','Staple grain area, maize/soybean rotation','2026-05-27 08:23:29',NULL,NULL,NULL,NULL),(3,'P-003','Eldoret Field',8.00,'Eldoret ? 0.5143N, 35.2698E','clay','furrow',1,NULL,'active','Highland climate, suitable for soybean / tea','2026-05-27 08:23:29',NULL,NULL,NULL,NULL),(4,'P-004','Mombasa Greenhouse',3.00,'Mombasa ? 4.0435S, 39.6682E','sand','drip',1,NULL,'active','Coastal greenhouse, off-season strawberry / vegetables','2026-05-27 08:23:29',NULL,NULL,NULL,NULL),(5,'P-005','Kisumu Lakeside',15.00,'Kisumu ? 0.0917S, 34.7680E','loam','drip',1,NULL,'active','Lake Victoria shore, long-cycle orchard (avocado / pineapple)','2026-05-27 08:23:29',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `plot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_order`
--

DROP TABLE IF EXISTS `purchase_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PO-YYYYMMDD-NNNN ????????????',
  `supplier_id` bigint NOT NULL,
  `order_date` date NOT NULL,
  `expected_date` date DEFAULT NULL COMMENT '???????????????',
  `currency` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'KES',
  `fx_rate` decimal(12,6) NOT NULL DEFAULT '1.000000' COMMENT 'to KES',
  `total_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '???????????? (currency)',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft' COMMENT 'draft / confirmed / partial_received / received / cancelled',
  `payment_status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'unpaid' COMMENT 'unpaid / partial / paid (??? VendorPaymentService ????????????)',
  `paid_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '??????????????? (KES ?????????)',
  `due_date` date DEFAULT NULL COMMENT '????????? = order_date + supplier.credit_days',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL COMMENT '??????',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_supplier` (`supplier_id`),
  KEY `idx_status` (`status`),
  KEY `idx_payment_status_due` (`payment_status`,`due_date`),
  KEY `idx_order_date` (`order_date`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='??????????????? - Sprint 17';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_order`
--

LOCK TABLES `purchase_order` WRITE;
/*!40000 ALTER TABLE `purchase_order` DISABLE KEYS */;
INSERT INTO `purchase_order` VALUES (1,'PO-20260526-0001',2,'2026-05-26','2026-05-28','KES',1.000000,500000.00,'confirmed','unpaid',0.00,'2026-06-01','Q2 fertilizer stocking','2026-05-26 16:35:11',NULL,'2026-05-27 06:29:50',NULL),(2,'PO-20260526-0002',2,'2026-05-26','2026-05-26','KES',1.000000,2000.00,'received','paid',2000.00,'2026-06-25','','2026-05-26 22:41:09',NULL,'2026-05-26 22:41:09',NULL),(3,'PO-20260526-0003',2,'2026-05-26','2026-05-26','KES',1.000000,72500.00,'received','paid',72500.00,'2026-04-22','','2026-05-27 05:12:48',NULL,'2026-05-27 05:21:02',NULL),(4,'PO-20260527-0001',1,'2026-05-27','2026-05-27','KES',1.000000,1000.00,'confirmed','unpaid',0.00,'2026-05-27','','2026-05-27 22:19:33',NULL,'2026-05-27 22:19:33',NULL),(5,'PO-20260527-0002',2,'2026-05-27','2026-05-27','KES',1.000000,1000.00,'received','paid',1000.00,'2026-06-26','','2026-05-27 22:21:06',NULL,'2026-05-27 22:21:06',NULL),(6,'PO-20260527-0003',2,'2026-05-27','2026-05-27','KES',1.000000,100000.00,'received','paid',100000.00,'2026-06-26','','2026-05-28 02:24:47',NULL,'2026-05-28 02:24:47',NULL),(7,'PO-20260527-0004',1,'2026-05-27','2026-05-27','KES',1.000000,50000.00,'received','paid',50000.00,'2026-05-27','','2026-05-28 04:44:51',NULL,'2026-05-28 04:44:51',NULL),(8,'PO-20260527-0005',1,'2026-05-27','2026-05-27','KES',1.000000,2000.00,'received','paid',2000.00,'2026-05-27','','2026-05-28 04:50:59',NULL,'2026-05-28 04:50:59',NULL);
/*!40000 ALTER TABLE `purchase_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_order_item`
--

DROP TABLE IF EXISTS `purchase_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `purchase_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `po_id` bigint NOT NULL,
  `input_item_id` bigint DEFAULT NULL COMMENT 'FK -> input_item.id (├¿┬╜┬»├Ñ┬ñΓÇô├⌐ΓÇ¥┬«, ├Ñ┼╜ΓÇá├Ñ┬Å┬▓├ªΓÇó┬░├ª┬ì┬«├Ñ┬Å┬»├º┬⌐┬║)',
  `input_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'labor / water / electricity / fertilizer / seed / pesticide / equipment / service / other',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '???: NPK 17:17:17 fertilizer 50kg',
  `quantity` decimal(14,3) NOT NULL,
  `unit` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'bag / kg / L / hour / person-day / lump-sum',
  `unit_price` decimal(14,2) NOT NULL COMMENT 'currency',
  `amount` decimal(14,2) NOT NULL COMMENT '= quantity ?? unit_price',
  `received_qty` decimal(14,3) NOT NULL DEFAULT '0.000' COMMENT '???????????? (??????????????????)',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_po` (`po_id`),
  KEY `idx_input_type` (`input_type`),
  KEY `idx_input_item` (`input_item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='?????????????????? - Sprint 17';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_order_item`
--

LOCK TABLES `purchase_order_item` WRITE;
/*!40000 ALTER TABLE `purchase_order_item` DISABLE KEYS */;
INSERT INTO `purchase_order_item` VALUES (1,1,NULL,'fertilizer','NPK 17:17:17 50kg bag',10.000,'bag',5000.00,50000.00,10.000,NULL,'2026-05-26 16:35:11'),(2,1,NULL,'fertilizer','Urea 50kg bag',5.000,'bag',4500.00,22500.00,5.000,NULL,'2026-05-26 16:35:11'),(4,2,NULL,'fertilizer','100kg /bag',1.000,'bag',2000.00,2000.00,1.000,'','2026-05-26 22:41:25'),(5,3,NULL,'fertilizer','NPK 17:17:17 50kg bag',10.000,'bag',5000.00,50000.00,10.000,'','2026-05-27 05:12:48'),(6,3,NULL,'fertilizer','Urea 50kg bag',5.000,'bag',4500.00,22500.00,5.000,'','2026-05-27 05:12:48'),(7,4,19,'seed','Maize Seed H614',1.000,'kg',1000.00,1000.00,0.000,'','2026-05-27 22:19:33'),(8,5,17,'fertilizer','NPK 17-17-17',1.000,'kg',1000.00,1000.00,1.000,'','2026-05-27 22:21:06'),(9,6,16,'fertilizer','Urea 46-0-0',100.000,'kg',1000.00,100000.00,100.000,'','2026-05-28 02:24:47'),(10,7,19,'seed','Maize Seed H614',50.000,'kg',1000.00,50000.00,50.000,'','2026-05-28 04:44:51'),(11,8,19,'seed','Maize Seed H614',10.000,'pcs',200.00,2000.00,10.000,'','2026-05-28 04:50:59');
/*!40000 ALTER TABLE `purchase_order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qc_inspection`
--

DROP TABLE IF EXISTS `qc_inspection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_inspection` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'QC-YYYYMMDD-NNNN',
  `inspection_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'incoming / in_process / outgoing',
  `ref_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'warehouse_inbound / activity / planting_plan / packing / batch',
  `ref_id` bigint DEFAULT NULL COMMENT 'ID of the referenced entity',
  `ref_code` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Snapshot code (e.g., IN-20260528-0001)',
  `inspect_date` date NOT NULL,
  `inspector_id` bigint DEFAULT NULL COMMENT 'FK -> sys_user.id',
  `result` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending / pass / conditional_pass / fail',
  `result_remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '├ªΓé¼┬╗├ñ┬╜ΓÇ£├º┬╗ΓÇ£├¿┬«┬║├Ñ┬ñΓÇí├ª┬│┬¿',
  `photo_ids` json DEFAULT NULL COMMENT '├⌐ΓäóΓÇ₧├ñ┬╗┬╢├ºΓÇª┬º├ºΓÇ░ΓÇí sys_file.id ├ªΓÇó┬░├º┬╗ΓÇ₧',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_qc_type` (`inspection_type`),
  KEY `idx_qc_result` (`result`),
  KEY `idx_qc_ref` (`ref_type`,`ref_id`),
  KEY `idx_qc_date` (`inspect_date` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='QC inspection orders ├óΓé¼ΓÇ¥ Sprint 24';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qc_inspection`
--

LOCK TABLES `qc_inspection` WRITE;
/*!40000 ALTER TABLE `qc_inspection` DISABLE KEYS */;
INSERT INTO `qc_inspection` VALUES (1,'QC-20260528-0001','incoming','',NULL,'IN-20260528-0001 ','2026-05-28',NULL,'pass','',NULL,'','2026-05-28 15:31:30',NULL,'2026-05-28 15:31:30');
/*!40000 ALTER TABLE `qc_inspection` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qc_inspection_item`
--

DROP TABLE IF EXISTS `qc_inspection_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qc_inspection_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `inspection_id` bigint NOT NULL,
  `check_point` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '├ª┬úΓé¼├ª┼╕┬Ñ├⌐┬í┬╣ (├Ñ┬ñΓÇô├¿┬ºΓÇÜ/├⌐ΓÇí┬ì├⌐ΓÇí┬Å/├ª┬░┬┤├Ñ╦åΓÇá/├ª┬«ΓÇ╣├ºΓÇóΓäó├⌐ΓÇí┬Å...)',
  `expected_value` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '├ª┼ô┼╕├ª┼ôΓÇ║├ÑΓé¼┬╝/├¿┼Æ╞Æ├ÑΓÇ║┬┤ (e.g., 8-10% ├ª┬░┬┤├Ñ╦åΓÇá)',
  `actual_value` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '├Ñ┬«┼╛├ª┬╡ΓÇ╣├ÑΓé¼┬╝',
  `result` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT 'pending' COMMENT 'pass / fail / pending',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_qci_parent` (`inspection_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='QC inspection items ├óΓé¼ΓÇ¥ Sprint 24';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qc_inspection_item`
--

LOCK TABLES `qc_inspection_item` WRITE;
/*!40000 ALTER TABLE `qc_inspection_item` DISABLE KEYS */;
INSERT INTO `qc_inspection_item` VALUES (3,1,'σñûΦºéσ«îσÑ╜','σ«îσÑ╜','σ«îσÑ╜','pass',''),(4,1,'µ░┤σêå','Γëñ12%','10%','pass','');
/*!40000 ALTER TABLE `qc_inspection_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recall`
--

DROP TABLE IF EXISTS `recall`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recall` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) NOT NULL COMMENT 'RECALL-YYYYMMDD-NNNN',
  `triggered_at` datetime NOT NULL,
  `source_complaint_id` bigint DEFAULT NULL COMMENT 'NULL = manually initiated by QC',
  `batch_id` bigint NOT NULL,
  `scope` varchar(24) NOT NULL DEFAULT 'batch_only' COMMENT 'batch_only / batch_plus_children',
  `reason` text NOT NULL,
  `status` varchar(24) NOT NULL DEFAULT 'initiated' COMMENT 'initiated / quarantined / customers_notified / closed',
  `affected_order_count` int NOT NULL DEFAULT '0',
  `affected_customer_count` int NOT NULL DEFAULT '0',
  `affected_qty` decimal(14,3) NOT NULL DEFAULT '0.000' COMMENT 'Total frozen quantity',
  `initiated_by_id` bigint DEFAULT NULL,
  `closed_at` datetime DEFAULT NULL,
  `closed_by_id` bigint DEFAULT NULL,
  `closed_remark` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_batch` (`batch_id`),
  KEY `idx_status` (`status`),
  KEY `idx_triggered_at` (`triggered_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Batch recall ??? quarantine + downstream notification';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recall`
--

LOCK TABLES `recall` WRITE;
/*!40000 ALTER TABLE `recall` DISABLE KEYS */;
/*!40000 ALTER TABLE `recall` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recall_affected_order`
--

DROP TABLE IF EXISTS `recall_affected_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recall_affected_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recall_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `order_code` varchar(64) NOT NULL,
  `customer_id` bigint NOT NULL,
  `customer_name` varchar(128) NOT NULL,
  `qty` decimal(14,3) NOT NULL,
  `unit` varchar(8) DEFAULT 'pack',
  `delivered_at` datetime DEFAULT NULL COMMENT 'From fulfillment.shipped_at if available',
  `notified_at` datetime DEFAULT NULL COMMENT 'Set when sales confirms customer was notified',
  `notified_by_id` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recall` (`recall_id`),
  KEY `idx_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Recall affected orders snapshot';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recall_affected_order`
--

LOCK TABLES `recall_affected_order` WRITE;
/*!40000 ALTER TABLE `recall_affected_order` DISABLE KEYS */;
/*!40000 ALTER TABLE `recall_affected_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `revenue`
--

DROP TABLE IF EXISTS `revenue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `revenue` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `order_item_id` bigint NOT NULL,
  `fulfillment_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `batch_id` bigint DEFAULT NULL COMMENT '???????????? (?????????????????????????????????)',
  `qty` decimal(12,3) NOT NULL,
  `gross_amount` decimal(14,2) NOT NULL,
  `tax` decimal(14,2) NOT NULL DEFAULT '0.00',
  `net_amount` decimal(14,2) NOT NULL,
  `currency` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'KES',
  `recognition_date` date NOT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'recognized' COMMENT 'recognized / reversed / adjusted',
  `channel` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'b2b / retail / export / online',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_customer_date` (`customer_id`,`recognition_date`),
  KEY `idx_sku_date` (`sku_id`,`recognition_date`),
  KEY `idx_date` (`recognition_date`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='???????????? - V2.0 P&L ?????????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `revenue`
--

LOCK TABLES `revenue` WRITE;
/*!40000 ALTER TABLE `revenue` DISABLE KEYS */;
INSERT INTO `revenue` VALUES (1,2,2,2,2,1,1,10.000,2000.00,0.00,2000.00,'KES','2026-05-26','recognized',NULL,'Auto-generated by fulfillment SH-20260525-002','2026-05-26 00:01:34',NULL),(2,1,1,1,1,1,2,10.000,10000.00,0.00,10000.00,'KES','2026-05-26','recognized',NULL,'Auto-generated by fulfillment SH-20260525-001','2026-05-26 01:56:16',NULL),(3,3,4,3,3,2,3,20.000,4000.00,0.00,4000.00,'KES','2026-05-26','recognized',NULL,'Auto-generated by fulfillment SH-20260526-001','2026-05-26 01:56:59',NULL);
/*!40000 ALTER TABLE `revenue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sales_order`
--

DROP TABLE IF EXISTS `sales_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sales_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SO-20260520-001',
  `customer_id` bigint NOT NULL,
  `order_date` date NOT NULL,
  `delivery_date` date NOT NULL,
  `ship_to` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `currency` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'KES' COMMENT 'KES / USD / EUR',
  `total_amount` decimal(14,2) NOT NULL DEFAULT '0.00',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending' COMMENT 'pending/confirmed/locked/shipping/shipped/delivered/completed/cancelled/returned',
  `payment_status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'unpaid',
  `paid_amount` decimal(14,2) NOT NULL DEFAULT '0.00',
  `due_date` date DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_customer_status` (`customer_id`,`status`),
  KEY `idx_date_status` (`order_date`,`status`),
  KEY `idx_status` (`status`),
  KEY `idx_payment_status_due` (`payment_status`,`due_date`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ΘöÇσö«Φ«óσìò';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sales_order`
--

LOCK TABLES `sales_order` WRITE;
/*!40000 ALTER TABLE `sales_order` DISABLE KEYS */;
INSERT INTO `sales_order` VALUES (1,'SO-20260525-001',1,'2026-05-25','2026-05-25','China Town','KES',10000.00,'completed','unpaid',0.00,'2026-04-26','','2026-05-25 23:18:44',NULL,'2026-05-26 04:21:16',NULL,NULL),(2,'SO-20260525-002',1,'2026-05-25','2026-05-25','China Town','KES',2000.00,'completed','unpaid',0.00,NULL,'','2026-05-25 23:24:03',NULL,'2026-05-25 23:24:03',NULL,NULL),(3,'SO-20260525-003',2,'2026-05-25','2026-05-25','Westlands','KES',4000.00,'completed','unpaid',0.00,NULL,'','2026-05-26 01:56:05',NULL,'2026-05-26 01:56:05',NULL,NULL),(4,'SO-20260525-004',1,'2026-05-25','2026-05-25','Westlands','KES',5000.00,'pending','unpaid',0.00,'2026-06-01','','2026-05-26 03:28:11',NULL,'2026-05-26 03:31:56',NULL,'2026-05-26 03:31:56');
/*!40000 ALTER TABLE `sales_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sku`
--

DROP TABLE IF EXISTS `sku`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sku` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `crop_id` bigint NOT NULL,
  `variety_id` bigint DEFAULT NULL,
  `grade` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
  `spec_id` bigint NOT NULL,
  `unit` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pack',
  `status` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  UNIQUE KEY `uk_dim` (`crop_id`,`variety_id`,`grade`,`spec_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SKU σòåσôü';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sku`
--

LOCK TABLES `sku` WRITE;
/*!40000 ALTER TABLE `sku` DISABLE KEYS */;
INSERT INTO `sku` VALUES (1,'SKU-CR-009-V-001-A-SP-5KG','Soybean SB19 ┬╖ A ┬╖ 5kg Crate',9,12,'A',4,'pack',1,'2026-05-25 19:20:01'),(2,'SKU-CR-004-NA-A-SP-250G','Strawberry ┬╖ A ┬╖ 250g Clear Punnet',4,NULL,'A',1,'pack',1,'2026-05-25 23:22:18'),(3,'SKU-CR-001-V-001-A-SP-1KG','Tomato Cherry Tomato ┬╖ A ┬╖ 1kg Gift Box',1,1,'A',3,'pack',1,'2026-05-26 01:55:29'),(4,'SKU-CR-004-NA-A-SP-1KG','Strawberry ┬╖ A ┬╖ 1kg Gift Box',4,NULL,'A',3,'pack',1,'2026-05-28 04:43:15');
/*!40000 ALTER TABLE `sku` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sms_log`
--

DROP TABLE IF EXISTS `sms_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sms_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint NOT NULL,
  `order_id` bigint DEFAULT NULL COMMENT '???????????????',
  `template_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '?????????????????????',
  `channel` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'sms',
  `phone` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '+254...',
  `content` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????????????????????? (??????????????????)',
  `provider` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'africas_talking / twilio / stub',
  `provider_msg_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Provider ????????? message id',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'sent' COMMENT 'sent / failed / delivered / unknown',
  `error` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sent_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `operator_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_customer` (`customer_id`,`sent_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SMS / WhatsApp ????????????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sms_log`
--

LOCK TABLES `sms_log` WRITE;
/*!40000 ALTER TABLE `sms_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sms_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sms_template`
--

DROP TABLE IF EXISTS `sms_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sms_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'AR_PRE_REMIND / AR_OVERDUE / AR_PROMISE_DUE ...',
  `name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '?????????',
  `channel` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'sms' COMMENT 'sms / whatsapp',
  `lang` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'en' COMMENT 'en / zh / sw',
  `content` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '????????????, ?????? {customerName} {orderCode} {amount} {dueDate} {daysOverdue} ?????????',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SMS / WhatsApp ??????';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sms_template`
--

LOCK TABLES `sms_template` WRITE;
/*!40000 ALTER TABLE `sms_template` DISABLE KEYS */;
INSERT INTO `sms_template` VALUES (1,'AR_PRE_REMIND','Pre-due reminder (3 days before)','sms','en','Hi {customerName}, this is a friendly reminder that invoice {orderCode} ({currency} {amount}) is due on {dueDate}. Thank you for your continued business. - 2Africa AgriOS',1,'2026-05-26 03:52:03',NULL),(2,'AR_OVERDUE','Overdue reminder','sms','en','Dear {customerName}, invoice {orderCode} ({currency} {amount}) was due on {dueDate} and is now {daysOverdue} days overdue. Please arrange payment. - 2Africa AgriOS',1,'2026-05-26 03:52:03',NULL),(3,'AR_PROMISE_DUE','Promise-to-pay follow-up','sms','en','Hi {customerName}, just following up on your promise to settle invoice {orderCode} ({currency} {amount}). Kindly confirm payment status today. - 2Africa AgriOS',1,'2026-05-26 03:52:03',NULL);
/*!40000 ALTER TABLE `sms_template` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `staff`
--

DROP TABLE IF EXISTS `staff`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL COMMENT 'σà│Φüöτ│╗τ╗ƒΦ┤ªµê╖',
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id_card` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'worker/leader/packhouse/sales/manager',
  `team_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'µëÇσ▒₧σ░Åτ╗ä',
  `hire_date` date DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  UNIQUE KEY `user_id` (`user_id`),
  KEY `idx_role` (`role_type`),
  KEY `idx_team` (`team_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σæÿσ╖Ñ';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `staff`
--

LOCK TABLES `staff` WRITE;
/*!40000 ALTER TABLE `staff` DISABLE KEYS */;
/*!40000 ALTER TABLE `staff` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supplier`
--

DROP TABLE IF EXISTS `supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SUP-NNNNN ????????????',
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'input_dealer / labor_contractor / utility / equipment / service / logistics / other',
  `tax_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '?????? / KRA PIN',
  `contact_name` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_phone` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_email` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `credit_days` int NOT NULL DEFAULT '0' COMMENT '????????????: 0=COD, 7=??????, 30=??????',
  `payment_terms` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '?????? label',
  `since_date` date DEFAULT NULL COMMENT '???????????????',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT 'active / inactive',
  `remark` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL COMMENT '??????',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_status` (`status`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='?????????????????? - Sprint 17';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier`
--

LOCK TABLES `supplier` WRITE;
/*!40000 ALTER TABLE `supplier` DISABLE KEYS */;
INSERT INTO `supplier` VALUES (1,'SUP-00001','Kenya Highland Seeds Ltd','input_dealer',NULL,'Peter Mwangi','+254-700-111-001',NULL,NULL,0,'COD','2025-01-15','active',NULL,'2026-05-26 15:40:42',NULL,NULL,NULL),(2,'SUP-00002','Athi River Fertilizers','input_dealer',NULL,'Grace Wanjiru','+254-700-111-002',NULL,NULL,30,'µ£êτ╗ô','2025-02-10','active',NULL,'2026-05-26 15:40:42',NULL,'2026-05-26 16:11:20',NULL),(3,'SUP-00003','Nairobi Day Labor Co-op','labor_contractor',NULL,'James Otieno','+254-700-111-003',NULL,NULL,7,'σæ¿τ╗ô','2025-03-01','active',NULL,'2026-05-26 15:40:42',NULL,'2026-05-26 16:11:20',NULL),(4,'SUP-00004','Kenya Power','utility',NULL,'Customer Service','+254-700-111-004',NULL,NULL,30,'µ£êτ╗ô','2024-12-01','active',NULL,'2026-05-26 15:40:42',NULL,'2026-05-26 16:11:20',NULL),(5,'SUP-00005','Nairobi Water Co.','utility',NULL,'Billing Office','+254-700-111-005',NULL,NULL,30,'µ£êτ╗ô','2024-12-01','active',NULL,'2026-05-26 15:40:42',NULL,'2026-05-26 16:11:20',NULL);
/*!40000 ALTER TABLE `supplier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_code_rule`
--

DROP TABLE IF EXISTS `sys_code_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_code_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `template` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'e.g. B-{yyyyMMdd}-{plotCode}-{seq:02d}',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `rule_key` (`rule_key`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='τ╝ûτáüΦºäσêÖ';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_code_rule`
--

LOCK TABLES `sys_code_rule` WRITE;
/*!40000 ALTER TABLE `sys_code_rule` DISABLE KEYS */;
INSERT INTO `sys_code_rule` VALUES (1,'plot','P-{seq:03d}','σ£░σ¥ùτ╝ûσÅ╖','2026-05-25 16:05:09'),(2,'plan','PL-{yy}-{seq:04d}','τºìµñìΦ«íσêÆ','2026-05-25 16:05:09'),(3,'harvest','HV-{yyyyMMdd}-{seq:03d}','Θççµö╢Φ«░σ╜ò','2026-05-25 16:05:09'),(4,'batch','B-{yyyyMMdd}-{plotCode}-{seq:02d}','µë╣µ¼íσÅ╖','2026-05-25 16:05:09'),(5,'packing','PK-{yyyyMMdd}-{seq:03d}','σîàΦúàσìò','2026-05-25 16:05:09'),(6,'order','SO-{yyyyMMdd}-{seq:03d}','Φ«óσìò','2026-05-25 16:05:09'),(7,'fulfillment','SH-{yyyyMMdd}-{seq:03d}','σç║σ║ôσìò','2026-05-25 16:05:09'),(8,'customer','CUS-{seq:05d}','σ«óµê╖','2026-05-25 16:05:09'),(9,'staff','EMP-{seq:04d}','σæÿσ╖Ñ','2026-05-25 16:05:09'),(10,'input','IN-{seq:05d}','µèòσàÑσôü','2026-05-25 16:05:09'),(11,'crop','CR-{seq:03d}','Σ╜£τë⌐','2026-05-25 16:05:09');
/*!40000 ALTER TABLE `sys_code_rule` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dict_label` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dict_value` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort` int NOT NULL DEFAULT '0',
  `css_class` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σ¡ùσà╕µò░µì«';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

LOCK TABLES `sys_dict_data` WRITE;
/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
INSERT INTO `sys_dict_data` VALUES (1,'activity_type','µÆ¡τºì','sow',1,NULL,1),(2,'activity_type','µû╜ΦéÑ','fertilize',2,NULL,1),(3,'activity_type','µëôΦì»','spray',3,NULL,1),(4,'activity_type','ΘÖñΦìë','weed',4,NULL,1),(5,'activity_type','τüîµ║ë','water',5,NULL,1),(6,'activity_type','Σ┐«σë¬','prune',6,NULL,1),(7,'activity_type','σà╢Σ╗û','other',99,NULL,1),(8,'soil_type','σúñσ£ƒ','loam',1,NULL,1),(9,'soil_type','µ▓Öσ£ƒ','sand',2,NULL,1),(10,'soil_type','Θ╗Åσ£ƒ','clay',3,NULL,1),(11,'soil_type','τ¢Éτó▒σ£░','saline',4,NULL,1),(12,'irrigation','µ╗┤τüî','drip',1,NULL,1),(13,'irrigation','σû╖τüî','spray',2,NULL,1),(14,'irrigation','µ▓ƒτüî','furrow',3,NULL,1),(15,'input_type','ΦéÑµûÖ','fertilizer',1,NULL,1),(16,'input_type','σå£Φì»','pesticide',2,NULL,1),(17,'input_type','τºìσ¡É','seed',3,NULL,1),(18,'input_type','σ£░Φå£','film',4,NULL,1),(19,'input_type','σà╢Σ╗û','other',99,NULL,1),(20,'customer_type','σòåΦ╢à','supermarket',1,NULL,1),(21,'customer_type','ΘñÉΘÑ«','restaurant',2,NULL,1),(22,'customer_type','τö╡σòå','ecommerce',3,NULL,1),(23,'customer_type','µë╣σÅæ','wholesale',4,NULL,1),(24,'customer_type','σà╢Σ╗û','other',99,NULL,1);
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dict_type` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dict_name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σ¡ùσà╕τ▒╗σ₧ï';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_type`
--

LOCK TABLES `sys_dict_type` WRITE;
/*!40000 ALTER TABLE `sys_dict_type` DISABLE KEYS */;
INSERT INTO `sys_dict_type` VALUES (1,'activity_type','σå£Σ║ïτ▒╗σ₧ï',NULL,'2026-05-25 16:05:09'),(2,'soil_type','σ£ƒσúñτ▒╗σ₧ï',NULL,'2026-05-25 16:05:09'),(3,'irrigation','τüîµ║ëµû╣σ╝Å',NULL,'2026-05-25 16:05:09'),(4,'input_type','µèòσàÑσôüτ▒╗σ₧ï',NULL,'2026-05-25 16:05:09'),(5,'customer_type','σ«óµê╖τ▒╗σ₧ï',NULL,'2026-05-25 16:05:09'),(6,'order_status','Φ«óσìòτè╢µÇü',NULL,'2026-05-25 16:05:09'),(7,'batch_status','µë╣µ¼íτè╢µÇü',NULL,'2026-05-25 16:05:09'),(8,'plan_status','τºìµñìΦ«íσêÆτè╢µÇü',NULL,'2026-05-25 16:05:09');
/*!40000 ALTER TABLE `sys_dict_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_file`
--

DROP TABLE IF EXISTS `sys_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_file` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `object_key` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MinIO/OSS σ»╣Φ▒í key',
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Σ╕èΣ╝áµù╢τÜäσÄƒσºïµûçΣ╗╢σÉì',
  `bucket` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `size_bytes` bigint NOT NULL,
  `mime_type` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ext` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `biz_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Σ╕Üσèíσêåτ▒╗: avatar/crop_image/activity_photo/...',
  `uploaded_by` bigint DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `object_key` (`object_key`),
  KEY `idx_uploaded_by` (`uploaded_by`),
  KEY `idx_biz_type` (`biz_type`),
  KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='µûçΣ╗╢Σ╕èΣ╝áΦ«░σ╜ò';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_file`
--

LOCK TABLES `sys_file` WRITE;
/*!40000 ALTER TABLE `sys_file` DISABLE KEYS */;
INSERT INTO `sys_file` VALUES (1,'demo_gallery/2026/05/7f1ef762c6bb4c7299bec9317803e0aa.png','ChatGPT Image May 24, 2026, 09_45_12 PM.png','2africa-agrios',2268477,'image/png','png','demo_gallery',1,'2026-05-25 16:15:45',NULL),(2,'demo_attach/2026/05/dc548bf26d454d57b3529bffa847fec6.pdf','_26447000000531542996 (2).pdf','2africa-agrios',64202,'application/pdf','pdf','demo_attach',1,'2026-05-25 16:15:50',NULL),(3,'activity_photo/2026/05/d97ceda5447c4c838fd6d108a6e3b7bf.png','c8ff56c6-1441-48ee-b388-647c7dffff85.png','2africa-agrios',784819,'image/png','png','activity_photo',1,'2026-05-25 18:17:04',NULL),(4,'activity_photo/2026/05/5bd393e7add24e67b84c60f771542ddc.jpg','σ╛«Σ┐íσ¢╛τëç_20260127220605_139_3649.jpg','2africa-agrios',168508,'image/jpeg','jpg','activity_photo',1,'2026-05-27 08:10:21',NULL),(5,'activity_photo/2026/05/5f716a43e0754200bfd9290eeb0a2582.jpg','σ╛«Σ┐íσ¢╛τëç_20260127220605_139_3649.jpg','2africa-agrios',168508,'image/jpeg','jpg','activity_photo',1,'2026-05-27 08:21:33',NULL);
/*!40000 ALTER TABLE `sys_file` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint NOT NULL DEFAULT '0',
  `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'menu/button',
  `path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `component` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `perms` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'µ¥âΘÖÉµáçΦ»å e.g. plot:list',
  `icon` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort` int NOT NULL DEFAULT '0',
  `visible` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ΦÅ£σìò/µîëΘÆ«';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `username` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `module` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `action` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `method` varchar(8) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `req_params` text COLLATE utf8mb4_unicode_ci,
  `resp_status` int DEFAULT NULL,
  `ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cost_ms` int DEFAULT NULL,
  `error_msg` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`,`created_at`),
  KEY `idx_target` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='µôìΣ╜£µùÑσ┐ù';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

LOCK TABLES `sys_oper_log` WRITE;
/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'WORKER/LEADER/PACKHOUSE/SALES/MANAGER',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `data_scope` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'self' COMMENT 'self/group/all',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ΦºÆΦë▓';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'SUPER_ADMIN','Φ╢àτ║ºτ«íτÉåσæÿ','all','τ│╗τ╗ƒσê¥σºïΦ┤ªσÅ╖','2026-05-25 16:05:09',NULL),(2,'MANAGER','σå£σ£║τ╗ÅτÉå','all','σà¿σ▒Çµ¥âΘÖÉ','2026-05-25 16:05:09',NULL),(3,'LEADER','σ£░σ¥ùΦ┤ƒΦ┤úΣ║║/τ╗äΘò┐','group','µ£¼τ╗äµò░µì«','2026-05-25 16:05:09',NULL),(4,'PACKHOUSE','Packhouse Σ╕╗τ«í','all','σêåτ║ºσîàΦúàσ║ôσ¡ÿ','2026-05-25 16:05:09',NULL),(5,'SALES','ΘöÇσö«/σ«óµ£ì','all','σ«óµê╖Φ«óσìòσç║σ║ô','2026-05-25 16:05:09',NULL),(6,'WORKER','σå£σ£║σ╖ÑΣ║║','self','Σ╗àµ£¼Σ║║Φ«░σ╜ò','2026-05-25 16:05:09',NULL);
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ΦºÆΦë▓-ΦÅ£σìò';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

LOCK TABLES `sys_role_menu` WRITE;
/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BCrypt',
  `nickname` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active' COMMENT 'active/locked/disabled',
  `last_login_at` datetime DEFAULT NULL,
  `last_login_ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='τ│╗τ╗ƒτö¿µê╖';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','$2b$10$GFHq9PcQS8SvCpf8pDczfuYnJUT0Nf.hBHNA3b6/7z5JPX4VC5srC','Administrator',NULL,NULL,NULL,'active','2026-05-28 21:48:32','172.18.0.1','2026-05-25 16:05:09',NULL,'2026-05-28 21:48:32',NULL),(2,'worker','$2b$10$GFHq9PcQS8SvCpf8pDczfuYnJUT0Nf.hBHNA3b6/7z5JPX4VC5srC','John Mwangi',NULL,NULL,NULL,'active',NULL,NULL,'2026-05-27 09:12:58',NULL,NULL,NULL);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='τö¿µê╖-ΦºÆΦë▓';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

LOCK TABLES `sys_user_role` WRITE;
/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` VALUES (1,1),(2,6);
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `variety`
--

DROP TABLE IF EXISTS `variety`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `variety` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `crop_id` bigint NOT NULL,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `traits` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'τë╣µÇº',
  `shelf_life_days` int DEFAULT NULL COMMENT 'Override of crop.shelf_life_days; NULL = use crop default',
  `status` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_crop_code` (`crop_id`,`code`),
  KEY `idx_crop` (`crop_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='σôüτºì';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `variety`
--

LOCK TABLES `variety` WRITE;
/*!40000 ALTER TABLE `variety` DISABLE KEYS */;
INSERT INTO `variety` VALUES (1,1,'V-001','Cherry Tomato','Small, high sweetness',NULL,1,'2026-05-25 16:05:09'),(2,1,'V-002','Provence Heritage','Large, juicy',NULL,1,'2026-05-25 16:05:09'),(3,2,'V-001','Mini Snack','Short, crisp',NULL,1,'2026-05-25 16:05:09'),(4,3,'V-001','Butterhead','Soft, buttery texture',NULL,1,'2026-05-25 16:05:09'),(5,5,'V-001','H614','KARI hybrid, high yield, drought tolerant',NULL,1,'2026-05-27 08:23:29'),(6,5,'V-002','WH505','White maize, long kernel, staple market favorite',NULL,1,'2026-05-27 08:23:29'),(7,6,'V-001','Hass','Dark thick skin, export grade',NULL,1,'2026-05-27 08:23:29'),(8,6,'V-002','Fuerte','Green thin skin, local market',NULL,1,'2026-05-27 08:23:29'),(9,7,'V-001','TRFK 31/8','TRFK improved, high-aroma broadleaf',NULL,1,'2026-05-27 08:23:29'),(10,8,'V-001','SL28','Classic Kenya specialty, Blue Mountain-like profile',NULL,1,'2026-05-27 08:23:29'),(11,8,'V-002','Ruiru 11','CBD-resistant improved variety',NULL,1,'2026-05-27 08:23:29'),(12,9,'V-001','SB19','High protein, early maturing',NULL,1,'2026-05-27 08:23:29'),(13,10,'V-001','MD2','High sweetness, export mainstream',NULL,1,'2026-05-27 08:23:29');
/*!40000 ALTER TABLE `variety` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vendor_payment`
--

DROP TABLE IF EXISTS `vendor_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vendor_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'VPAY-YYYYMMDD-NNNN',
  `po_id` bigint NOT NULL,
  `supplier_id` bigint NOT NULL,
  `amount` decimal(14,2) NOT NULL,
  `currency` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'KES',
  `fx_rate` decimal(12,6) NOT NULL DEFAULT '1.000000',
  `amount_kes` decimal(14,2) NOT NULL COMMENT '???????????????',
  `method` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'cash / bank / cheque / loop_online / loop_pos',
  `payment_date` date NOT NULL,
  `reference_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '???????????? / ????????? / Loop ??????',
  `pos_terminal_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'loop_pos ?????????',
  `channel` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'mpesa / card / bank',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'cleared' COMMENT 'pending / cleared / reversed',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_po` (`po_id`),
  KEY `idx_supplier` (`supplier_id`,`payment_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='??????????????? - Sprint 17';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vendor_payment`
--

LOCK TABLES `vendor_payment` WRITE;
/*!40000 ALTER TABLE `vendor_payment` DISABLE KEYS */;
INSERT INTO `vendor_payment` VALUES (1,'VPAY-20260526-0001',1,2,50000.00,'KES',1.000000,50000.00,'bank','2026-05-26','BANK-REF-001',NULL,NULL,'cleared',NULL,'2026-05-26 23:13:12',NULL,'2026-05-26 23:13:12'),(2,'VPAY-20260526-0002',2,2,2000.00,'KES',1.000000,2000.00,'bank','2026-05-26','F404949949944','','','cleared','','2026-05-26 23:23:11',NULL,'2026-05-26 23:23:11'),(3,'VPAY-20260527-0003',1,2,22500.00,'KES',1.000000,22500.00,'bank','2026-05-26','Stabic-202605260001','','','cleared','','2026-05-27 04:48:58',NULL,'2026-05-27 04:48:58'),(4,'VPAY-20260527-0004',3,2,30000.00,'KES',1.000000,30000.00,'bank','2026-05-26','BANK-001','','','reversed','','2026-05-27 05:17:11',NULL,'2026-05-27 05:17:11'),(5,'VPAY-20260527-0005',3,2,42500.00,'KES',1.000000,42500.00,'bank','2026-05-26','BANK-002','','','cleared','','2026-05-27 05:17:45',NULL,'2026-05-27 05:17:45'),(6,'VPAY-20260527-0006',3,2,30000.00,'KES',1.000000,30000.00,'bank','2026-05-26','Bank-003','','','cleared','','2026-05-27 05:32:02',NULL,'2026-05-27 05:32:02'),(7,'VPAY-20260528-0007',6,2,100000.00,'KES',1.000000,100000.00,'bank','2026-05-27','Stanbic-0005','','','cleared','','2026-05-28 02:25:24',NULL,'2026-05-28 02:25:24'),(8,'VPAY-20260528-0008',5,2,1000.00,'KES',1.000000,1000.00,'bank','2026-05-27','00000123','','','cleared','','2026-05-28 04:36:32',NULL,'2026-05-28 04:36:32'),(9,'VPAY-20260528-0009',7,1,50000.00,'KES',1.000000,50000.00,'bank','2026-05-27','','','','cleared','','2026-05-28 04:45:07',NULL,'2026-05-28 04:45:07'),(10,'VPAY-20260528-0010',8,1,2000.00,'KES',1.000000,2000.00,'bank','2026-05-27','','','','cleared','','2026-05-28 04:51:12',NULL,'2026-05-28 04:51:12');
/*!40000 ALTER TABLE `vendor_payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `warehouse_inbound`
--

DROP TABLE IF EXISTS `warehouse_inbound`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_inbound` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'IN-YYYYMMDD-NNNN',
  `source_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'po_receive / return_in / transfer_in / manual',
  `source_id` bigint DEFAULT NULL COMMENT 'Polymorphic: purchase_order.id / transfer.id / NULL',
  `warehouse_id` bigint NOT NULL COMMENT 'Target warehouse (FK -> location_warehouse.id)',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft' COMMENT 'draft / confirmed / cancelled',
  `confirmed_by` bigint DEFAULT NULL COMMENT 'FK -> sys_user.id (who confirmed)',
  `confirmed_at` datetime DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_inbound_status` (`status`),
  KEY `idx_inbound_source` (`source_type`,`source_id`),
  KEY `idx_inbound_wh` (`warehouse_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Warehouse inbound order ├óΓé¼ΓÇ¥ Sprint 22.4';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `warehouse_inbound`
--

LOCK TABLES `warehouse_inbound` WRITE;
/*!40000 ALTER TABLE `warehouse_inbound` DISABLE KEYS */;
INSERT INTO `warehouse_inbound` VALUES (1,'IN-20260528-0001','po_receive',8,2,'confirmed',NULL,'2026-05-28 05:27:59',NULL,'2026-05-28 04:51:09',NULL,'2026-05-28 04:51:09');
/*!40000 ALTER TABLE `warehouse_inbound` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `warehouse_inbound_item`
--

DROP TABLE IF EXISTS `warehouse_inbound_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_inbound_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `inbound_id` bigint NOT NULL COMMENT 'FK -> warehouse_inbound.id',
  `input_item_id` bigint NOT NULL COMMENT 'FK -> input_item.id',
  `expected_qty` decimal(14,3) NOT NULL COMMENT 'Expected quantity (from PO / source)',
  `actual_qty` decimal(14,3) DEFAULT NULL COMMENT 'Actual received qty (filled by warehouse on confirm)',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_inbound_item` (`inbound_id`),
  KEY `idx_inbound_ii` (`input_item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Warehouse inbound order items ├óΓé¼ΓÇ¥ Sprint 22.4';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `warehouse_inbound_item`
--

LOCK TABLES `warehouse_inbound_item` WRITE;
/*!40000 ALTER TABLE `warehouse_inbound_item` DISABLE KEYS */;
INSERT INTO `warehouse_inbound_item` VALUES (1,1,19,10.000,10.000,'');
/*!40000 ALTER TABLE `warehouse_inbound_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `warehouse_outbound`
--

DROP TABLE IF EXISTS `warehouse_outbound`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_outbound` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'OUT-YYYYMMDD-NNNN',
  `source_type` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'activity_consume / sales_ship / transfer_out / manual',
  `source_id` bigint DEFAULT NULL COMMENT 'Polymorphic: activity.id / sales_order.id / transfer.id / NULL',
  `warehouse_id` bigint NOT NULL COMMENT 'Source warehouse (FK -> location_warehouse.id)',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft' COMMENT 'draft / picked / confirmed / cancelled',
  `picked_by` bigint DEFAULT NULL COMMENT 'FK -> sys_user.id (picker)',
  `picked_at` datetime DEFAULT NULL,
  `confirmed_by` bigint DEFAULT NULL COMMENT 'FK -> sys_user.id (confirmer)',
  `confirmed_at` datetime DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_outbound_status` (`status`),
  KEY `idx_outbound_source` (`source_type`,`source_id`),
  KEY `idx_outbound_wh` (`warehouse_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Warehouse outbound order ├óΓé¼ΓÇ¥ Sprint 22.5';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `warehouse_outbound`
--

LOCK TABLES `warehouse_outbound` WRITE;
/*!40000 ALTER TABLE `warehouse_outbound` DISABLE KEYS */;
INSERT INTO `warehouse_outbound` VALUES (1,'OUT-20260528-0001','manual',NULL,12,'confirmed',NULL,'2026-05-28 03:23:54',NULL,'2026-05-28 03:23:58',NULL,'2026-05-28 03:23:42',NULL,'2026-05-28 03:23:42');
/*!40000 ALTER TABLE `warehouse_outbound` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `warehouse_outbound_item`
--

DROP TABLE IF EXISTS `warehouse_outbound_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_outbound_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `outbound_id` bigint NOT NULL COMMENT 'FK -> warehouse_outbound.id',
  `input_item_id` bigint NOT NULL COMMENT 'FK -> input_item.id',
  `requested_qty` decimal(14,3) NOT NULL COMMENT 'Requested quantity (from activity / sales)',
  `picked_qty` decimal(14,3) DEFAULT NULL COMMENT 'Picked quantity (filled by picker)',
  `actual_qty` decimal(14,3) DEFAULT NULL COMMENT 'Actual issued qty (filled on confirm, usually = picked)',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_outbound_item` (`outbound_id`),
  KEY `idx_outbound_ii` (`input_item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Warehouse outbound order items ├óΓé¼ΓÇ¥ Sprint 22.5';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `warehouse_outbound_item`
--

LOCK TABLES `warehouse_outbound_item` WRITE;
/*!40000 ALTER TABLE `warehouse_outbound_item` DISABLE KEYS */;
INSERT INTO `warehouse_outbound_item` VALUES (1,1,17,20.000,20.000,20.000,'');
/*!40000 ALTER TABLE `warehouse_outbound_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `warehouse_scrap`
--

DROP TABLE IF EXISTS `warehouse_scrap`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_scrap` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SC-YYYYMMDD-NNNN',
  `warehouse_id` bigint NOT NULL,
  `scrap_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'damaged' COMMENT 'damaged / expired / other',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft' COMMENT 'draft / confirmed / cancelled',
  `confirmed_by` bigint DEFAULT NULL,
  `confirmed_at` datetime DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_sc_status` (`status`),
  KEY `idx_sc_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Warehouse scrap / write-off order ├óΓé¼ΓÇ¥ Sprint 22.8';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `warehouse_scrap`
--

LOCK TABLES `warehouse_scrap` WRITE;
/*!40000 ALTER TABLE `warehouse_scrap` DISABLE KEYS */;
/*!40000 ALTER TABLE `warehouse_scrap` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `warehouse_scrap_item`
--

DROP TABLE IF EXISTS `warehouse_scrap_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_scrap_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scrap_id` bigint NOT NULL,
  `input_item_id` bigint NOT NULL,
  `qty` decimal(14,3) NOT NULL COMMENT 'Scrap quantity',
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Reason for scrap (per item)',
  PRIMARY KEY (`id`),
  KEY `idx_sci_parent` (`scrap_id`),
  KEY `idx_sci_item` (`input_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Scrap order items ├óΓé¼ΓÇ¥ Sprint 22.8';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `warehouse_scrap_item`
--

LOCK TABLES `warehouse_scrap_item` WRITE;
/*!40000 ALTER TABLE `warehouse_scrap_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `warehouse_scrap_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `warehouse_stocktake`
--

DROP TABLE IF EXISTS `warehouse_stocktake`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_stocktake` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ST-YYYYMMDD-NNNN',
  `warehouse_id` bigint NOT NULL COMMENT 'FK -> location_warehouse.id',
  `count_type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'full' COMMENT 'full / cycle / random',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft' COMMENT 'draft / counting / confirmed / cancelled',
  `counted_by` bigint DEFAULT NULL,
  `counted_at` datetime DEFAULT NULL,
  `confirmed_by` bigint DEFAULT NULL,
  `confirmed_at` datetime DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_st_status` (`status`),
  KEY `idx_st_warehouse` (`warehouse_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Warehouse stocktake / inventory count ├óΓé¼ΓÇ¥ Sprint 22.6';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `warehouse_stocktake`
--

LOCK TABLES `warehouse_stocktake` WRITE;
/*!40000 ALTER TABLE `warehouse_stocktake` DISABLE KEYS */;
INSERT INTO `warehouse_stocktake` VALUES (1,'ST-20260528-0001',7,'full','confirmed',NULL,'2026-05-28 03:32:41',NULL,'2026-05-28 03:32:46','','2026-05-28 03:32:29',NULL,'2026-05-28 03:32:29');
/*!40000 ALTER TABLE `warehouse_stocktake` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `warehouse_stocktake_item`
--

DROP TABLE IF EXISTS `warehouse_stocktake_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_stocktake_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stocktake_id` bigint NOT NULL,
  `input_item_id` bigint NOT NULL,
  `system_qty` decimal(14,3) NOT NULL COMMENT 'System qty_on_hand at snapshot time',
  `count_qty` decimal(14,3) DEFAULT NULL COMMENT 'Physical count qty (filled by counter)',
  `diff_qty` decimal(14,3) DEFAULT NULL COMMENT '= count_qty - system_qty (computed on confirm)',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sti_parent` (`stocktake_id`),
  KEY `idx_sti_item` (`input_item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stocktake line items ├óΓé¼ΓÇ¥ Sprint 22.6';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `warehouse_stocktake_item`
--

LOCK TABLES `warehouse_stocktake_item` WRITE;
/*!40000 ALTER TABLE `warehouse_stocktake_item` DISABLE KEYS */;
INSERT INTO `warehouse_stocktake_item` VALUES (1,1,16,100.000,100.000,0.000,'');
/*!40000 ALTER TABLE `warehouse_stocktake_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `warehouse_transfer`
--

DROP TABLE IF EXISTS `warehouse_transfer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_transfer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'TR-YYYYMMDD-NNNN',
  `from_warehouse_id` bigint NOT NULL COMMENT 'Source warehouse',
  `to_warehouse_id` bigint NOT NULL COMMENT 'Target warehouse',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft' COMMENT 'draft / confirmed / cancelled',
  `confirmed_by` bigint DEFAULT NULL,
  `confirmed_at` datetime DEFAULT NULL,
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`),
  KEY `idx_tr_status` (`status`),
  KEY `idx_tr_from` (`from_warehouse_id`),
  KEY `idx_tr_to` (`to_warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Warehouse transfer order ├óΓé¼ΓÇ¥ Sprint 22.7';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `warehouse_transfer`
--

LOCK TABLES `warehouse_transfer` WRITE;
/*!40000 ALTER TABLE `warehouse_transfer` DISABLE KEYS */;
/*!40000 ALTER TABLE `warehouse_transfer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `warehouse_transfer_item`
--

DROP TABLE IF EXISTS `warehouse_transfer_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `warehouse_transfer_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transfer_id` bigint NOT NULL,
  `input_item_id` bigint NOT NULL,
  `qty` decimal(14,3) NOT NULL COMMENT 'Transfer quantity',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tri_parent` (`transfer_id`),
  KEY `idx_tri_item` (`input_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Transfer order items ├óΓé¼ΓÇ¥ Sprint 22.7';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `warehouse_transfer_item`
--

LOCK TABLES `warehouse_transfer_item` WRITE;
/*!40000 ALTER TABLE `warehouse_transfer_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `warehouse_transfer_item` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-28 22:48:41
