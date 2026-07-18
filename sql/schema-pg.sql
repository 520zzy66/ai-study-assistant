-- ============================================================
-- AI 智能学习助手 - PostgreSQL 初始化脚本
-- ============================================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    avatar VARCHAR(500),
    deleted SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE app_user IS '用户表';
COMMENT ON COLUMN app_user.username IS '用户名';
COMMENT ON COLUMN app_user.password IS '密码（BCrypt加密）';
COMMENT ON COLUMN app_user.deleted IS '逻辑删除 0/1';

-- 2. 学习资料表
CREATE TABLE IF NOT EXISTS learning_material (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    category VARCHAR(50),
    folder_id BIGINT,
    summary TEXT,
    mind_map TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'processing',
    error_msg VARCHAR(500),
    chunk_count INT DEFAULT 0,
    source VARCHAR(20) DEFAULT 'user',
    retry_count INT DEFAULT 0,
    deleted SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, source, original_name)
);
CREATE INDEX idx_learning_material_user_id ON learning_material(user_id);
CREATE INDEX idx_learning_material_status ON learning_material(status);
CREATE INDEX idx_learning_material_folder_id ON learning_material(folder_id);
COMMENT ON TABLE learning_material IS '学习资料表';
COMMENT ON COLUMN learning_material.folder_id IS '所属文件夹ID（NULL表示未分类）';
COMMENT ON COLUMN learning_material.source IS '资料来源 user=用户上传 system=系统预置';
COMMENT ON COLUMN learning_material.retry_count IS '自动重试次数';

