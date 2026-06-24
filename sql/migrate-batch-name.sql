-- ============================================================
-- 迁移脚本：添加 batch_name 字段到 ai_question_bank 表
-- 执行时间：2026-06-24
-- ============================================================

-- 添加 batch_name 字段（如果已存在会报错，可忽略）
ALTER TABLE `ai_question_bank`
ADD COLUMN `batch_name` VARCHAR(200) COMMENT '批次名称' AFTER `batch_id`;

-- 验证字段已添加
DESCRIBE `ai_question_bank`;
