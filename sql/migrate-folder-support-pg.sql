-- ============================================================
-- 资料文件夹功能迁移脚本（PostgreSQL 版本）
-- 适用于已有数据库升级
-- 执行顺序：先执行此脚本，再重启应用
-- ============================================================

-- 1. 创建资料文件夹表
CREATE TABLE IF NOT EXISTS material_folder (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    sort_order INT DEFAULT 0,
    deleted SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_material_folder_user_id ON material_folder(user_id);
CREATE INDEX IF NOT EXISTS idx_material_folder_parent_id ON material_folder(parent_id);
COMMENT ON TABLE material_folder IS '资料文件夹表';
COMMENT ON COLUMN material_folder.user_id IS '所属用户ID';
COMMENT ON COLUMN material_folder.name IS '文件夹名称';
COMMENT ON COLUMN material_folder.parent_id IS '父文件夹ID（NULL表示根层级）';
COMMENT ON COLUMN material_folder.sort_order IS '同级排序（越小越靠前）';

-- 2. 给 learning_material 表添加 folder_id 字段
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'learning_material' AND column_name = 'folder_id'
    ) THEN
        ALTER TABLE learning_material ADD COLUMN folder_id BIGINT;
        COMMENT ON COLUMN learning_material.folder_id IS '所属文件夹ID（NULL表示未分类）';
    END IF;
END $$;

-- 3. 给 learning_material 添加索引
CREATE INDEX IF NOT EXISTS idx_learning_material_folder_id ON learning_material(folder_id);

-- 4. 给 material_chunk 表添加 folder_id 字段
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'material_chunk' AND column_name = 'folder_id'
    ) THEN
        ALTER TABLE material_chunk ADD COLUMN folder_id BIGINT;
        COMMENT ON COLUMN material_chunk.folder_id IS '所属文件夹ID（冗余字段，便于按文件夹筛选）';
    END IF;
END $$;

-- 5. 给 material_chunk 添加索引
CREATE INDEX IF NOT EXISTS idx_material_chunk_folder_id ON material_chunk(folder_id);

-- 6. 同步已有切片的 folder_id（从 learning_material 关联获取）
UPDATE material_chunk c
SET folder_id = m.folder_id
FROM learning_material m
WHERE c.material_id = m.id
  AND c.folder_id IS NULL
  AND m.folder_id IS NOT NULL;

-- 7. 创建用户画像表（如果不存在）
CREATE TABLE IF NOT EXISTS user_profile (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    age INT,
    education VARCHAR(30),
    target_exam VARCHAR(100),
    study_subject VARCHAR(100),
    daily_study_goal_minutes INT DEFAULT 60,
    preferred_difficulty VARCHAR(10) DEFAULT 'normal',
    learning_style VARCHAR(20),
    weak_points JSONB,
    strong_points JSONB,
    overall_level INT DEFAULT 50,
    total_study_days INT DEFAULT 0,
    total_questions INT DEFAULT 0,
    accuracy_rate NUMERIC(5,2) DEFAULT 0,
    last_active_time TIMESTAMP,
    compressed_profile TEXT,
    profile_version INT DEFAULT 0,
    compressed_at TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_profile_user_id ON user_profile(user_id);
COMMENT ON TABLE user_profile IS '用户画像表';

-- 8. 创建 Agent执行日志表（如果不存在）
CREATE TABLE IF NOT EXISTS agent_execution_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    conversation_id VARCHAR(64),
    question TEXT,
    route_level INT DEFAULT 2,
    routed_expert VARCHAR(50),
    route_domain VARCHAR(20),
    route_intent VARCHAR(200),
    route_confidence DOUBLE PRECISION DEFAULT 0.0,
    routing JSONB,
    execution_chain JSONB,
    final_answer TEXT,
    fallback INT DEFAULT 0,
    error_message VARCHAR(500),
    total_duration_ms BIGINT DEFAULT 0,
    llm_call_count INT DEFAULT 0,
    tool_call_count INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    user_feedback VARCHAR(10),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_agent_execution_log_user_id ON agent_execution_log(user_id);
CREATE INDEX IF NOT EXISTS idx_agent_execution_log_conversation_id ON agent_execution_log(conversation_id);
COMMENT ON TABLE agent_execution_log IS 'Agent执行日志';

-- 完成
DO $$
BEGIN
    RAISE NOTICE 'Folder migration completed successfully!';
END $$;