-- 2.1 资料文件夹表
CREATE TABLE IF NOT EXISTS material_folder (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    sort_order INT DEFAULT 0,
    deleted SMALLINT DEFAULT 0,
    summary TEXT,
    mind_map TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_material_folder_user_id ON material_folder(user_id);
CREATE INDEX idx_material_folder_parent_id ON material_folder(parent_id);
COMMENT ON TABLE material_folder IS '资料文件夹表';
COMMENT ON COLUMN material_folder.user_id IS '所属用户ID';
COMMENT ON COLUMN material_folder.name IS '文件夹名称';
COMMENT ON COLUMN material_folder.parent_id IS '父文件夹ID（NULL表示根层级）';
COMMENT ON COLUMN material_folder.sort_order IS '同级排序（越小越靠前）';

-- 3. 文档切片表
CREATE TABLE IF NOT EXISTS material_chunk (
    id BIGSERIAL PRIMARY KEY,
    material_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    folder_id BIGINT,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding BYTEA,
    chunk_size INT NOT NULL,
    deleted SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_material_chunk_material_id ON material_chunk(material_id);
CREATE INDEX idx_material_chunk_user_material ON material_chunk(user_id, material_id);
CREATE INDEX idx_material_chunk_folder_id ON material_chunk(folder_id);
COMMENT ON TABLE material_chunk IS '文档切片表';
COMMENT ON COLUMN material_chunk.folder_id IS '所属文件夹ID（冗余字段，便于按文件夹筛选）';

-- 3.1 会话临时资料表（默认保留7天）
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
COMMENT ON TABLE temporary_material IS 'AI问答会话临时资料表';

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
COMMENT ON TABLE temporary_material_chunk IS '会话临时资料切片表';

-- 4. AI对话历史表
CREATE TABLE IF NOT EXISTS ai_chat_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chat_type VARCHAR(20) NOT NULL,
    material_id BIGINT,
    folder_id BIGINT,
    temporary_material_token VARCHAR(64),
    user_message TEXT,
    ai_response TEXT,
    batch_id VARCHAR(64),
    conversation_id VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ai_chat_history_user_type_time ON ai_chat_history(user_id, chat_type, create_time);
CREATE INDEX idx_ai_chat_history_user_material ON ai_chat_history(user_id, material_id);
CREATE INDEX idx_ai_chat_history_user_folder ON ai_chat_history(user_id, folder_id);
CREATE INDEX idx_ai_chat_history_conversation_id ON ai_chat_history(conversation_id);
COMMENT ON TABLE ai_chat_history IS 'AI对话历史表';
COMMENT ON COLUMN ai_chat_history.chat_type IS 'summary/qa/quiz/plan';
COMMENT ON COLUMN ai_chat_history.material_id IS '关联资料ID（与 folder_id 互斥）';
COMMENT ON COLUMN ai_chat_history.folder_id IS '关联文件夹ID（文件夹级问答时使用，与 material_id 互斥）';
COMMENT ON COLUMN ai_chat_history.user_message IS '用户输入';
COMMENT ON COLUMN ai_chat_history.ai_response IS 'AI回复';
COMMENT ON COLUMN ai_chat_history.batch_id IS '批次ID（出题专用）';
COMMENT ON COLUMN ai_chat_history.conversation_id IS '会话ID（多轮对话专用）';
COMMENT ON COLUMN ai_chat_history.temporary_material_token IS '会话临时资料令牌（最多保留7天）';

-- 5. AI题库表
CREATE TABLE IF NOT EXISTS ai_question_bank (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    batch_id VARCHAR(64) NOT NULL,
    batch_name VARCHAR(200),
    question_type VARCHAR(20) NOT NULL,
    difficulty VARCHAR(10) NOT NULL DEFAULT 'medium',
    question TEXT NOT NULL,
    options JSONB,
    answer TEXT NOT NULL,
    explanation TEXT,
    is_favorite SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ai_question_bank_batch_id ON ai_question_bank(batch_id);
CREATE INDEX idx_ai_question_bank_user_type ON ai_question_bank(user_id, question_type);
CREATE INDEX idx_ai_question_bank_material_id ON ai_question_bank(material_id);
CREATE INDEX idx_ai_question_bank_user_favorite ON ai_question_bank(user_id, is_favorite);
COMMENT ON TABLE ai_question_bank IS 'AI题库表';
COMMENT ON COLUMN ai_question_bank.batch_id IS '批次ID';
COMMENT ON COLUMN ai_question_bank.question_type IS 'choice/judge/short_answer';
COMMENT ON COLUMN ai_question_bank.options IS '选项JSON（选择题）';
COMMENT ON COLUMN ai_question_bank.explanation IS '答案解析';
COMMENT ON COLUMN ai_question_bank.is_favorite IS '收藏标记 0/1';

-- 6. 作答记录表
CREATE TABLE IF NOT EXISTS ai_quiz_record (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    batch_id VARCHAR(64) NOT NULL,
    question_id BIGINT NOT NULL,
    user_answer TEXT,
    is_correct SMALLINT DEFAULT 0,
    score NUMERIC(3,2) DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ai_quiz_record_user_batch ON ai_quiz_record(user_id, batch_id);
CREATE INDEX idx_ai_quiz_record_batch_id ON ai_quiz_record(batch_id);
COMMENT ON TABLE ai_quiz_record IS '出题作答记录';
COMMENT ON COLUMN ai_quiz_record.score IS '简答题AI评分';

-- 7. 学习计划表
CREATE TABLE IF NOT EXISTS study_plan (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    goal TEXT,
    exam_date DATE,
    daily_hours INT DEFAULT 2,
    total_days INT,
    plan_content TEXT,
    material_ids VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_study_plan_user_id ON study_plan(user_id);
COMMENT ON TABLE study_plan IS '学习计划表';
COMMENT ON COLUMN study_plan.title IS '计划标题';
COMMENT ON COLUMN study_plan.goal IS '学习目标';
COMMENT ON COLUMN study_plan.exam_date IS '考试日期';
COMMENT ON COLUMN study_plan.daily_hours IS '每日学习时长';
COMMENT ON COLUMN study_plan.total_days IS '总天数';
COMMENT ON COLUMN study_plan.plan_content IS '计划内容JSON';
COMMENT ON COLUMN study_plan.material_ids IS '关联资料ID列表';

-- 7.1 学习计划进度表
CREATE TABLE IF NOT EXISTS study_plan_progress (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    day_index INT NOT NULL,
    completed SMALLINT DEFAULT 0,
    actual_hours NUMERIC(4,1) DEFAULT 0,
    note TEXT,
    complete_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (plan_id, day_index)
);
CREATE INDEX idx_study_plan_progress_user_plan ON study_plan_progress(user_id, plan_id);
CREATE INDEX idx_study_plan_progress_completed ON study_plan_progress(plan_id, completed);
COMMENT ON TABLE study_plan_progress IS '学习计划进度表';
COMMENT ON COLUMN study_plan_progress.plan_id IS '关联 study_plan.id';
COMMENT ON COLUMN study_plan_progress.day_index IS '第几天（从1开始）';
COMMENT ON COLUMN study_plan_progress.completed IS '是否完成 0/1';
COMMENT ON COLUMN study_plan_progress.actual_hours IS '实际学习时长';
COMMENT ON COLUMN study_plan_progress.note IS '学习笔记';
COMMENT ON COLUMN study_plan_progress.complete_time IS '完成时间';

-- 8. 错题本表
CREATE TABLE IF NOT EXISTS user_wrong_question (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    material_id BIGINT,
    question_type VARCHAR(20) NOT NULL,
    user_answer TEXT,
    correct_answer TEXT,
    wrong_count INT DEFAULT 1,
    last_wrong_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_mastered SMALLINT DEFAULT 0,
    master_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, question_id)
);
CREATE INDEX idx_user_wrong_question_mastered ON user_wrong_question(user_id, is_mastered);
CREATE INDEX idx_user_wrong_question_material_id ON user_wrong_question(material_id);
COMMENT ON TABLE user_wrong_question IS '用户错题本';
COMMENT ON COLUMN user_wrong_question.question_id IS '关联 ai_question_bank.id';
COMMENT ON COLUMN user_wrong_question.material_id IS '来源资料';
COMMENT ON COLUMN user_wrong_question.user_answer IS '用户答案';
COMMENT ON COLUMN user_wrong_question.correct_answer IS '正确答案';
COMMENT ON COLUMN user_wrong_question.wrong_count IS '错误次数';
COMMENT ON COLUMN user_wrong_question.is_mastered IS '是否已掌握 0/1';
COMMENT ON COLUMN user_wrong_question.master_time IS '掌握时间';

-- 9. AI异步任务表
CREATE TABLE IF NOT EXISTS ai_task (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    ref_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    progress INT DEFAULT 0,
    message VARCHAR(500),
    result JSONB,
    error_msg TEXT,
    cancel_requested SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ai_task_user_id ON ai_task(user_id);
CREATE INDEX idx_ai_task_task_id ON ai_task(task_id);
CREATE INDEX idx_ai_task_status ON ai_task(status);
COMMENT ON TABLE ai_task IS 'AI异步任务表';
COMMENT ON COLUMN ai_task.task_id IS '任务UUID';
COMMENT ON COLUMN ai_task.type IS 'material_process/summary/quiz/plan';
COMMENT ON COLUMN ai_task.ref_id IS '关联资源ID';
COMMENT ON COLUMN ai_task.status IS 'pending/running/success/failed';
COMMENT ON COLUMN ai_task.progress IS '进度 0-100';
COMMENT ON COLUMN ai_task.result IS '任务结果';
COMMENT ON COLUMN ai_task.cancel_requested IS '是否请求取消 0/1';

-- 10. 用户画像表
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
CREATE INDEX idx_user_profile_user_id ON user_profile(user_id);
COMMENT ON TABLE user_profile IS '用户画像表';
COMMENT ON COLUMN user_profile.user_id IS '用户ID（一对一关联）';
COMMENT ON COLUMN user_profile.education IS '学籍：high_school/junior_college/undergraduate/master/doctor/working/fresh_graduate';
COMMENT ON COLUMN user_profile.target_exam IS '目标考试';
COMMENT ON COLUMN user_profile.study_subject IS '学习科目';
COMMENT ON COLUMN user_profile.daily_study_goal_minutes IS '每日学习目标（分钟）';
COMMENT ON COLUMN user_profile.preferred_difficulty IS '难度偏好：easy/normal/hard';
COMMENT ON COLUMN user_profile.learning_style IS '学习风格：visual/auditory/practice';
COMMENT ON COLUMN user_profile.weak_points IS '薄弱知识点列表';
COMMENT ON COLUMN user_profile.strong_points IS '擅长知识点列表';
COMMENT ON COLUMN user_profile.overall_level IS '综合水平评估 0~100';
COMMENT ON COLUMN user_profile.compressed_profile IS 'LLM压缩的用户学习画像';
COMMENT ON COLUMN user_profile.profile_version IS '压缩时的对话轮次号';
COMMENT ON COLUMN user_profile.compressed_at IS '画像压缩时间';

-- 11. Agent执行日志表
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_agent_execution_log_user_id ON agent_execution_log(user_id);
CREATE INDEX idx_agent_execution_log_conversation_id ON agent_execution_log(conversation_id);
COMMENT ON TABLE agent_execution_log IS 'Agent执行日志';

-- 12. 用户记忆条目表（非结构化长期记忆）
CREATE TABLE IF NOT EXISTS user_memory_entry (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    importance REAL DEFAULT 0.5,
    source_type VARCHAR(20),
    source_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_user_memory_entry_user_id ON user_memory_entry(user_id);
CREATE INDEX idx_user_memory_entry_category ON user_memory_entry(user_id, category);
COMMENT ON TABLE user_memory_entry IS '用户记忆条目表';
COMMENT ON COLUMN user_memory_entry.category IS '记忆类别：preference/goal/knowledge/mistake/habit';
COMMENT ON COLUMN user_memory_entry.importance IS '重要度 0~1';
COMMENT ON COLUMN user_memory_entry.source_type IS '来源类型：dialog/quiz/plan/profile';

-- 13. 系统知识库导入日志表
CREATE TABLE IF NOT EXISTS system_knowledge_import_log (
    id BIGSERIAL PRIMARY KEY,
    file_path VARCHAR(500) NOT NULL UNIQUE,
    file_md5 VARCHAR(64) NOT NULL,
    file_size BIGINT,
    chunk_count INT DEFAULT 0,
    import_batch VARCHAR(64),
    knowledge_root VARCHAR(500),
    deleted SMALLINT DEFAULT 0,
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_system_knowledge_import_log_md5 ON system_knowledge_import_log(file_md5);
COMMENT ON TABLE system_knowledge_import_log IS '系统知识库导入日志';
COMMENT ON COLUMN system_knowledge_import_log.file_md5 IS '文件内容MD5，用于判断内容是否变更';
COMMENT ON COLUMN system_knowledge_import_log.import_batch IS '导入批次号（版本标识）';

-- 14. 向量存储表（Spring AI 默认 vector_store 表）
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata jsonb,
    embedding vector(1024)
);
CREATE INDEX IF NOT EXISTS vector_store_hnsw_idx ON vector_store USING HNSW (embedding vector_cosine_ops);

-- 19. 资源资产表（讯飞 TTS 音频 + 文生图图片元数据）
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
COMMENT ON COLUMN resource_asset.asset_type IS '资产类型：audio/image';
COMMENT ON COLUMN resource_asset.asset_role IS '资产角色：podcast/cover/explanation';
COMMENT ON COLUMN resource_asset.status IS '状态：pending/generating/success/failed/cancelled';
COMMENT ON COLUMN resource_asset.provider IS '提供方：xfyun-tts/xfyun-tti/xfyun-hidream';
COMMENT ON COLUMN resource_asset.storage_path IS '相对存储路径（禁止暴露绝对路径到前端）';
