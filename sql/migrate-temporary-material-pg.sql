-- 会话临时资料：独立于正式学习资料，默认保留7天。
CREATE TABLE IF NOT EXISTS temporary_material (
    id BIGSERIAL PRIMARY KEY,
    upload_token VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    summary TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'processing',
    error_msg VARCHAR(500),
    chunk_count INT DEFAULT 0,
    expires_at TIMESTAMP NOT NULL,
    converted_material_id BIGINT,
    deleted SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_temp_material_user_expiry
    ON temporary_material(user_id, expires_at);
CREATE INDEX IF NOT EXISTS idx_temp_material_conversation
    ON temporary_material(user_id, conversation_id);

CREATE TABLE IF NOT EXISTS temporary_material_chunk (
    id BIGSERIAL PRIMARY KEY,
    temporary_material_id BIGINT NOT NULL,
    upload_token VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    chunk_size INT NOT NULL,
    deleted SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_temp_chunk_scope
    ON temporary_material_chunk(user_id, conversation_id, upload_token);
CREATE INDEX IF NOT EXISTS idx_temp_chunk_material
    ON temporary_material_chunk(temporary_material_id);

ALTER TABLE ai_chat_history
    ADD COLUMN IF NOT EXISTS temporary_material_token VARCHAR(64);
