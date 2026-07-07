-- ============================================================
-- 用户画像表扩展：compressed_profile（LLM 压缩的用户学习画像）
-- ============================================================

-- 1. 新增字段
ALTER TABLE user_profile
    ADD COLUMN IF NOT EXISTS compressed_profile TEXT,
    ADD COLUMN IF NOT EXISTS profile_version INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS compressed_at TIMESTAMP;

-- 2. 注释
COMMENT ON COLUMN user_profile.compressed_profile IS 'LLM 压缩的用户学习画像（500字以内），基于最近50轮对话异步生成';
COMMENT ON COLUMN user_profile.profile_version IS '压缩时的对话轮次号，用于判断是否过期需要重新压缩';
COMMENT ON COLUMN user_profile.compressed_at IS '画像压缩时间';
