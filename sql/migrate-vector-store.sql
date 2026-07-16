-- 启用向量和UUID扩展
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 创建向量存储表（维度 512，匹配 bge-m3 局部化维度）
CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata jsonb,
    embedding vector(1024)
);

-- 创建 HNSW 余弦相似度索引
CREATE INDEX IF NOT EXISTS vector_store_hnsw_idx ON vector_store USING HNSW (embedding vector_cosine_ops);
