-- 迁移脚本：为 ai_chat_history 表新增 folder_id 列
-- 用于支持 AI 问答页面选择整个文件夹进行多资料 RAG 检索
-- 向后兼容：老数据 folder_id 为 NULL

ALTER TABLE ai_chat_history
    ADD COLUMN IF NOT EXISTS folder_id BIGINT;

COMMENT ON COLUMN ai_chat_history.folder_id IS '关联文件夹ID（文件夹级问答时使用，与 material_id 互斥）';

-- 按文件夹查询历史的索引
CREATE INDEX IF NOT EXISTS idx_ai_chat_history_user_folder
    ON ai_chat_history(user_id, folder_id);
