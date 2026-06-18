-- ============================================================
-- AI 智能学习助手 - 数据库初始化脚本
-- ============================================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname` VARCHAR(50) COMMENT '昵称',
    `email` VARCHAR(100) COMMENT '邮箱',
    `avatar` VARCHAR(500) COMMENT '头像URL',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0/1',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 学习资料表
CREATE TABLE IF NOT EXISTS `learning_material` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `original_name` VARCHAR(255) NOT NULL,
    `stored_name` VARCHAR(255) NOT NULL,
    `file_type` VARCHAR(20) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `file_path` VARCHAR(500) NOT NULL,
    `category` VARCHAR(50),
    `summary` MEDIUMTEXT,
    `status` VARCHAR(20) NOT NULL DEFAULT 'processing',
    `error_msg` VARCHAR(500),
    `chunk_count` INT DEFAULT 0,
    `deleted` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习资料表';

-- 3. 文档切片表
CREATE TABLE IF NOT EXISTS `material_chunk` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `material_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `chunk_index` INT NOT NULL,
    `content` TEXT NOT NULL,
    `embedding` BLOB,
    `chunk_size` INT NOT NULL,
    `deleted` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_material_id` (`material_id`),
    INDEX `idx_user_material` (`user_id`, `material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档切片表';

-- 4. AI对话历史表
CREATE TABLE IF NOT EXISTS `ai_chat_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `chat_type` VARCHAR(20) NOT NULL COMMENT 'summary/qa/quiz/plan',
    `material_id` BIGINT COMMENT '关联资料ID',
    `user_message` TEXT COMMENT '用户输入',
    `ai_response` MEDIUMTEXT COMMENT 'AI回复',
    `batch_id` VARCHAR(64) COMMENT '批次ID（出题专用）',
    `conversation_id` VARCHAR(64) COMMENT '会话ID（多轮对话专用）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_type_time` (`user_id`, `chat_type`, `create_time`),
    INDEX `idx_user_material` (`user_id`, `material_id`),
    INDEX `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话历史表';

-- 5. AI题库表
CREATE TABLE IF NOT EXISTS `ai_question_bank` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `material_id` BIGINT NOT NULL,
    `batch_id` VARCHAR(64) NOT NULL COMMENT '批次ID',
    `question_type` VARCHAR(20) NOT NULL COMMENT 'choice/judge/short_answer',
    `difficulty` VARCHAR(10) NOT NULL DEFAULT 'medium',
    `question` TEXT NOT NULL,
    `options` JSON COMMENT '选项JSON（选择题）',
    `answer` TEXT NOT NULL,
    `explanation` TEXT COMMENT '答案解析',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_batch_id` (`batch_id`),
    INDEX `idx_user_type` (`user_id`, `question_type`),
    INDEX `idx_material_id` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI题库表';

-- 6. 作答记录表
CREATE TABLE IF NOT EXISTS `ai_quiz_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `batch_id` VARCHAR(64) NOT NULL,
    `question_id` BIGINT NOT NULL,
    `user_answer` TEXT,
    `is_correct` TINYINT DEFAULT 0,
    `score` DECIMAL(3,2) DEFAULT 0 COMMENT '简答题AI评分',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_batch` (`user_id`, `batch_id`),
    INDEX `idx_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出题作答记录';

-- 7. 学习计划表
CREATE TABLE IF NOT EXISTS `study_plan` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) COMMENT '计划标题',
    `goal` TEXT COMMENT '学习目标',
    `exam_date` DATE COMMENT '考试日期',
    `daily_hours` INT DEFAULT 2 COMMENT '每日学习时长',
    `total_days` INT COMMENT '总天数',
    `plan_content` MEDIUMTEXT COMMENT '计划内容JSON',
    `material_ids` VARCHAR(500) COMMENT '关联资料ID列表',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划表';

-- 8. 错题本表
CREATE TABLE IF NOT EXISTS `user_wrong_question` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL COMMENT '关联 ai_question_bank.id',
    `material_id` BIGINT COMMENT '来源资料',
    `question_type` VARCHAR(20) NOT NULL,
    `user_answer` TEXT COMMENT '用户答案',
    `correct_answer` TEXT COMMENT '正确答案',
    `wrong_count` INT DEFAULT 1 COMMENT '错误次数',
    `last_wrong_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `is_mastered` TINYINT DEFAULT 0 COMMENT '是否已掌握 0/1',
    `master_time` DATETIME COMMENT '掌握时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
    INDEX `idx_user_mastered` (`user_id`, `is_mastered`),
    INDEX `idx_material_id` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户错题本';

-- 9. AI异步任务表
CREATE TABLE IF NOT EXISTS `ai_task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `task_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '任务UUID',
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(30) NOT NULL COMMENT 'material_process/summary/quiz/plan',
    `ref_id` BIGINT COMMENT '关联资源ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/running/success/failed',
    `progress` INT DEFAULT 0 COMMENT '进度 0-100',
    `message` VARCHAR(500),
    `result` JSON COMMENT '任务结果',
    `error_msg` TEXT,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI异步任务表';
