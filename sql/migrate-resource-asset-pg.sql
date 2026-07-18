-- ============================================================
-- 迁移脚本：资源资产表 resource_asset
-- 用于保存讯飞 TTS 音频和文生图图片资产元数据
-- 文件本体保存到本地上传目录或后续对象存储
-- ============================================================

CREATE TABLE IF NOT EXISTS resource_asset (
    id BIGSERIAL PRIMARY KEY,
    asset_id VARCHAR(64) NOT NULL UNIQUE,
    package_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64),
    user_id BIGINT NOT NULL,
    material_id BIGINT,
    asset_type VARCHAR(32) NOT NULL,
    asset_role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(128),
    file_name VARCHAR(255),
    content_type VARCHAR(128),
    storage_path VARCHAR(512),
    size_bytes BIGINT,
    duration_seconds INTEGER,
    width INTEGER,
    height INTEGER,
    prompt_summary TEXT,
    source_keys TEXT,
    error_code VARCHAR(64),
    error_message VARCHAR(512),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_resource_asset_package_id ON resource_asset(package_id);
CREATE INDEX IF NOT EXISTS idx_resource_asset_task_id ON resource_asset(task_id);
CREATE INDEX IF NOT EXISTS idx_resource_asset_user_id ON resource_asset(user_id);

COMMENT ON TABLE resource_asset IS '资源资产元数据表（音频/图片）';
COMMENT ON COLUMN resource_asset.asset_id IS '资产UUID（前端下载用）';
COMMENT ON COLUMN resource_asset.package_id IS '所属资源包ID';
COMMENT ON COLUMN resource_asset.task_id IS '生成任务ID（可空）';
COMMENT ON COLUMN resource_asset.user_id IS '所属用户ID';
COMMENT ON COLUMN resource_asset.material_id IS '关联资料ID（可空）';
COMMENT ON COLUMN resource_asset.asset_type IS '资产类型：audio/image';
COMMENT ON COLUMN resource_asset.asset_role IS '资产角色：podcast/cover/explanation';
COMMENT ON COLUMN resource_asset.status IS '状态：pending/generating/success/failed/cancelled';
COMMENT ON COLUMN resource_asset.provider IS '提供方：xfyun-tts/xfyun-tti/xfyun-hidream';
COMMENT ON COLUMN resource_asset.storage_path IS '相对存储路径（禁止暴露绝对路径到前端）';
COMMENT ON COLUMN resource_asset.prompt_summary IS '生成提示词摘要（脱敏后）';
COMMENT ON COLUMN resource_asset.source_keys IS '来源资源 key 列表（逗号分隔，如 summary,mindMap）';
COMMENT ON COLUMN resource_asset.error_code IS '失败错误码（业务可理解）';
COMMENT ON COLUMN resource_asset.error_message IS '失败原因（业务可理解）';
COMMENT ON COLUMN resource_asset.metadata IS '扩展元数据（JSONB，如 originalPrompt/style/voice）';
