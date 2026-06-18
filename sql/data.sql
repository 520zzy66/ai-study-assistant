-- AI 学习助手初始化数据

USE ai_study;
SET NAMES utf8mb4;

-- 插入测试用户 (密码: 123456, BCrypt加密)
INSERT INTO user (username, password, nickname, email) VALUES
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试用户', 'test@example.com')
ON DUPLICATE KEY UPDATE username=username;
