-- 修复 019 种子数据里的中文乱码 (PowerShell Get-Content 编码问题导致)
SET NAMES utf8mb4;

UPDATE `supplier` SET `payment_terms` = '月结'
  WHERE `code` IN ('SUP-00002', 'SUP-00004', 'SUP-00005');

UPDATE `supplier` SET `payment_terms` = '周结'
  WHERE `code` = 'SUP-00003';

-- 验证
SELECT code, name, payment_terms, credit_days FROM supplier;
